package com.rudra.smartworktracker.ui.screens.team

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.data.SharedPreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*
import java.time.format.*
import java.util.Locale

@Composable
fun SecondaryScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    edgePadding: Dp = 16.dp,
    divider: @Composable () -> Unit = {},
    tabs: @Composable () -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        edgePadding = edgePadding,
        divider = divider,
        indicator = { tabPositions ->
            if (selectedTabIndex < tabPositions.size) {
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = contentColor
                )
            }
        },
        tabs = tabs
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferenceManager = remember { SharedPreferenceManager(context) }
    val teamViewModel: TeamViewModel = viewModel(
        factory = TeamViewModelFactory(sharedPreferenceManager)
    )

    // Initialize notification manager
    val notificationManager = remember { DutyNotificationManager(context) }
    LaunchedEffect(Unit) {
        teamViewModel.setNotificationManager(notificationManager)
    }

    // Permission Launchers
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Handle permission denial if needed
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Show contact picker if granted
        }
    }

    // Request permissions on start for Android 13+
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val teams by teamViewModel.teams.collectAsState()
    val dutyCalendar by teamViewModel.dutyCalendar.collectAsState()
    val pendingSwaps by teamViewModel.pendingSwaps.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddTeamDialog by remember { mutableStateOf(false) }
    var showAddTeammateDialog by remember { mutableStateOf(false) }
    var showDutyCalendar by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var selectedTeam by remember { mutableStateOf<Team?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val tabs = listOf("Teams", "Calendar", "Overview", "Swaps")
    
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surface
        )
    )

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        teamViewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Team Manager", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { 
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                            showContactPicker = true 
                        } else {
                            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    }) {
                        Icon(Icons.Default.Contacts, contentDescription = "Import Contacts", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { 
                        selectedTeam = teams.firstOrNull()
                        showDutyCalendar = true 
                    }) {
                        BadgedBox(
                            badge = {
                                val openSwaps = pendingSwaps.count { it.status == SwapStatus.PENDING }
                                if (openSwaps > 0) Badge { Text(openSwaps.toString()) }
                            }
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Duty Operations", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedTab == 0,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(
                        onClick = { showAddTeammateDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Teammate")
                    }
                    
                    FloatingActionButton(
                        onClick = { showAddTeamDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "Add Team")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(backgroundGradient).padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                SearchBar(query = searchQuery, onQueryChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth().padding(16.dp))

                SecondaryScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(text = title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium) },
                            icon = {
                                val icon = when (title) {
                                    "Teams" -> Icons.Default.Group
                                    "Calendar" -> Icons.Default.CalendarMonth
                                    "Overview" -> Icons.Default.Dashboard
                                    "Swaps" -> Icons.Default.SwapHoriz
                                    else -> Icons.Default.Group
                                }
                                Icon(icon, contentDescription = title, modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> TeamsTab(
                        teams = teams,
                        searchQuery = searchQuery,
                        onTeamSelected = { team -> selectedTeam = team },
                        onAddTeammate = { team -> selectedTeam = team; showAddTeammateDialog = true },
                        onManageDuty = { team -> selectedTeam = team; showDutyCalendar = true },
                        onCallTeammate = { phoneNumber ->
                            val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phoneNumber") }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    1 -> CalendarTab(
                        dutyCalendar = dutyCalendar,
                        selectedDate = selectedDate,
                        onDateSelected = { date -> selectedDate = date },
                        onDutyClick = { /* Show details */ },
                        onSwapRequest = { duty -> 
                            // This would ideally open teammate selection
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    2 -> TeamOverviewTab(
                        teams = teams,
                        searchQuery = searchQuery,
                        modifier = Modifier.fillMaxSize()
                    )

                    3 -> SwapsTab(
                        pendingSwaps = pendingSwaps,
                        onApproveSwap = { swap -> teamViewModel.approveDutySwap(swap) },
                        onRejectSwap = { swap -> teamViewModel.rejectDutySwap(swap) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (showAddTeamDialog) {
            AddTeamDialog(onDismiss = { showAddTeamDialog = false }, onConfirm = { team -> teamViewModel.addTeam(team); showAddTeamDialog = false })
        }

        if (showAddTeammateDialog) {
            AddTeammateDialog(
                teams = teams,
                selectedTeam = selectedTeam,
                onDismiss = { showAddTeammateDialog = false; selectedTeam = null },
                onAddFromContacts = { 
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        showContactPicker = true 
                    } else {
                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                },
                onConfirm = { teamName, teammate -> teamViewModel.addTeammate(teamName, teammate); showAddTeammateDialog = false; selectedTeam = null }
            )
        }

        if (showDutyCalendar && selectedTeam != null) {
            DutyCalendarDialog(
                team = selectedTeam!!,
                onDismiss = { showDutyCalendar = false; selectedTeam = null },
                onAssignDuty = { teammate, date, shift -> teamViewModel.assignDuty(selectedTeam!!.name, teammate.id, date, shift) },
                onRemoveDuty = { duty -> teamViewModel.removeDuty(duty) },
                onToggleHoliday = { teammate, date -> teamViewModel.toggleHoliday(selectedTeam!!.name, teammate.id, date) },
                onSetWeeklySchedule = { teammate, days, start, end -> teamViewModel.setWeeklySchedule(selectedTeam!!.name, teammate.id, days, start, end) },
                onSwapRequest = { requester, responder, date -> teamViewModel.initiateDutySwap(requester.id, responder.id, date) },
                getTeammateStats = { teammateId -> teamViewModel.getTeammateDutyStats(teammateId) }
            )
        }

        if (showContactPicker) {
            ContactPickerDialog(
                onDismiss = { showContactPicker = false },
                onContactSelected = { contact -> 
                    if (teams.isNotEmpty()) {
                        val teamName = selectedTeam?.name ?: teams.first().name
                        val teammate = Teammate(
                            name = contact.name,
                            phoneNumbers = contact.phoneNumbers,
                            email = contact.email,
                            contactId = contact.id
                        )
                        teamViewModel.addTeammate(teamName, teammate)
                    }
                }
            )
        }
    }
}

@Composable
fun TeamOverviewTab(teams: List<Team>, searchQuery: String, modifier: Modifier = Modifier) {
    val allTeammates = teams.flatMap { it.teammates }.filter { it.name.contains(searchQuery, true) }
    
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Team Duty Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Weekly regular working days and times", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        if (allTeammates.isEmpty()) {
            item { EmptyState(icon = Icons.Default.People, title = "No members found", description = "Add members to see their schedule") }
        } else {
            items(allTeammates) { teammate ->
                TeammateOverviewCard(teammate)
            }
        }
    }
}

@Composable
fun TeammateOverviewCard(teammate: Teammate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Text(teammate.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(teammate.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(teammate.role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            
            Text("Weekly Schedule", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            
            val days = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEachIndexed { index, day ->
                    val isWorking = teammate.dutySchedule.regularDutyDays.contains(index + 1)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isWorking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(day, color = if (isWorking) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
            if (teammate.dutySchedule.regularDutyDays.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${teammate.dutySchedule.dutyStartTime.format(DateTimeFormatter.ofPattern("hh:mm a"))} - ${teammate.dutySchedule.dutyEndTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search teams, members...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, contentDescription = null) } },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun TeamsTab(teams: List<Team>, searchQuery: String, onTeamSelected: (Team) -> Unit, onAddTeammate: (Team) -> Unit, onManageDuty: (Team) -> Unit, onCallTeammate: (String) -> Unit, modifier: Modifier = Modifier) {
    val filteredTeams = if (searchQuery.isEmpty()) teams else teams.filter { it.name.contains(searchQuery, true) || it.teammates.any { t -> t.name.contains(searchQuery, true) } }
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(16.dp)) {
        if (filteredTeams.isEmpty()) {
            item { EmptyState(icon = Icons.Default.Group, title = "No teams found", description = "Create a team to get started") }
        } else {
            items(filteredTeams, key = { it.id }) { team ->
                TeamCard(team = team, onTeamSelected = { onTeamSelected(team) }, onAddTeammate = { onAddTeammate(team) }, onManageDuty = { onManageDuty(team) }, onCallTeammate = onCallTeammate)
            }
        }
    }
}

@Composable
fun TeamCard(team: Team, onTeamSelected: () -> Unit, onAddTeammate: () -> Unit, onManageDuty: () -> Unit, onCallTeammate: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.clickable { onTeamSelected() }.animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(modifier = Modifier.size(12.dp), shape = CircleShape, color = Color(team.teamColor)) {}
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = team.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (team.description.isNotEmpty()) Text(text = team.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TeamStatItem(icon = Icons.Default.People, value = "${team.teammates.size}", label = "Members")
                TeamStatItem(icon = Icons.Default.Sync, value = "${team.dutyCycleDays}d", label = "Cycle")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onAddTeammate, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Add Member", fontSize = 12.sp)
                }
                OutlinedButton(onClick = { onManageDuty() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Duties", fontSize = 12.sp)
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Team Members", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    team.teammates.forEach { teammate ->
                        TeammateRow(teammate = teammate, onCall = { teammate.phoneNumbers.firstOrNull()?.let(onCallTeammate) })
                    }
                }
            }
        }
    }
}

@Composable
fun TeammateRow(teammate: Teammate, onCall: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(teammate.name.take(1).uppercase(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = teammate.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(text = teammate.role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onCall, enabled = teammate.phoneNumbers.isNotEmpty()) {
                Icon(Icons.Default.Call, null, tint = if (teammate.phoneNumbers.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun CalendarTab(
    dutyCalendar: Map<LocalDate, List<CalendarDuty>>, 
    selectedDate: LocalDate, 
    onDateSelected: (LocalDate) -> Unit, 
    onDutyClick: (AssignedDuty) -> Unit, 
    onSwapRequest: (AssignedDuty) -> Unit, 
    modifier: Modifier = Modifier
) {
    val weekDates = remember(selectedDate) {
        val startOfWeek = selectedDate.with(DayOfWeek.MONDAY)
        (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }
    
    Column(modifier = modifier) {
        // Week Selector Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onDateSelected(LocalDate.now()) }) {
                        Text("Today")
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    weekDates.forEach { date ->
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()
                        val hasDuties = dutyCalendar[date]?.isNotEmpty() == true
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (isToday && !isSelected) 1.dp else 0.dp,
                                        color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()).take(1), 
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, 
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = date.dayOfMonth.toString(), 
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, 
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            // Duty Indicator Dot
                            if (hasDuties) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                                )
                            }
                        }
                    }
                }
            }
        }

        val dutiesForDate = dutyCalendar[selectedDate] ?: emptyList()
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (dutiesForDate.isEmpty()) {
                item { 
                    EmptyState(
                        icon = Icons.Default.EventAvailable, 
                        title = "Free Day", 
                        description = "No team duties scheduled for ${selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))}"
                    ) 
                }
            } else {
                items(dutiesForDate) { calendarDuty -> 
                    val duty = calendarDuty.duty
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDutyClick(duty) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (duty.isSwapped) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f) 
                                            else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar/Initial
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = calendarDuty.teammateName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = calendarDuty.teammateName,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Schedule, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${duty.startTime.format(DateTimeFormatter.ofPattern("hh:mm a"))} - ${duty.endTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (duty.dutyType.isNotEmpty()) {
                                    Text(
                                        text = duty.dutyType,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            
                            if (!duty.isSwapped) {
                                IconButton(
                                    onClick = { onSwapRequest(duty) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = "Request Swap", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SwapsTab(pendingSwaps: List<DutySwap>, onApproveSwap: (DutySwap) -> Unit, onRejectSwap: (DutySwap) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(16.dp)) {
        if (pendingSwaps.isEmpty()) {
            item { EmptyState(icon = Icons.Default.SwapHoriz, title = "No pending swaps", description = "All caught up!") }
        } else {
            // Pending first; resolved swaps stay visible as history without action buttons
            items(pendingSwaps.sortedBy { it.status != SwapStatus.PENDING }, key = { it.id }) { swap ->
                val isPending = swap.status == SwapStatus.PENDING
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPending) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Duty Swap Request", fontWeight = FontWeight.ExtraBold)
                            if (!isPending) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(swap.status.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                        Text(
                            "${swap.requestDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))} ↔ " +
                                swap.swapDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))
                        )
                        if (isPending) {
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = { onRejectSwap(swap) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Reject") }
                                Button(onClick = { onApproveSwap(swap) }, modifier = Modifier.weight(1f)) { Text("Approve") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamStatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(description, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AddTeamDialog(onDismiss: () -> Unit, onConfirm: (Team) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Team", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Team Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onConfirm(Team(name = name, description = desc)) }, enabled = name.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddTeammateDialog(teams: List<Team>, selectedTeam: Team?, onDismiss: () -> Unit, onAddFromContacts: () -> Unit, onConfirm: (String, Teammate) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Team Member") }
    var selectedTeamName by remember { mutableStateOf(selectedTeam?.name ?: teams.firstOrNull()?.name ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Team Member", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (selectedTeam == null) {
                    Text("Select Team", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(teams) { team ->
                            FilterChip(selected = selectedTeamName == team.name, onClick = { selectedTeamName = team.name }, label = { Text(team.name) })
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = onAddFromContacts, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Contacts, null); Spacer(Modifier.width(8.dp)); Text("Import from Contacts")
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(selectedTeamName, Teammate(name = name, phoneNumbers = listOf(phone), role = role)) }, enabled = name.isNotBlank() && selectedTeamName.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DutyCalendarDialog(
    team: Team,
    onDismiss: () -> Unit,
    onAssignDuty: (Teammate, LocalDate, DutyShift) -> Unit,
    onRemoveDuty: (AssignedDuty) -> Unit,
    onToggleHoliday: (Teammate, LocalDate) -> Unit,
    onSetWeeklySchedule: (Teammate, List<Int>, LocalTime, LocalTime) -> Unit,
    onSwapRequest: (Teammate, Teammate, LocalDate) -> Unit,
    getTeammateStats: (String) -> DutyStats
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTeammate by remember { mutableStateOf<Teammate?>(team.teammates.firstOrNull()) }
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(17, 0)) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isPickingStartTime by remember { mutableStateOf(true) }
    var showSwapSelection by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Duty Operations", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Select Member", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(team.teammates) { teammate ->
                        FilterChip(selected = selectedTeammate?.id == teammate.id, onClick = { selectedTeammate = teammate }, label = { Text(teammate.name) })
                    }
                }
                
                selectedTeammate?.let { teammate ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Weekly Regulars", fontWeight = FontWeight.Bold)
                            val days = listOf("M", "T", "W", "T", "F", "S", "S")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                days.forEachIndexed { idx, day ->
                                    val dayNum = idx + 1
                                    val active = teammate.dutySchedule.regularDutyDays.contains(dayNum)
                                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).clickable {
                                        val newDays = teammate.dutySchedule.regularDutyDays.toMutableList().apply { if (active) remove(dayNum) else add(dayNum) }
                                        onSetWeeklySchedule(teammate, newDays, teammate.dutySchedule.dutyStartTime, teammate.dutySchedule.dutyEndTime)
                                    }, contentAlignment = Alignment.Center) {
                                        Text(day, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                        IconButton(onClick = { selectedDate = selectedDate.minusDays(1) }) { Icon(Icons.Default.ChevronLeft, null) }
                        Text(selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { selectedDate = selectedDate.plusDays(1) }) { Icon(Icons.Default.ChevronRight, null) }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { isPickingStartTime = true; showTimePicker = true }, modifier = Modifier.weight(1f)) { Text(startTime.format(DateTimeFormatter.ofPattern("hh:mm a"))) }
                    Button(onClick = { isPickingStartTime = false; showTimePicker = true }, modifier = Modifier.weight(1f)) { Text(endTime.format(DateTimeFormatter.ofPattern("hh:mm a"))) }
                }

                selectedTeammate?.let { teammate ->
                    val isHoliday = teammate.dutySchedule.offDays.contains(selectedDate)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onToggleHoliday(teammate, selectedDate) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (isHoliday) Color(0xFF4CAF50) else Color(0xFFFF5252))) {
                            Icon(if (isHoliday) Icons.Default.Work else Icons.Default.BeachAccess, null); Spacer(Modifier.width(4.dp)); Text(if (isHoliday) "Work" else "Holiday")
                        }
                        Button(onClick = { onAssignDuty(teammate, selectedDate, DutyShift(startTime, endTime, "")) }, modifier = Modifier.weight(1f), enabled = !isHoliday) {
                            Icon(Icons.Default.Save, null); Spacer(Modifier.width(4.dp)); Text("Save")
                        }
                    }
                    Button(onClick = { showSwapSelection = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                        Icon(Icons.Default.SwapHoriz, null); Spacer(Modifier.width(8.dp)); Text("Swap Request")
                    }
                }

                Text("Existing Duties", fontWeight = FontWeight.Bold)
                team.teammates.flatMap { t -> t.dutySchedule.assignedDuties.filter { it.date == selectedDate }.map { it to t.name } }.forEach { (duty, name) ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Bold)
                                Text("${duty.startTime.format(DateTimeFormatter.ofPattern("hh:mm a"))} - ${duty.endTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}")
                            }
                            IconButton(onClick = { onRemoveDuty(duty) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }

    if (showSwapSelection && selectedTeammate != null) {
        SwapSelectionDialog(teammates = team.teammates.filter { it.id != selectedTeammate!!.id }, onDismiss = { showSwapSelection = false }, onTeammateSelected = { onSwapRequest(selectedTeammate!!, it, selectedDate); showSwapSelection = false })
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = if (isPickingStartTime) startTime.hour else endTime.hour, initialMinute = if (isPickingStartTime) startTime.minute else endTime.minute)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = { TextButton(onClick = {
                val newTime = LocalTime.of(timeState.hour, timeState.minute)
                if (isPickingStartTime) startTime = newTime else endTime = newTime
                showTimePicker = false
            }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = timeState) }
        )
    }
}

@Composable
fun SwapSelectionDialog(teammates: List<Teammate>, onDismiss: () -> Unit, onTeammateSelected: (Teammate) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Swap Partner", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(teammates) { teammate ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onTeammateSelected(teammate) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Text(teammate.name.take(1), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(12.dp)); Text(teammate.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun loadContacts(context: Context): List<Contact> {
    val contacts = mutableListOf<Contact>()
    val cr: ContentResolver = context.contentResolver
    val cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
    cursor?.use {
        while (it.moveToNext()) {
            val id = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)) ?: ""
            val phoneNumbers = mutableListOf<String>()
            cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?", arrayOf(id), null)?.use { pCursor ->
                while (pCursor.moveToNext()) { phoneNumbers.add(pCursor.getString(pCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))) }
            }
            if (name.isNotBlank() && phoneNumbers.isNotEmpty()) contacts.add(Contact(id, name, phoneNumbers))
        }
    }
    return contacts
}
