package com.rudra.smartworktracker.ui.screens.realitytracker

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.repository.RealityTrackerRepository
import com.rudra.smartworktracker.model.RealityCategory
import com.rudra.smartworktracker.model.RealityEntry
import com.rudra.smartworktracker.model.RealityEntryType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

enum class TimeRange(val label: String) {
    TODAY("Today"), WEEK("This week"), MONTH("This month")
}

enum class EntryFilter(val label: String) {
    ALL("All"), OPEN("Open"), OVERDUE("Overdue"), DONE("Done")
}

data class RealityStats(
    val totalPlanned: Int = 0,
    val totalCompleted: Int = 0,
    val goalsPlanned: Int = 0,
    val goalsCompleted: Int = 0,
    val promisesPlanned: Int = 0,
    val promisesCompleted: Int = 0,
    val plansPlanned: Int = 0,
    val plansCompleted: Int = 0,
    val overdue: Int = 0,
    val weakestCategory: RealityCategory? = null
) {
    /** Share of what was planned in the period that got done (0-100, never above 100). */
    val completionRate: Float
        get() = if (totalPlanned > 0) totalCompleted.toFloat() / totalPlanned * 100 else 0f

    val overestimationPercentage: Float
        get() = if (totalPlanned > 0) 100f - completionRate else 0f

    fun getStatsByType(type: RealityEntryType): Pair<Int, Int> = when (type) {
        RealityEntryType.GOAL -> goalsPlanned to goalsCompleted
        RealityEntryType.PROMISE -> promisesPlanned to promisesCompleted
        RealityEntryType.PLAN -> plansPlanned to plansCompleted
    }
}

data class RealityUiState(
    val entries: List<RealityEntry> = emptyList(),
    val stats: RealityStats = RealityStats(),
    val range: TimeRange = TimeRange.WEEK,
    val filter: EntryFilter = EntryFilter.ALL,
    val counts: Map<EntryFilter, Int> = emptyMap(),
    val isLoading: Boolean = true
)

class RealityTrackerViewModel(private val repository: RealityTrackerRepository) : ViewModel() {

    private val _selectedTimeRange = MutableStateFlow(TimeRange.WEEK)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()

    private val _filter = MutableStateFlow(EntryFilter.ALL)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var lastDeleted: RealityEntry? = null

    val entries: StateFlow<List<RealityEntry>> = repository.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<RealityUiState> = combine(
        repository.getAllEntries(), _selectedTimeRange, _filter
    ) { all, range, filter ->
        val (start, end) = rangeMillis(range)
        val today = LocalDate.now()

        // Entries that belong to the period: created or finished in it, plus anything still open
        val relevant = all.filter { entry ->
            entry.createdAt in start until end ||
                (entry.completedAt != null && entry.completedAt in start until end) ||
                (!entry.isCompleted && entry.createdAt < end)
        }
        val counts = EntryFilter.entries.associateWith { f -> relevant.count { matches(it, f, today) } }
        val visible = relevant
            .filter { matches(it, filter, today) }
            .sortedWith(
                compareBy<RealityEntry> { it.isCompleted }
                    .thenByDescending { isOverdue(it, today) }
                    .thenBy { it.targetDate ?: Long.MAX_VALUE }
                    .thenByDescending { it.createdAt }
            )

        RealityUiState(
            entries = visible,
            stats = statsFor(all.filter { it.createdAt in start until end }, all, today),
            range = range,
            filter = filter,
            counts = counts,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RealityUiState())

    /** Kept for callers that only need the numbers. */
    val stats: StateFlow<RealityStats> = uiState.map { it.stats }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RealityStats())

    fun setTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
    }

    fun setFilter(filter: EntryFilter) {
        _filter.value = filter
    }

    fun addEntry(
        type: RealityEntryType,
        title: String,
        description: String,
        category: RealityCategory,
        targetDate: Long? = null
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addEntry(
                RealityEntry(
                    type = type,
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    targetDate = targetDate
                )
            )
        }
    }

    fun updateEntry(entry: RealityEntry) {
        if (entry.title.isBlank()) return
        viewModelScope.launch {
            repository.updateEntry(entry.copy(title = entry.title.trim(), description = entry.description.trim()))
        }
    }

    fun toggleCompletion(entry: RealityEntry) {
        viewModelScope.launch {
            repository.markAsCompleted(entry.id, !entry.isCompleted)
        }
    }

    fun deleteEntry(entry: RealityEntry) {
        viewModelScope.launch {
            lastDeleted = entry
            repository.deleteEntry(entry.id)
            _events.emit("\"${entry.title}\" deleted")
        }
    }

    fun deleteEntry(id: String) {
        entries.value.firstOrNull { it.id == id }?.let(::deleteEntry)
    }

    fun undoDelete() {
        val entry = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { repository.addEntry(entry) }
    }

    private fun statsFor(cohort: List<RealityEntry>, all: List<RealityEntry>, today: LocalDate): RealityStats {
        fun planned(type: RealityEntryType) = cohort.count { it.type == type }
        fun done(type: RealityEntryType) = cohort.count { it.type == type && it.isCompleted }
        // Category with the most unfinished items (needs a few entries to mean anything)
        val weakest = cohort.filter { !it.isCompleted }
            .groupingBy { it.category }.eachCount()
            .filterValues { it >= 2 }
            .maxByOrNull { it.value }?.key
        return RealityStats(
            totalPlanned = cohort.size,
            totalCompleted = cohort.count { it.isCompleted },
            goalsPlanned = planned(RealityEntryType.GOAL),
            goalsCompleted = done(RealityEntryType.GOAL),
            promisesPlanned = planned(RealityEntryType.PROMISE),
            promisesCompleted = done(RealityEntryType.PROMISE),
            plansPlanned = planned(RealityEntryType.PLAN),
            plansCompleted = done(RealityEntryType.PLAN),
            overdue = all.count { isOverdue(it, today) },
            weakestCategory = weakest
        )
    }

    private fun matches(entry: RealityEntry, filter: EntryFilter, today: LocalDate): Boolean = when (filter) {
        EntryFilter.ALL -> true
        EntryFilter.OPEN -> !entry.isCompleted
        EntryFilter.OVERDUE -> isOverdue(entry, today)
        EntryFilter.DONE -> entry.isCompleted
    }

    private fun rangeMillis(range: TimeRange): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val (start, endExclusive) = when (range) {
            TimeRange.TODAY -> today to today.plusDays(1)
            TimeRange.WEEK -> {
                val first = WeekFields.of(Locale.getDefault()).firstDayOfWeek
                val start = today.with(TemporalAdjusters.previousOrSame(first))
                start to start.plusWeeks(1)
            }
            TimeRange.MONTH -> today.withDayOfMonth(1) to today.withDayOfMonth(1).plusMonths(1)
        }
        return start.atStartOfDay(zone).toInstant().toEpochMilli() to
            endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    companion object {
        fun dueDateOf(entry: RealityEntry): LocalDate? = entry.targetDate?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        fun isOverdue(entry: RealityEntry, today: LocalDate = LocalDate.now()): Boolean =
            !entry.isCompleted && dueDateOf(entry)?.isBefore(today) == true
    }
}

class RealityTrackerViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RealityTrackerViewModel::class.java)) {
            val realityTrackerDao = AppDatabase.getDatabase(application).realityTrackerDao()
            val repository = RealityTrackerRepository(realityTrackerDao)
            @Suppress("UNCHECKED_CAST")
            return RealityTrackerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
