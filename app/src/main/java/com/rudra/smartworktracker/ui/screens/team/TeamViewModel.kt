package com.rudra.smartworktracker.ui.screens.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.SharedPreferenceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import kotlin.math.min

class TeamViewModel(private val sharedPreferenceManager: SharedPreferenceManager) : ViewModel() {

    private val _teams = MutableStateFlow(loadAndCleanTeams())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    private val _dutyCalendar = MutableStateFlow<Map<LocalDate, List<CalendarDuty>>>(emptyMap())
    val dutyCalendar: StateFlow<Map<LocalDate, List<CalendarDuty>>> = _dutyCalendar.asStateFlow()

    // Persisted so swap requests survive leaving the screen / restarting the app
    private val _pendingSwaps = MutableStateFlow(sharedPreferenceManager.getDutySwaps())
    val pendingSwaps: StateFlow<List<DutySwap>> = _pendingSwaps.asStateFlow()

    private fun updateSwaps(swaps: List<DutySwap>) {
        _pendingSwaps.value = swaps
        sharedPreferenceManager.saveDutySwaps(swaps)
    }

    private val _uiEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val uiEvent = _uiEvent.asSharedFlow()

    private var notificationManager: DutyNotificationManager? = null

    init {
        updateDutyCalendar()
        
        viewModelScope.launch {
            _teams.collect { 
                updateDutyCalendar() 
            }
        }
    }

    fun setNotificationManager(manager: DutyNotificationManager) {
        this.notificationManager = manager
    }

    private fun updateTeams(newTeams: List<Team>) {
        _teams.value = newTeams
        sharedPreferenceManager.saveTeams(newTeams)
        updateDutyCalendar()
    }

    private fun updateDutyCalendar() {
        val calendarMap = mutableMapOf<LocalDate, MutableList<CalendarDuty>>()
        
        _teams.value.forEach { team ->
            team.teammates.forEach { teammate ->
                // Add explicit assigned duties
                teammate.dutySchedule.assignedDuties.forEach { duty ->
                    val list = calendarMap.getOrPut(duty.date) { mutableListOf() }
                    list.add(CalendarDuty(
                        teammateId = teammate.id,
                        teammateName = teammate.name,
                        duty = duty.copy()
                    )) 
                }
                
                // Add regular schedule duties for the next 30 days if not overridden
                val today = LocalDate.now()
                for (i in 0..30) {
                    val date = today.plusDays(i.toLong())
                    val dayValue = date.dayOfWeek.value 
                    
                    if (teammate.dutySchedule.regularDutyDays.contains(dayValue)) {
                        val hasExplicit = teammate.dutySchedule.assignedDuties.any { it.date == date }
                        val isHoliday = teammate.dutySchedule.offDays.contains(date)
                        
                        if (!hasExplicit && !isHoliday) {
                            val list = calendarMap.getOrPut(date) { mutableListOf() }
                            list.add(CalendarDuty(
                                teammateId = teammate.id,
                                teammateName = teammate.name,
                                duty = AssignedDuty(
                                    date = date,
                                    startTime = teammate.dutySchedule.dutyStartTime,
                                    endTime = teammate.dutySchedule.dutyEndTime,
                                    dutyType = "Regular (Auto)",
                                    notes = "Weekly Schedule"
                                )
                            ))
                        }
                    }
                }
            }
        }
        
        _dutyCalendar.value = calendarMap
    }

    fun addTeam(team: Team) {
        val currentTeams = _teams.value.toMutableList()
        currentTeams.add(team)
        updateTeams(currentTeams)
    }

    fun updateTeammate(teamName: String, teammateId: String, update: (Teammate) -> Teammate) {
        val currentTeams = _teams.value.toMutableList()
        val teamIndex = currentTeams.indexOfFirst { it.name == teamName }
        if (teamIndex != -1) {
            val team = currentTeams[teamIndex]
            val updatedTeammates = team.teammates.map { teammate ->
                if (teammate.id == teammateId) update(teammate) else teammate
            }
            val updatedTeam = team.copy(teammates = updatedTeammates)
            currentTeams[teamIndex] = updatedTeam
            updateTeams(currentTeams)
        }
    }

