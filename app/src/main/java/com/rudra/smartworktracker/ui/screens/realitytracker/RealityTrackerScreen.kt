package com.rudra.smartworktracker.ui.screens.realitytracker

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.model.RealityCategory
import com.rudra.smartworktracker.model.RealityEntry
import com.rudra.smartworktracker.model.RealityEntryType
import com.rudra.smartworktracker.ui.components.EmptyStateCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealityTrackerScreen(
    onNavigateBack: (() -> Unit)? = null,
    viewModel: RealityTrackerViewModel = viewModel(
        factory = RealityTrackerViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RealityEntry?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            val result = snackbarHostState.showSnackbar(message, actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reality Tracker", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add entry") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TimeRange.entries.forEachIndexed { index, range ->
                        SegmentedButton(
                            selected = uiState.range == range,
                            onClick = { viewModel.setTimeRange(range) },
                            shape = SegmentedButtonDefaults.itemShape(index, TimeRange.entries.size)
                        ) { Text(range.label, maxLines = 1) }
                    }
                }
            }

            item { RealityStatsCard(stats = uiState.stats, range = uiState.range) }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EntryFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text("${filter.label} (${uiState.counts[filter] ?: 0})") }
                        )
                    }
                }
            }

            if (!uiState.isLoading && uiState.entries.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        title = if (uiState.filter == EntryFilter.ALL) "Nothing planned yet" else "No ${uiState.filter.label.lowercase()} entries",
                        message = "Write down goals, promises and plans, then tick them off to see how much actually gets done.",
                        modifier = Modifier.fillMaxWidth(),
                        actionText = if (uiState.filter == EntryFilter.ALL) "Add entry" else null,
                        onAction = if (uiState.filter == EntryFilter.ALL) ({ showAddDialog = true }) else null
                    )
                }
            } else {
                items(items = uiState.entries, key = { it.id }) { entry ->
                    RealityEntryItem(
                        entry = entry,
                        onToggleComplete = { viewModel.toggleCompletion(entry) },
                        onEdit = { editing = entry },
                        onDelete = { viewModel.deleteEntry(entry) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        if (showAddDialog) {
            RealityEntryDialog(
                initial = null,
                onDismiss = { showAddDialog = false },
                onConfirm = { type, title, description, category, due ->
                    viewModel.addEntry(type, title, description, category, due)
                    showAddDialog = false
                }
            )
        }

        editing?.let { entry ->
            RealityEntryDialog(
                initial = entry,
                onDismiss = { editing = null },
                onConfirm = { type, title, description, category, due ->
                    viewModel.updateEntry(
                        entry.copy(type = type, title = title, description = description, category = category, targetDate = due)
                    )
                    editing = null
                }
            )
        }
    }
}

@Composable
fun RealityStatsCard(stats: RealityStats, range: TimeRange) {
    val rate by animateFloatAsState(stats.completionRate / 100f, tween(700), label = "rate")
    val rateColor = when {
        stats.totalPlanned == 0 -> MaterialTheme.colorScheme.primary
        stats.completionRate >= 70 -> Color(0xFF43A047)
        stats.completionRate >= 40 -> Color(0xFFFFA000)
        else -> Color(0xFFE53935)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
                    CircularProgressIndicator(
                        progress = { rate },
                        modifier = Modifier.fillMaxSize(),
                        color = rateColor,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round,
                        trackColor = rateColor.copy(alpha = 0.15f)
                    )
                    Text("${stats.completionRate.toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Follow-through", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (stats.totalPlanned == 0) "Nothing planned ${range.label.lowercase()}"
                        else "${stats.totalCompleted} of ${stats.totalPlanned} done ${range.label.lowercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (stats.overdue > 0) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${stats.overdue} overdue",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            RealityEntryType.entries.forEach { type ->
                val (planned, done) = stats.getStatsByType(type)
                TypeProgressRow(type, planned, done)
                Spacer(Modifier.height(8.dp))
            }

            insightFor(stats, range)?.let { insight ->
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(insight, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }
    }
}

private fun insightFor(stats: RealityStats, range: TimeRange): String? {
    if (stats.totalPlanned < 3) return null
    val rate = stats.completionRate.toInt()
    val weakest = stats.weakestCategory?.let { " ${it.displayName()} items slip the most." } ?: ""
    return when {
        rate >= 80 -> "Great follow-through: $rate% of what you planned ${range.label.lowercase()} is done."
        rate >= 50 -> "You finish about $rate% of your plans.$weakest"
        else -> "You plan more than you finish (${100 - rate}% still open). Try committing to fewer things.$weakest"
    }
}

@Composable
private fun TypeProgressRow(type: RealityEntryType, planned: Int, done: Int) {
    val color = type.color()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(type.icon(), contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(type.displayName() + "s", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(76.dp))
        LinearProgressIndicator(
            progress = { if (planned > 0) done.toFloat() / planned else 0f },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.width(8.dp))
        Text("$done/$planned", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RealityEntryItem(
    entry: RealityEntry,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val overdue = RealityTrackerViewModel.isOverdue(entry)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                entry.isCompleted -> MaterialTheme.colorScheme.surfaceContainerLow
                overdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = entry.isCompleted, onCheckedChange = { onToggleComplete() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (entry.isCompleted) TextDecoration.LineThrough else null,
                    color = if (entry.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (entry.description.isNotBlank()) {
                    Text(
                        text = entry.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    EntryTypeChip(type = entry.type)
                    CategoryChip(category = entry.category)
                    RealityTrackerViewModel.dueDateOf(entry)?.let { due ->
                        DueChip(due = due, overdue = overdue, done = entry.isCompleted)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DueChip(due: LocalDate, overdue: Boolean, done: Boolean) {
    val today = LocalDate.now()
    val label = when (val days = ChronoUnit.DAYS.between(today, due)) {
        0L -> "Today"
        1L -> "Tomorrow"
        -1L -> "Yesterday"
        else -> if (days in 2..6) due.format(DateTimeFormatter.ofPattern("EEE")) else due.format(DateTimeFormatter.ofPattern("d MMM"))
    }
    val color = when {
        done -> MaterialTheme.colorScheme.onSurfaceVariant
        overdue -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.1f)) {
        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(12.dp), tint = color)
            Spacer(Modifier.width(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
fun EntryTypeChip(type: RealityEntryType) {
    val color = type.color()
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.1f)) {
        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(type.icon(), contentDescription = null, modifier = Modifier.size(12.dp), tint = color)
            Spacer(modifier = Modifier.width(2.dp))
            Text(text = type.displayName(), style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
fun CategoryChip(category: RealityCategory) {
    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text = category.displayName(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RealityEntryDialog(
    initial: RealityEntry?,
    onDismiss: () -> Unit,
    onConfirm: (RealityEntryType, String, String, RealityCategory, Long?) -> Unit
) {
    var selectedType by rememberSaveable { mutableStateOf(initial?.type ?: RealityEntryType.PLAN) }
    var title by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var description by rememberSaveable { mutableStateOf(initial?.description ?: "") }
    var selectedCategory by rememberSaveable { mutableStateOf(initial?.category ?: RealityCategory.GENERAL) }
    var dueDate by rememberSaveable { mutableStateOf(initial?.let { RealityTrackerViewModel.dueDateOf(it) }?.toEpochDay()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New entry" else "Edit entry") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    RealityEntryType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            shape = SegmentedButtonDefaults.itemShape(index, RealityEntryType.entries.size),
                            icon = {}
                        ) { Text(type.displayName(), maxLines = 1) }
                    }
                }
                Text(
                    text = when (selectedType) {
                        RealityEntryType.GOAL -> "Something you want to achieve"
                        RealityEntryType.PROMISE -> "A commitment you made to someone"
                        RealityEntryType.PLAN -> "A task you intend to do"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(300) },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = {
                            Text(dueDate?.let { "Due ${LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("EEE, d MMM"))}" } ?: "Add due date")
                        },
                        leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    if (dueDate != null) {
                        IconButton(onClick = { dueDate = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear due date", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    RealityCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.displayName()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dueMillis = dueDate?.let { LocalDate.ofEpochDay(it).atStartOfDay(zone).toInstant().toEpochMilli() }
                    onConfirm(selectedType, title, description, selectedCategory, dueMillis)
                },
                enabled = title.isNotBlank()
            ) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        // DatePicker works in UTC days; convert explicitly so the chosen day doesn't shift by timezone
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (dueDate?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now())
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        dueDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}

private fun RealityEntryType.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun RealityCategory.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun RealityEntryType.icon(): ImageVector = when (this) {
    RealityEntryType.GOAL -> Icons.Default.Flag
    RealityEntryType.PROMISE -> Icons.Default.Handshake
    RealityEntryType.PLAN -> Icons.Default.Checklist
}

private fun RealityEntryType.color(): Color = when (this) {
    RealityEntryType.GOAL -> Color(0xFF1E88E5)
    RealityEntryType.PROMISE -> Color(0xFF8E24AA)
    RealityEntryType.PLAN -> Color(0xFF00897B)
}
