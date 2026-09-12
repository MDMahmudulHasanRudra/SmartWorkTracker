package com.rudra.smartworktracker.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.smartworktracker.ui.FinancialSummary
import com.rudra.smartworktracker.ui.components.AnimatedDoubleCounter

private val IncomeGreen = Color(0xFF10B981)
private val ExpenseRed = Color(0xFFEF4444)

@Composable
fun TodaySnapshot(financialSummary: FinancialSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TodayMetricCard(
            title = "Today's Income",
            value = financialSummary.dailyIncome,
            icon = Icons.Outlined.AttachMoney,
            color = IncomeGreen,
            modifier = Modifier.weight(1f)
        )
        TodayMetricCard(
            title = "Today's Expense",
            value = financialSummary.dailyExpense,
            icon = Icons.Outlined.MoneyOff,
            color = ExpenseRed,
            modifier = Modifier.weight(1f)
        )
        TodayMetricCard(
            title = "Today's Savings",
            value = financialSummary.dailySavings,
            icon = Icons.Outlined.Savings,
            color = if (financialSummary.dailySavings >= 0) IncomeGreen else ExpenseRed,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TodayMetricCard(
    title: String,
    value: Double,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(36.dp)
            ) {
                Column(
                    modifier = Modifier.padding(0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedDoubleCounter(
                targetValue = value,
                prefix = "\u09F3",
                color = color,
                fontSize = 15.sp,
                durationMillis = 600
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