    fun setWeeklySchedule(teamName: String, teammateId: String, days: List<Int>, start: LocalTime, end: LocalTime) {
        updateTeammate(teamName, teammateId) { teammate ->
            teammate.copy(
                dutySchedule = teammate.dutySchedule.copy(
                    regularDutyDays = days,
                    dutyStartTime = start,
                    dutyEndTime = end
                )
            )
        }
    }

    fun assignDuty(teamName: String, teammateId: String, date: LocalDate, shift: DutyShift) {
        updateTeammate(teamName, teammateId) { teammate ->
            val newDuty = AssignedDuty(
                date = date,
                startTime = shift.startTime,
                endTime = shift.endTime,
                dutyType = shift.type,
                notes = shift.notes
            )
            val filteredDuties = teammate.dutySchedule.assignedDuties.filterNot { it.date == date }
            teammate.copy(dutySchedule = teammate.dutySchedule.copy(assignedDuties = filteredDuties + newDuty))
        }
    }

    fun toggleHoliday(teamName: String, teammateId: String, date: LocalDate) {
        updateTeammate(teamName, teammateId) { teammate ->
            val currentOffDays = teammate.dutySchedule.offDays.toMutableList()
            if (currentOffDays.contains(date)) {
                currentOffDays.remove(date)
            } else {
                currentOffDays.add(date)
            }
            val updatedDuties = teammate.dutySchedule.assignedDuties.filterNot { it.date == date }
            teammate.copy(dutySchedule = teammate.dutySchedule.copy(offDays = currentOffDays, assignedDuties = updatedDuties))
        }
    }

    fun removeDuty(duty: AssignedDuty) {
        val currentTeams = _teams.value.toMutableList()
        currentTeams.forEachIndexed { teamIndex, team ->
            val updatedTeammates = team.teammates.map { teammate ->
                if (teammate.dutySchedule.assignedDuties.contains(duty)) {
                    teammate.copy(dutySchedule = teammate.dutySchedule.copy(assignedDuties = teammate.dutySchedule.assignedDuties - duty))
                } else teammate
            }
            currentTeams[teamIndex] = team.copy(teammates = updatedTeammates)
        }
        updateTeams(currentTeams)
    }

    fun getTeammateDutyStats(teammateId: String): DutyStats {
        val teammate = _teams.value.flatMap { it.teammates }.find { it.id == teammateId } ?: return DutyStats(0, 0, 0, 0.0)
        
        val manualDuties = teammate.dutySchedule.assignedDuties
        val totalDutiesCount = manualDuties.size
        
        var totalOvertimeHours = 0.0
        manualDuties.forEach { duty ->
            val hours = calculateDuration(duty.startTime, duty.endTime)
            if (hours > 8.0) totalOvertimeHours += (hours - 8.0)
        }
        
        return DutyStats(
            totalDuties = totalDutiesCount,
            upcomingDuties = manualDuties.count { it.date.isAfter(LocalDate.now().minusDays(1)) },
            completedSwaps = _pendingSwaps.value.count {
                it.status == SwapStatus.APPROVED && (it.requesterId == teammateId || it.responderId == teammateId)
            },
            overtimeHours = totalOvertimeHours
        )
    }

    private fun calculateDuration(start: LocalTime, end: LocalTime): Double {
        val duration = if (end.isAfter(start)) {
            Duration.between(start, end)
        } else {
            Duration.ofHours(24).minus(Duration.between(end, start))
        }
        return duration.toMinutes() / 60.0
    }

    private fun loadAndCleanTeams(): List<Team> {
        return sharedPreferenceManager.getTeams()
    }

    fun addTeammate(teamName: String, teammate: Teammate) {
        val currentTeams = _teams.value.toMutableList()
        val teamIndex = currentTeams.indexOfFirst { it.name == teamName }
        if (teamIndex != -1) {
            val team = currentTeams[teamIndex]
            currentTeams[teamIndex] = team.copy(teammates = team.teammates + teammate)
            updateTeams(currentTeams)
        }
    }

