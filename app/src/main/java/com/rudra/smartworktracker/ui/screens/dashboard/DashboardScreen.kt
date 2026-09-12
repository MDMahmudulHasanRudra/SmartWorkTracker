package com.rudra.smartworktracker.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. Premium Hero Section - Gradient + Account Selector + Net Worth
            item {
                DashboardHeroSection(
                    userName = uiState.userName,
                    financialSummary = uiState.financialSummary,
                    accounts = uiState.accounts,
                    todayWorkType = uiState.todayWorkType,
                    onWorkTypeClick = viewModel::updateTodayWorkType,
                    onNavigateToAccounts = onNavigateToAccounts
                )
            }

            // 2. Today Snapshot - Income/Expense/Savings
            item {
                TodaySnapshot(financialSummary = uiState.financialSummary)
            }

            // 3. Financial Insights - Savings rate + metrics
            item {
                FinancialInsightsCard(financialSummary = uiState.financialSummary)
            }

            // 4. Monthly Income vs Expense Comparison
            item {
                MonthlyComparisonBar(financialSummary = uiState.financialSummary)
            }

            // 5. Savings Goal
            item {
                SavingsGoalCard(financialSummary = uiState.financialSummary)
            }

            // 6. Monthly Donut Chart
            item {
                MonthlyDonutChart(expensesByCategory = uiState.expensesByCategory)
            }

            // 7. Weekly Spending Trend
            item {
                WeeklySpendingTrend(expenses = uiState.expenses)
            }

            // 8. Expense Categories
            item {
                ExpenseCategories(expensesByCategory = uiState.expensesByCategory)
            }

            // 9. Work Stats Grid
            item {
                WorkStatsGrid(stats = uiState.monthlyStats)
            }

            // 10. Weekly Activity Bars
            item {
                WeeklyActivityBars(workLogs = uiState.workLogs)
            }

            // 11. Today's Expense Breakdown
            item {
                TodayExpenseBreakdown(expenses = uiState.expenses)
            }

            // 12. Recent Activity Timeline
            if (hasRecentActivities) {
                item {
                    RecentActivityTimeline(activities = uiState.recentActivities)
                }
            }

            // Bottom spacer for FAB clearance
            item {
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.padding(bottom = 80.dp)
                )
            }
        }
    }
}
