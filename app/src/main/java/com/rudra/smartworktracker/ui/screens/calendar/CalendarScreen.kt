package com.rudra.smartworktracker.ui.screens.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.model.WorkType
import com.rudra.smartworktracker.ui.WorkLogUi
import com.rudra.smartworktracker.ui.components.LoadingShimmer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditEntry: (Long) -> Unit
) {
    val context = LocalContext.current
    val viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.factory(AppDatabase.getDatabase(context)))
    val uiState by viewModel.uiState.collectAsState()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val scrollState = rememberScrollState()

    // Handle share intent
    LaunchedEffect(Unit) {
        viewModel.shareWorkLog.collect { workLog ->
            val shareText = buildString {
                append("Work Entry Details\n")
                append("Date: ${workLog.formattedDate}\n")
                append("Type: ${workLog.workType.name.replace("_", " ")}\n")
                if (workLog.startTime != null && workLog.endTime != null) {
                    append("Time: ${workLog.startTime} - ${workLog.endTime}\n")
                    append("Duration: ${workLog.duration}")
                }
            }
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Work Entry")
            context.startActivity(shareIntent)
        }
    }

    // Handle template save
    LaunchedEffect(Unit) {
        viewModel.templateWorkLog.collect { workLog ->
            viewModel.copyWorkLog(workLog)
        }
    }

    // Stats card follows whichever month the grid shows
    LaunchedEffect(currentMonth) {
        viewModel.onQuickMonthSelect(currentMonth)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Work Calendar",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Row {
                        var showSearchBar by remember { mutableStateOf(false) }
                        var searchText by remember { mutableStateOf("") }

                        if (showSearchBar) {
                            OutlinedTextField(
                                value = searchText,
                                onValueChange = {
                                    searchText = it
                                    viewModel.updateSearchQuery(it)
                                },
                                placeholder = { Text("Search...", fontSize = 14.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Search, "Search")
                                },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        showSearchBar = false
                                        searchText = ""
                                        viewModel.updateSearchQuery("")
                                    }) {
                                        Icon(Icons.Default.Close, "Close")
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        } else {
                            IconButton(onClick = { showSearchBar = true }) {
                                Icon(Icons.Default.Search, "Search")
                            }
                        }

                        var showFilterDialog by remember { mutableStateOf(false) }

                        IconButton(onClick = { showFilterDialog = true }) {
                            Box {
                                Icon(
                                    Icons.Default.FilterList,
                                    "Filter",
                                    tint = if (uiState.activeFilters.isNotEmpty())
                                        MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (uiState.activeFilters.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(uiState.activeFilters.size.toString())
                                    }
                                }
                            }
                        }

                        if (showFilterDialog) {
                            FilterDialog(
                                currentFilters = uiState.activeFilters,
                                onFilterToggle = { workType ->
                                    if (workType in uiState.activeFilters) {
                                        viewModel.removeFilter(workType)
                                    } else {
                                        viewModel.addFilter(workType)
                                    }
                                },
                                onDismiss = { showFilterDialog = false },
                                onClearAll = {
                                    viewModel.clearFilters()
                                    showFilterDialog = false
                                }
                            )
                        }

                        AnimatedContent(
                            targetState = uiState.isMultiSelectMode,
                            transitionSpec = {
                                fadeIn() with fadeOut()
                            },
                            label = "multi select icon"
                        ) { isMultiSelect ->
                            IconButton(
                                onClick = { viewModel.toggleMultiSelectMode() },
                                modifier = Modifier.animateContentSize()
                            ) {
                                Icon(
                                    if (isMultiSelect) Icons.Default.Done else Icons.Default.SelectAll,
                                    contentDescription = if (isMultiSelect) "Done Selection" else "Select Multiple",
                                    tint = if (isMultiSelect)
                                        MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.isMultiSelectMode && uiState.multiSelectedDates.isNotEmpty(),
                enter = slideInHorizontally { fullWidth -> fullWidth } + fadeIn(),
                exit = slideOutHorizontally { fullWidth -> fullWidth } + fadeOut()
            ) {
                MultiSelectFABRow(
                    selectedCount = uiState.multiSelectedDates.size,
                    onMarkOffice = { viewModel.markSelectedDates(WorkType.OFFICE) },
                    onMarkHome = { viewModel.markSelectedDates(WorkType.HOME_OFFICE) },
                    onMarkOff = { viewModel.markSelectedDates(WorkType.OFF_DAY) },
                    onMarkOvertime = { viewModel.markSelectedDates(WorkType.OVERTIME) }
                )
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingShimmer(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                QuickStatsCard(
                    stats = uiState.monthlyStats,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                MonthNavigationCard(
                    currentMonth = currentMonth,
                    onMonthChange = { currentMonth = it },
                    // Quick chips used to update the stats only, leaving the grid on the old month
                    onQuickMonthSelect = { currentMonth = it },
                    isMultiSelectMode = uiState.isMultiSelectMode,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (uiState.activeFilters.isNotEmpty()) {
                item {
                    ActiveFiltersRow(
                        filters = uiState.activeFilters,
                        onRemoveFilter = viewModel::removeFilter,
                        onClearAll = viewModel::clearFilters,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                CalendarCard(
                    currentMonth = currentMonth,
                    workLogs = uiState.filteredWorkLogs,
                    selectedDate = uiState.selectedDate,
                    multiSelectedDates = uiState.multiSelectedDates,
                    isMultiSelectMode = uiState.isMultiSelectMode,
                    onDateSelected = viewModel::onDateSelected,
                    onLongPress = viewModel::onDateLongPress,
                    onSelectAll = { viewModel.selectAllDatesInMonth(currentMonth) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                WorkTypeLegend(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (uiState.selectedWorkLog != null && !uiState.isMultiSelectMode) {
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        WorkLogDetails(
                            workLog = uiState.selectedWorkLog!!,
                            onEdit = onNavigateToEditEntry,
                            onDelete = viewModel::deleteWorkLog,
                            onCopy = viewModel::copyWorkLog,
                            onShare = viewModel::shareWorkLog,
                            onSaveAsTemplate = viewModel::saveAsTemplate,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            item {
                MonthSummaryCard(
                    yearMonth = currentMonth,
                    workLogs = uiState.filteredWorkLogs,
                    modifier = Modifier.padding(16.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        }
    }
}

@Composable
fun MultiSelectFABRow(
    selectedCount: Int,
    onMarkOffice: () -> Unit,
    onMarkHome: () -> Unit,
    onMarkOff: () -> Unit,
    onMarkOvertime: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = CircleShape
        ) {
            Text(
                text = selectedCount.toString(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        MultiSelectActionButton(
            onClick = onMarkOffice,
            icon = Icons.Default.Business,
            text = "Office",
            backgroundColor = Color(0xFF2196F3)
        )

        MultiSelectActionButton(
            onClick = onMarkHome,
            icon = Icons.Default.Home,
            text = "Home",
            backgroundColor = Color(0xFFFF9800)
        )

        MultiSelectActionButton(
            onClick = onMarkOff,
            icon = Icons.Default.BeachAccess,
            text = "Off",
            backgroundColor = Color(0xFF4CAF50)
        )

        MultiSelectActionButton(
            onClick = onMarkOvertime,
            icon = Icons.Default.BusinessCenter,
            text = "OT",
            backgroundColor = Color(0xFFF44336)
        )
    }
}

@Composable
fun MultiSelectActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    backgroundColor: Color
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = backgroundColor,
        modifier = Modifier.size(56.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, text, modifier = Modifier.size(20.dp))
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QuickStatsCard(
    stats: MonthlyStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = stats.officeDays.toString(),
                label = "Office",
                color = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )

            VerticalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            StatItem(
                value = stats.homeDays.toString(),
                label = "Home",
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )

            VerticalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            StatItem(
                value = stats.offDays.toString(),
                label = "Off",
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )

            VerticalDivider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            StatItem(
                value = stats.totalHours,
                label = "Hours",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MonthNavigationCard(
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onQuickMonthSelect: (YearMonth) -> Unit,
    isMultiSelectMode: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onMonthChange(currentMonth.minusMonths(1)) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.ChevronLeft, "Previous Month")
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = currentMonth,
                        transitionSpec = {
                            fadeIn() + slideInHorizontally() togetherWith
                                    fadeOut() + slideOutHorizontally()
                        },
                        label = "month name"
                    ) { month ->
                        Text(
                            text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (currentMonth != YearMonth.now()) {
                        TextButton(
                            onClick = { onMonthChange(YearMonth.now()) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Today", fontSize = 12.sp)
                        }
                    }
                }

                IconButton(
                    onClick = { onMonthChange(currentMonth.plusMonths(1)) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.ChevronRight, "Next Month")
                }
            }

            if (!isMultiSelectMode) {
                QuickMonthNavigation(
                    currentMonth = currentMonth,
                    onMonthChange = onQuickMonthSelect,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun QuickMonthNavigation(
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentYear = YearMonth.now().year
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(months) { month ->
            val monthIndex = months.indexOf(month) + 1
            val isSelected = currentMonth.monthValue == monthIndex
            val isCurrentMonth = monthIndex == YearMonth.now().monthValue &&
                    currentYear == YearMonth.now().year

            FilterChip(
                selected = isSelected,
                onClick = { onMonthChange(YearMonth.of(currentYear, monthIndex)) },
                label = {
                    Text(
                        month,
                        fontSize = if (isCurrentMonth) 13.sp else 12.sp,
                        fontWeight = if (isCurrentMonth) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = if (isCurrentMonth && !isSelected)
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                else null
            )
        }
    }
}

@Composable
fun ActiveFiltersRow(
    filters: List<WorkType>,
    onRemoveFilter: (WorkType) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                filters.forEach { filter ->
                    AssistChip(
                        onClick = { onRemoveFilter(filter) },
                        label = { Text(filter.name.replace("_", " ")) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                "Remove",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = getWorkTypeColor(filter).copy(alpha = 0.2f)
                        )
                    )
                }
            }

            TextButton(
                onClick = onClearAll,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("Clear All", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CalendarCard(
    currentMonth: YearMonth,
    workLogs: List<WorkLogUi>,
    selectedDate: LocalDate,
    multiSelectedDates: List<LocalDate>,
    isMultiSelectMode: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    onLongPress: (LocalDate) -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isMultiSelectMode)
                        "Select multiple dates"
                        else "Tap date to view details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AnimatedVisibility(visible = isMultiSelectMode) {
                    SelectionInfo(
                        selectedCount = multiSelectedDates.size,
                        onSelectAll = onSelectAll
                    )
                }
            }

            DaysOfWeekHeader()

            CalendarGrid(
                currentMonth = currentMonth,
                workLogs = workLogs,
                selectedDate = selectedDate,
                multiSelectedDates = multiSelectedDates,
                isMultiSelectMode = isMultiSelectMode,
                onDateSelected = onDateSelected,
                onDateLongPress = onLongPress
            )
        }
    }
}

@Composable
fun SelectionInfo(
    selectedCount: Int,
    onSelectAll: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Badge(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Text(selectedCount.toString())
        }

        if (selectedCount < 35) {
            TextButton(
                onClick = onSelectAll,
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("Select All", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun DaysOfWeekHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        val days = DayOfWeek.values()
        days.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    workLogs: List<WorkLogUi>,
    selectedDate: LocalDate,
    multiSelectedDates: List<LocalDate>,
    isMultiSelectMode: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    onDateLongPress: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek.value - 1
    val today = LocalDate.now()

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(daysInMonth + firstDayOfMonth) { dayIndex ->
            if (dayIndex >= firstDayOfMonth) {
                val date = currentMonth.atDay(dayIndex - firstDayOfMonth + 1)
                val workLogForDay = workLogs.find {
                    it.date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate() == date
                }
                val isSelected = if (isMultiSelectMode)
                    multiSelectedDates.contains(date)
                    else date == selectedDate

                CalendarDay(
                    date = date,
                    workType = workLogForDay?.workType,
                    isSelected = isSelected,
                    isToday = date == today,
                    isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY ||
                            date.dayOfWeek == DayOfWeek.SUNDAY,
                    onDateSelected = { onDateSelected(date) },
                    onDateLongPress = { onDateLongPress(date) }
                )
            } else {
                Spacer(modifier = Modifier.aspectRatio(1f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarDay(
    date: LocalDate,
    workType: WorkType?,
    isSelected: Boolean,
    isToday: Boolean,
    isWeekend: Boolean,
    onDateSelected: () -> Unit,
    onDateLongPress: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isToday -> MaterialTheme.colorScheme.primaryContainer
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "background color"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> if (isWeekend)
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(300),
        label = "text color"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = tween(300),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (isToday && !isSelected)
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
                else if (isWeekend && !isSelected && !isToday)
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
                else Modifier
            )
            .combinedClickable(
                onClick = onDateSelected,
                onLongClick = onDateLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = textColor,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )

            workType?.let {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(getWorkTypeColor(it))
                )
            }
        }
    }
}

@Composable
fun WorkTypeLegend(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                "Work Type Legend",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(WorkType.entries.toList()) { workType ->
                    LegendItem(
                        color = getWorkTypeColor(workType),
                        label = workType.name.replace("_", " ")
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MonthSummaryCard(
    yearMonth: YearMonth,
    workLogs: List<WorkLogUi>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${yearMonth.format(DateTimeFormatter.ofPattern("MMMM"))} Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            val workTypeCounts = workLogs.groupBy { it.workType }
                .mapValues { it.value.size }

            workTypeCounts.forEach { (workType, count) ->
                SummaryRow(
                    workType = workType,
                    count = count,
                    color = getWorkTypeColor(workType)
                )
            }

            if (workTypeCounts.isEmpty()) {
                Text(
                    "No entries for this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SummaryRow(
    workType: WorkType,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = workType.name.replace("_", " "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = "$count day${if (count != 1) "s" else ""}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun WorkLogDetails(
    workLog: WorkLogUi,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onCopy: (WorkLogUi) -> Unit,
    onShare: (WorkLogUi) -> Unit,
    onSaveAsTemplate: (WorkLogUi) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        "Work Entry Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        workLog.formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(
                        onClick = { onCopy(workLog) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copy Entry")
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { onEdit(workLog.id) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Edit, "Edit Entry")
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { onDelete(workLog.id) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(Icons.Default.Delete, "Delete Entry")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Work Type:",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                WorkTypeChip(workLog.workType)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Start Time",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            workLog.startTime ?: "Not set",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "End Time",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            workLog.endTime ?: "Not set",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Duration",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            workLog.duration,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onShare(workLog) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }

                OutlinedButton(
                    onClick = { onSaveAsTemplate(workLog) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Bookmark, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Template")
                }
            }
        }
    }
}

@Composable
fun WorkTypeChip(workType: WorkType) {
    val color = getWorkTypeColor(workType)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = workType.name.replace("_", " "),
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}

@Composable
fun FilterDialog(
    currentFilters: List<WorkType>,
    onFilterToggle: (WorkType) -> Unit,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Filter by Work Type",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                WorkType.entries.forEach { workType ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFilterToggle(workType) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = workType in currentFilters,
                            onCheckedChange = { onFilterToggle(workType) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(getWorkTypeColor(workType))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = workType.name.replace("_", " "),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onClearAll) {
                Text("Clear All", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

fun getWorkTypeColor(workType: WorkType): Color {
    return when (workType) {
        WorkType.OFFICE -> Color(0xFF2196F3)
        WorkType.HOME_OFFICE -> Color(0xFFFF9800)
        WorkType.OFF_DAY -> Color(0xFF4CAF50)
        WorkType.EXTRA_WORK -> Color(0xFF9C27B0)
        WorkType.OVERTIME -> Color(0xFFF44336)
    }
}

data class MonthlyStats(
    val officeDays: Int = 0,
    val homeDays: Int = 0,
    val offDays: Int = 0,
    val extraDays: Int = 0,
    val totalHours: String = "0h"
)
