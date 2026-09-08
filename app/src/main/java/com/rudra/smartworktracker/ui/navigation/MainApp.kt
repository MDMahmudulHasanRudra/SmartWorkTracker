package com.rudra.smartworktracker.ui.navigation

import android.content.Context
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RealEstateAgent
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.ui.screens.achievements.AchievementsScreen
import com.rudra.smartworktracker.ui.screens.add_entry.AddEntryScreen
import com.rudra.smartworktracker.ui.screens.all_funsion.AllFunsionScreen
import com.rudra.smartworktracker.ui.screens.analytics.AnalyticsScreen
import com.rudra.smartworktracker.ui.screens.backup.BackupScreen
import com.rudra.smartworktracker.ui.screens.breaks.MindfulBreakScreen
import com.rudra.smartworktracker.ui.screens.calculation.CalculationScreen
import com.rudra.smartworktracker.ui.screens.calendar.CalendarScreen
import com.rudra.smartworktracker.ui.screens.creditcard.CreditCardScreen
import com.rudra.smartworktracker.ui.screens.dashboard.DashboardScreen
import com.rudra.smartworktracker.ui.screens.emi.EmiScreen
import com.rudra.smartworktracker.ui.screens.expense.ExpenseScreen
import com.rudra.smartworktracker.ui.screens.financials.FinancialStatementScreen
import com.rudra.smartworktracker.ui.screens.focus.FocusScreen
import com.rudra.smartworktracker.ui.screens.habit.HabitScreen
import com.rudra.smartworktracker.ui.screens.health.HealthMetricsScreen
import com.rudra.smartworktracker.ui.screens.income.IncomeScreen
import com.rudra.smartworktracker.ui.screens.journal.DailyJournalScreen
import com.rudra.smartworktracker.ui.screens.loans.LoansScreen
import com.rudra.smartworktracker.ui.screens.onboarding.OnboardingScreen
import com.rudra.smartworktracker.ui.screens.overtime.OvertimeScreen
import com.rudra.smartworktracker.ui.screens.profile.ProfileScreen
import com.rudra.smartworktracker.ui.screens.profile.ProfileSetupScreen
import com.rudra.smartworktracker.ui.screens.report.MonthlyReportScreen
import com.rudra.smartworktracker.ui.screens.reports.ReportsScreen
import com.rudra.smartworktracker.ui.screens.savings.SavingsScreen
import com.rudra.smartworktracker.ui.screens.scheduler.SchedulerScreen
import com.rudra.smartworktracker.ui.screens.settings.SettingsScreen
import com.rudra.smartworktracker.ui.screens.team.TeamScreen
import com.rudra.smartworktracker.ui.screens.timer.WorkTimerScreen
import com.rudra.smartworktracker.ui.screens.transfer.TransferScreen
import com.rudra.smartworktracker.ui.screens.wisdom.WisdomScreen
import com.rudra.smartworktracker.ui.screens.spendadvisor.SpendAdvisorScreen
import com.rudra.smartworktracker.ui.screens.recurring.RecurringScreen
import com.rudra.smartworktracker.ui.screens.realitytracker.RealityTrackerScreen
import com.rudra.smartworktracker.ui.screens.futureimpact.FutureImpactScreen
import com.rudra.smartworktracker.ui.screens.accounts.AccountsScreen
import com.rudra.smartworktracker.ui.screens.accounts.AccountDetailScreen
import com.rudra.smartworktracker.ui.theme.SmartWorkTrackerTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("main_prefs", Context.MODE_PRIVATE) }
    val isOnboardingCompleted = remember { sharedPreferences.getBoolean("onboarding_completed", false) }
    val startDestination = if (isOnboardingCompleted) NavigationItem.Dashboard.route else NavigationItem.Onboarding.route

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val shouldShowBars = currentRoute != NavigationItem.Onboarding.route

    val navigationItems = listOf(
        NavigationItem.Dashboard,
        NavigationItem.AddEntry,
        NavigationItem.Reports,
        NavigationItem.Journal,
        NavigationItem.WorkTimer,
        NavigationItem.Focus,
        NavigationItem.MindfulBreak,
        NavigationItem.Habit,
        NavigationItem.Health,
        NavigationItem.Achievements,
        NavigationItem.Calendar,
        NavigationItem.Analytics,
        NavigationItem.MonthlyReport,
        NavigationItem.Calculation,
        NavigationItem.FinancialStatement,
        NavigationItem.Expense,
        NavigationItem.Income,
        NavigationItem.Savings,
        NavigationItem.Loans,
        NavigationItem.EMI,
        NavigationItem.CreditCard,
        NavigationItem.Transfer,
        NavigationItem.Accounts,
        NavigationItem.Recurring,
        NavigationItem.Backup,
        NavigationItem.Settings,
        NavigationItem.Team,
        NavigationItem.Overtime,
        NavigationItem.Scheduler,
        NavigationItem.UserProfile,
        NavigationItem.RealityTracker,
        NavigationItem.FutureImpact,
        NavigationItem.SpendAdvisor

    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                val sections = remember {
                    mutableStateMapOf(
                        "Core" to true,
                        "Finance" to true,
                        "Productivity" to true,
                        "Wellness" to true,
                        "System" to false
                    )
                }

                val sectionItems = mapOf(
                    "Core" to listOf(
                        NavigationItem.Dashboard,
                        NavigationItem.AddEntry,
                        NavigationItem.Reports,
                        NavigationItem.Journal,
                        NavigationItem.WorkTimer,
                        NavigationItem.Focus
                    ),
                    "Finance" to listOf(
                        NavigationItem.Expense,
                        NavigationItem.Income,
                        NavigationItem.Accounts,
                        NavigationItem.Transfer,
                        NavigationItem.Savings,
                        NavigationItem.Loans,
                        NavigationItem.EMI,
                        NavigationItem.CreditCard,
                        NavigationItem.FinancialStatement,
                        NavigationItem.Calculation,
                        NavigationItem.BillSplit,
                        NavigationItem.SpendAdvisor
                    ),
                    "Productivity" to listOf(
                        NavigationItem.Calendar,
                        NavigationItem.Analytics,
                        NavigationItem.MonthlyReport,
                        NavigationItem.Recurring,
                        NavigationItem.Scheduler,
                        NavigationItem.Overtime,
                        NavigationItem.Team
                    ),
                    "Wellness" to listOf(
                        NavigationItem.Health,
                        NavigationItem.Habit,
                        NavigationItem.Achievements,
                        NavigationItem.MindfulBreak,
                        NavigationItem.Wisdom,
                        NavigationItem.RealityTracker,
                        NavigationItem.FutureImpact
                    ),
                    "System" to listOf(
                        NavigationItem.Settings,
                        NavigationItem.Backup,
                        NavigationItem.UserProfile,
                        NavigationItem.AllFunsion
                    )
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                .padding(horizontal = 24.dp, vertical = 20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    shadowElevation = 2.dp
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile",
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .size(28.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Smart Work Tracker",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Track, plan, grow",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                    }

                    sections.forEach { (sectionTitle, _) ->
                        val expanded = sections[sectionTitle] ?: true

                        item(key = "header_$sectionTitle") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { sections[sectionTitle] = !expanded }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = sectionTitle.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (expanded) "Collapse" else "Expand",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (expanded) {
                            itemsIndexed(
                                items = sectionItems[sectionTitle] ?: emptyList(),
                                key = { _, item -> item.route }
                            ) { _, item ->
                                val isSelected = item.route == currentRoute
                                val bgColor by animateColorAsState(
                                    targetValue = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        Color.Transparent,
                                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    label = "drawerBg"
                                )

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 1.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                            scope.launch { drawerState.close() }
                                        },
                                    color = bgColor
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (isSelected)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                            item.description?.let { desc ->
                                                Text(
                                                    text = desc,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "divider_$sectionTitle") {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (shouldShowBars) {
                    val currentTitle = remember(currentRoute) {
                        navigationItems.find { it.route == currentRoute }?.title ?: "Smart Work Tracker"
                    }
                    TopAppBar(
                        title = {
                            Text(
                                text = currentTitle,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch {
                                    drawerState.apply {
                                        if (isClosed) open() else close()
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            bottomBar = {
                if (shouldShowBars) {
                    AppBottomNavigation(
                        navController = navController,
                        onAddEntry = { navController.navigate(NavigationItem.AddEntry.route) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            floatingActionButton = {
                if (shouldShowBars && currentRoute != NavigationItem.AddEntry.route) {
                    FloatingActionButton(
                        onClick = { navController.navigate(NavigationItem.AddEntry.route) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Entry",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(NavigationItem.Onboarding.route) {
                    OnboardingScreen(
                        onOnboardingFinished = {
                            sharedPreferences.edit().putBoolean("onboarding_completed", true).apply()
                            navController.navigate(NavigationItem.Dashboard.route) {
                                popUpTo(NavigationItem.Onboarding.route) {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }
                composable(
                    route = NavigationItem.Dashboard.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    DashboardScreen(
                        onNavigateToAddEntry = {
                            navController.navigate(NavigationItem.AddEntry.route)
                        },
                        onNavigateToIncome = { navController.navigate(NavigationItem.Income.route) },
                        onNavigateToExpense = { navController.navigate(NavigationItem.Expense.route) },
                        onNavigateToAccounts = { navController.navigate(NavigationItem.Accounts.route) }
                    )
                }

                composable(
                    route = NavigationItem.Calendar.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    CalendarScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEditEntry = { workLogId ->
                            navController.navigate("${NavigationItem.AddEntry.route}?workLogId=$workLogId")
                        }
                    )
                }


                composable(
                    route = NavigationItem.Analytics.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    AnalyticsScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(
                    route = NavigationItem.Settings.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    SettingsScreen(navController = navController)
                }

                composable(
                    route = NavigationItem.MonthlyReport.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    MonthlyReportScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(
                    route = NavigationItem.WorkTimer.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    WorkTimerScreen()
                }

                composable(
                    route = NavigationItem.Expense.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    ExpenseScreen()
                }
                 composable(
                    route = NavigationItem.Income.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    IncomeScreen()
                }


                composable(
                    route = NavigationItem.Health.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    HealthMetricsScreen()
                }

                composable(
                    route = NavigationItem.Focus.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    FocusScreen()
                }

                composable(
                    route = NavigationItem.Habit.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    HabitScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(
                    route = NavigationItem.Achievements.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    AchievementsScreen()
                }

                composable(
                    route = NavigationItem.Journal.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    DailyJournalScreen()
                }

                composable(
                    route = NavigationItem.MindfulBreak.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    MindfulBreakScreen()
                }

                composable(
                    route = NavigationItem.Wisdom.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    WisdomScreen()
                }



                composable(
                    route = NavigationItem.Calculation.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    CalculationScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(
                    route = NavigationItem.Backup.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
popExitTransition = { defaultPopExitTransition() }
                ) {
                    BackupScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(
                    route = NavigationItem.AllFunsion.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    AllFunsionScreen(navController = navController)
                }


                composable(
                    route = NavigationItem.AddEntry.route + "?workLogId={workLogId}",
                    arguments = listOf(navArgument("workLogId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }),
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    AddEntryScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(
                    route = NavigationItem.Reports.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    ReportsScreen(onNavigateBack = { navController.popBackStack() })
                }
                 composable(
                    route = NavigationItem.FinancialStatement.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    FinancialStatementScreen()
                }
                composable(
                    route = NavigationItem.Savings.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    SavingsScreen()
                }
                composable(
                    route = NavigationItem.Loans.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    LoansScreen()
                }
                composable(
                    route = NavigationItem.EMI.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    EmiScreen()
                }
                composable(
                    route = NavigationItem.CreditCard.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    CreditCardScreen()
                }
                 composable(
                    route = NavigationItem.Transfer.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    TransferScreen()
                }
                composable(
                    route = NavigationItem.Accounts.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    AccountsScreen(
                        onNavigateToTransfer = { navController.navigate(NavigationItem.Transfer.route) },
                        onNavigateToAddAccount = { },
                        onNavigateToAccountDetail = { accountId ->
                            navController.navigate("${NavigationItem.AccountDetail.route}/$accountId")
                        }
                    )
                }
                composable(
                    route = NavigationItem.AccountDetail.route + "/{accountId}",
                    arguments = listOf(navArgument("accountId") { type = NavType.LongType }),
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) { backStackEntry ->
                    val accountId = backStackEntry.arguments?.getLong("accountId") ?: 0L
                    AccountDetailScreen(
                        accountId = accountId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                 composable(
                    route = NavigationItem.Team.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    TeamScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable(
                    route = NavigationItem.Overtime.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    OvertimeScreen()
                }
                composable(
                    route = NavigationItem.Scheduler.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    SchedulerScreen()
                }
                composable(
                    route = NavigationItem.Recurring.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    RecurringScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = NavigationItem.BillSplit.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    val viewModel: com.rudra.smartworktracker.ui.screens.billsplit.BillSplitViewModel = viewModel(
                        factory = com.rudra.smartworktracker.ui.screens.billsplit.BillSplitViewModelFactory(context)
                    )
                    val billSplits by viewModel.billSplits.collectAsState()
                    com.rudra.smartworktracker.ui.screens.billsplit.BillSplitScreen(
                        billSplits = billSplits,
                        onAddBillSplit = { viewModel.addBillSplit(it) },
                        onMarkSettled = { viewModel.markSettled(it) },
                        onDelete = { viewModel.deleteBillSplit(it) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = NavigationItem.RealityTracker.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    RealityTrackerScreen()
                }
                composable(
                    route = NavigationItem.FutureImpact.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    FutureImpactScreen()
                }
                composable(
                    route = NavigationItem.SpendAdvisor.route,
                    enterTransition = { defaultEnterTransition() },
                    exitTransition = { defaultExitTransition() },
                    popEnterTransition = { defaultPopEnterTransition() },
                    popExitTransition = { defaultPopExitTransition() }
                ) {
                    SpendAdvisorScreen()
                }
                composable(NavigationItem.UserProfile.route) {
                    ProfileScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSetup = { navController.navigate("profile_setup") }
                    )
                }
                composable("profile_setup") {
                    ProfileSetupScreen(
                        onProfileSaved = { navController.popBackStack() },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultEnterTransition() =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(400)
    )

private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultExitTransition() =
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = tween(400)
    )

private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopEnterTransition() =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(400)
    )

private fun AnimatedContentTransitionScope<NavBackStackEntry>.defaultPopExitTransition() =
    slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = tween(400)
    )

@Composable
fun AppBottomNavigation(
    navController: NavHostController,
    onAddEntry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bottomItems = listOf(
        NavigationItem.Dashboard,
        NavigationItem.Calendar,
        NavigationItem.Analytics,
        NavigationItem.AllFunsion,
        NavigationItem.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = modifier,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomItems.forEachIndexed { index, item ->
                val isSelected = currentRoute == item.route

                val animatedColor by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "navColor"
                )

                val animatedSize by animateDpAsState(
                    targetValue = if (isSelected) 28.dp else 24.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "navSize"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else
                                Color.Transparent
                        )
                        .padding(vertical = 8.dp)
                        .then(
                            if (index == 2) Modifier // spacer for center FAB gap
                            else Modifier
                        )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(animatedSize),
                            tint = animatedColor
                        )
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = if (isSelected) 11.sp else 10.sp
                            ),
                            color = animatedColor,
                            maxLines = 1
                        )
                    }
                }

                // Add spacing around the center FAB area
                if (index == 1) {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
        }
    }
}

sealed class NavigationItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val description: String? = null
) {
    object Onboarding : NavigationItem("onboarding", "Onboarding", Icons.Default.Dashboard)
    object Dashboard : NavigationItem(
        route = "dashboard",
        title = "Dashboard",
        icon = Icons.Default.Dashboard,
        description = "Your daily summary"
    )

    object Calendar : NavigationItem(
        route = "calendar",
        title = "Calendar",
        icon = Icons.Default.CalendarToday,
        description = "View your work logs"
    )

    object Analytics : NavigationItem(
        route = "analytics",
        title = "Analytics",
        icon = Icons.Default.Analytics,
        description = "Visualize your progress"
    )

    object Settings : NavigationItem(
        route = "settings",
        title = "Settings",
        icon = Icons.Default.Settings,
        description = "Customize the app"
    )

    object MonthlyReport : NavigationItem(
        route = "monthly_report",
        title = "Monthly Report",
        icon = Icons.Default.PieChart,
        description = "Detailed monthly summaries"
    )

    object WorkTimer : NavigationItem(
        route = "work_timer",
        title = "Work Timer",
        icon = Icons.Default.Timer,
        description = "Track your work sessions"
    )

    object Expense : NavigationItem(
        route = "expense",
        title = "Expense Log",
        icon = Icons.Default.AttachMoney,
        description = "Log your expenses"
    )
    object Income : NavigationItem(
        route = "income",
        title = "Income",
        icon = Icons.Default.AttachMoney,
        description = "Track your income"
    )

    object Health : NavigationItem(
        route = "health",
        title = "Health Metrics",
        icon = Icons.Default.Favorite,
        description = "Monitor your well-being"
    )

    object Focus : NavigationItem(
        route = "focus",
        title = "Focus Sessions",
        icon = Icons.Default.FilterCenterFocus,
        description = "Improve your concentration"
    )

    object Habit : NavigationItem(
        route = "habit",
        title = "Habit Tracker",
        icon = Icons.Default.CheckCircle,
        description = "Build positive habits"
    )

    object Achievements : NavigationItem(
        route = "achievements",
        title = "Achievements",
        icon = Icons.Default.EmojiEvents,
        description = "Celebrate your milestones"
    )

    object Journal : NavigationItem(
        route = "journal",
        title = "Daily Journal",
        icon = Icons.Default.Book,
        description = "Reflect on your day"
    )

    object MindfulBreak : NavigationItem(
        route = "mindful_break",
        title = "Mindful Break",
        icon = Icons.Default.SelfImprovement,
        description = "Relax and recharge"
    )

    object Wisdom : NavigationItem(
        route = "wisdom",
        title = "Wisdom Library",
        icon = Icons.AutoMirrored.Filled.LibraryBooks,
        description = "Get inspired"
    )

    object Calculation : NavigationItem(
        route = "calculation",
        title = "Calculation",
        icon = Icons.Default.Calculate,
        description = "Meal and overtime rates"
    )

    object Backup : NavigationItem(
        route = "backup",
        title = "Backup & Restore",
        icon = Icons.Default.Backup,
        description = "Secure your data"
    )

    object AllFunsion : NavigationItem(
        route = "all_funsion",
        title = "All Features",
        icon = Icons.Default.AddRoad,
        description = "Explore all app features"
    )

    object UserProfile : NavigationItem(
        route = "user_profile",
        title = "User Profile",
        icon = Icons.Default.Person,
        description = "Manage your profile"
    )

    object AddEntry : NavigationItem(
        route = "add_entry",
        title = "Add Entry",
        icon = Icons.Default.Add,
        description = "Log your work"
    )

    object Reports : NavigationItem(
        route = "reports",
        title = "Reports",
        icon = Icons.Default.Assessment,
        description = "Generate work reports"
    )
    object FinancialStatement : NavigationItem(
        route = "financial_statement",
        title = "Financial Statement",
        icon = Icons.Default.Assessment,
        description = "View all financial transactions"
    )
    object Savings : NavigationItem(
        route = "savings",
        title = "Savings",
        icon = Icons.Default.Savings,
        description = "Manage your savings"
    )
    object Loans : NavigationItem(
        route = "loans",
        title = "Loans",
        icon = Icons.Default.RealEstateAgent,
        description = "Manage your loans"
    )
    object EMI : NavigationItem(
        route = "emi",
        title = "EMI",
        icon = Icons.Default.Payment,
        description = "Manage your EMIs"
    )
    object CreditCard : NavigationItem(
        route = "credit_card",
        title = "Credit Card",
        icon = Icons.Default.CreditCard,
        description = "Manage your credit cards"
    )
    object Transfer : NavigationItem(
        route = "transfer",
        title = "Transfer",
        icon = Icons.Default.SwapHoriz,
        description = "Move money between accounts"
    )
    object Accounts : NavigationItem(
        route = "accounts",
        title = "Accounts",
        icon = Icons.Default.AccountBalance,
        description = "Manage all your accounts"
    )
    object AccountDetail : NavigationItem(
        route = "account_detail",
        title = "Account Detail",
        icon = Icons.Default.AccountBalance,
        description = "View account details"
    )
    object Team : NavigationItem(
        route = "team",
        title = "Team",
        icon = Icons.Default.Group,
        description = "Manage your teams"
    )
    object Overtime : NavigationItem(
        route = "Over Time",
        title = "Overtime",
        icon = Icons.Default.Group,
        description = "Over time calculation"
    )
    object Scheduler : NavigationItem(
        route = "scheduler",
        title = "Scheduler",
        icon = Icons.Default.Schedule,
        description = "Schedule your tasks"
    )

    object Recurring : NavigationItem(
        route = "recurring",
        title = "Recurring",
        icon = Icons.Default.Repeat,
        description = "Automate your transactions"
    )

    object RealityTracker : NavigationItem(
        route = "reality_tracker",
        title = "Reality Tracker",
        icon = Icons.Default.TrackChanges,
        description = "Track goals, promises, and plans vs reality"
    )

    object FutureImpact : NavigationItem(
        route = "future_impact",
        title = "Future Self",
        icon = Icons.Default.Psychology,
        description = "Are your actions matching who you want to become?"
    )

    object SpendAdvisor : NavigationItem(
        route = "spend_advisor",
        title = "Spend Advisor",
        icon = Icons.Default.Recommend,
        description = "Plan and analyze your future expenses"
    )

    object BillSplit : NavigationItem(
        route = "bill_split",
        title = "Bill Split",
        icon = Icons.Default.Receipt,
        description = "Split bills with others"
    )


}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
    SmartWorkTrackerTheme {
        MainApp()
    }
}