    fun initiateDutySwap(requesterId: String, responderId: String, dutyDate: LocalDate, swapDate: LocalDate = dutyDate.plusDays(1)) {
        val requester = _teams.value.flatMap { it.teammates }.find { it.id == requesterId }
        val swap = DutySwap(
            requesterId = requesterId,
            responderId = responderId,
            requestDate = dutyDate,
            swapDate = swapDate,
            reason = "Duty Swap Request"
        )
        updateSwaps(_pendingSwaps.value + swap)
        
        notificationManager?.sendSwapRequestNotification(
            requester?.name ?: "A teammate",
            dutyDate.format(DateTimeFormatter.ofPattern("MMM d"))
        )
        
        viewModelScope.launch {
            _uiEvent.emit("Swap Request Sent Successfully")
        }
    }

    fun approveDutySwap(swap: DutySwap) {
        val currentSwaps = _pendingSwaps.value.toMutableList()
        val index = currentSwaps.indexOfFirst { it.id == swap.id }
        if (index != -1) {
            currentSwaps[index] = swap.copy(status = SwapStatus.APPROVED)
            updateSwaps(currentSwaps)
            
            val teams = _teams.value
            var requesterInfo: Pair<String, Teammate>? = null
            var responderInfo: Pair<String, Teammate>? = null
            
            teams.forEach { team ->
                team.teammates.forEach { teammate ->
                    if (teammate.id == swap.requesterId) requesterInfo = team.name to teammate
                    if (teammate.id == swap.responderId) responderInfo = team.name to teammate
                }
            }
            
            if (requesterInfo != null && responderInfo != null) {
                val (reqTeam, req) = requesterInfo!!
                val (resTeam, res) = responderInfo!!
                
                val reqDuty = getOrCreateDuty(req, swap.requestDate)
                val resDuty = getOrCreateDuty(res, swap.swapDate)
                
                updateTeammate(reqTeam, req.id) { r ->
                    val filtered = r.dutySchedule.assignedDuties.filterNot { it.date == swap.requestDate || it.date == swap.swapDate }
                    val newDuty = resDuty.copy(date = swap.swapDate, isSwapped = true, swappedWith = res.name)
                    r.copy(dutySchedule = r.dutySchedule.copy(assignedDuties = filtered + newDuty))
                }
                
                updateTeammate(resTeam, res.id) { r ->
                    val filtered = r.dutySchedule.assignedDuties.filterNot { it.date == swap.requestDate || it.date == swap.swapDate }
                    val newDuty = reqDuty.copy(date = swap.requestDate, isSwapped = true, swappedWith = req.name)
                    r.copy(dutySchedule = r.dutySchedule.copy(assignedDuties = filtered + newDuty))
                }
                
                notificationManager?.sendSwapApprovalNotification(
                    swap.requestDate.format(DateTimeFormatter.ofPattern("MMM d"))
                )
            }
            
            viewModelScope.launch {
                _uiEvent.emit("Swap Request Approved and Duties Exchanged")
            }
        }
    }

    private fun getOrCreateDuty(teammate: Teammate, date: LocalDate): AssignedDuty {
        return teammate.dutySchedule.assignedDuties.find { it.date == date }
            ?: AssignedDuty(
                date = date,
                startTime = teammate.dutySchedule.dutyStartTime,
                endTime = teammate.dutySchedule.dutyEndTime,
                dutyType = "Regular (Auto)",
                notes = "Scheduled"
            )
    }

    fun rejectDutySwap(swap: DutySwap) {
        val currentSwaps = _pendingSwaps.value.toMutableList()
        val index = currentSwaps.indexOfFirst { it.id == swap.id }
        if (index != -1) {
            currentSwaps[index] = swap.copy(status = SwapStatus.REJECTED)
            updateSwaps(currentSwaps)
            
            viewModelScope.launch {
                _uiEvent.emit("Swap Request Rejected")
            }
        }
    }

    /** Removes swaps that were already approved or rejected. */
    fun clearResolvedSwaps() {
        updateSwaps(_pendingSwaps.value.filter { it.status == SwapStatus.PENDING })
    }
}

data class CalendarDuty(
    val teammateId: String,
    val teammateName: String,
    val duty: AssignedDuty
)

data class DutyShift(val startTime: LocalTime, val endTime: LocalTime, val type: String = "Regular", val notes: String = "")
data class DutyStats(val totalDuties: Int, val upcomingDuties: Int, val completedSwaps: Int, val overtimeHours: Double = 0.0)
