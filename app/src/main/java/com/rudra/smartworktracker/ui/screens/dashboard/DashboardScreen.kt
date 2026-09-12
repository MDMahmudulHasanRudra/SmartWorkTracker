package com.rudra.smartworktracker.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.data.AppDatabase

@Composable
fun DashboardScreen(
    onNavigateToAddEntry: () -> Unit,
    onNavigateToIncome: () -> Unit,
    onNavigateToExpense: () -> Unit,
    onNavigateToAccounts: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(AppDatabase.getDatabase(context), context)
    )
    val uiState by viewModel.uiState.collectAsState()

    val hasRecentActivities by remember(uiState.recentActivities) {
        derivedStateOf { uiState.recentActivities.isNotEmpty() }
    }

    Scaffold(
        floatingActionButton = {
            DashboardQuickActions(
                onNavigateToAddEntry = onNavigateToAddEntry,
                onNavigateToIncome = onNavigateToIncome,
                onNavigateToExpense = onNavigateToExpense,
                onNavigateToAccounts = onNavigateToAccounts
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // 1. Premium Hero Section - Account Selector + Greeting + Net Worth
            item { DashboardHeroSection(
                userName = uiState.userName,
                financialSummary = uiState.financialSummary,
                accounts = uiState.accounts,
                todayWorkType = uiState.todayWorkType,
                onWorkTypeClick = viewModel::updateTodayWorkType,
                onNavigateToAccounts = onNavigateToAccounts
            )}

            // 2. Financial Insights Card
            item { FinancialInsightsCard(financialSummary = uiState.financialSummary) }

            // 3. Today Snapshot (Income/Expense/Savings)
            item { TodaySnapshot(financialSummary = uiState.financialSummary) }

            // 3. Monthly Income vs Expense Comparison
            item { MonthlyComparisonBar(financialSummary = uiState.financialSummary) }

            // 4. Savings Goal
            item { SavingsGoalCard(financialSummary = uiState.financialSummary) }

            // 5. Monthly Donut Chart
            item { MonthlyDonutChart(expensesByCategory = uiState.expensesByCategory) }

            // 6. Weekly Spending Trend
            item { WeeklySpendingTrend(expenses = uiState.expenses) }

            // 7. Expense Categories
            item { ExpenseCategories(expensesByCategory = uiState.expensesByCategory) }

            // 8. Work Stats Grid
            item { WorkStatsGrid(stats = uiState.monthlyStats) }

            // 9. Weekly Activity Bars
            item { WeeklyActivityBars(workLogs = uiState.workLogs) }

            // 10. Today's Expense Breakdown
            item { TodayExpenseBreakdown(expenses = uiState.expenses) }

            // 11. Recent Activity Timeline
            if (hasRecentActivities) {
                item { RecentActivityTimeline(activities = uiState.recentActivities) }
            }
        }
    }
}