package com.rudra.smartworktracker.data.repository

import android.content.Context
import com.rudra.smartworktracker.data.dao.ExpenseDao
import com.rudra.smartworktracker.data.dao.IncomeDao
import com.rudra.smartworktracker.model.SpendAdvisor
import com.rudra.smartworktracker.model.SpendingTrend
import com.rudra.smartworktracker.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

class SpendAdvisorRepository(
    private val incomeDao: IncomeDao,
    private val expenseDao: ExpenseDao,
    private val context: Context? = null
) {
    private val prefs = context?.getSharedPreferences("spend_advisor_prefs", Context.MODE_PRIVATE)
    private val defaultMonthlyGoal = 30000.0

    // Observable so a new budget is reflected immediately (it used to be read once)
    private val monthlyGoal = MutableStateFlow(
        prefs?.getFloat("monthly_goal", defaultMonthlyGoal.toFloat())?.toDouble() ?: defaultMonthlyGoal
    )

    /**
     * Balance is all-time income minus expenses; income/expense totals, the daily average and
     * the category split cover the current month, matching the monthly budget they're compared to.
     */
    fun getSpendAdvisorFlow(): Flow<SpendAdvisor> {
        val now = System.currentTimeMillis()
        val (monthStart, monthEnd) = DateTimeUtils.monthRange(now)
        val (todayStart, _) = DateTimeUtils.dayRange(now)
        val windowStart = minOf(monthStart, todayStart - 13 * DAY_MS)

        return combine(
            incomeDao.getTotalIncome(),
            expenseDao.getTotalExpenses(),
            incomeDao.getTotalIncomeBetween(monthStart, monthEnd - 1),
            expenseDao.getExpensesBetween(windowStart, monthEnd - 1),
            monthlyGoal
        ) { allIncome, allExpenses, monthIncome, recentExpenses, goal ->
            val monthExpenses = recentExpenses.filter { it.timestamp >= monthStart }
            val monthTotal = monthExpenses.sumOf { it.amount }
            val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

            // Daily totals for the last 14 days, oldest first
            val dailyTotals = (13 downTo 0).map { daysAgo ->
                val start = todayStart - daysAgo * DAY_MS
                recentExpenses.filter { it.timestamp in start until start + DAY_MS }.sumOf { it.amount }
            }
            val previousWeek = dailyTotals.take(7).sum()
            val lastWeek = dailyTotals.takeLast(7).sum()
            val trend = when {
                previousWeek == 0.0 && lastWeek == 0.0 -> SpendingTrend.STABLE
                previousWeek == 0.0 -> SpendingTrend.INCREASING
                lastWeek > previousWeek * 1.1 -> SpendingTrend.INCREASING
                lastWeek < previousWeek * 0.9 -> SpendingTrend.DECREASING
                else -> SpendingTrend.STABLE
            }

            SpendAdvisor(
                totalIncome = monthIncome ?: 0.0,
                totalExpenses = monthTotal,
                currentBalance = (allIncome ?: 0.0) - (allExpenses ?: 0.0),
                dailyAverageExpense = monthTotal / dayOfMonth.coerceAtLeast(1),
                monthlyGoal = goal,
                spendingTrend = trend,
                trendData = dailyTotals.takeLast(7),
                categoryBreakdown = monthExpenses.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }
            )
        }
    }

    fun updateMonthlyGoal(newGoal: Double) {
        prefs?.edit()?.putFloat("monthly_goal", newGoal.toFloat())?.apply()
        monthlyGoal.value = newGoal
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
