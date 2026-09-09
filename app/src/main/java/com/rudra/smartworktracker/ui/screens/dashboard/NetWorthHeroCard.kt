package com.rudra.smartworktracker.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.smartworktracker.ui.components.AnimatedDoubleCounter
import com.rudra.smartworktracker.ui.FinancialSummary

@Composable
fun NetWorthHeroCard(financialSummary: FinancialSummary) {
    val netWorth = financialSummary.allTimeIncome - financialSummary.allTimeExpense
    val isPositive = netWorth >= 0
    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Net Worth",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            AnimatedDoubleCounter(
                targetValue = netWorth,
                prefix = "\u09F3",
                color = if (isPositive) incomeColor else expenseColor,
                fontSize = 32.sp,
                durationMillis = 1000
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))

            // Summary stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NetWorthSide(
                    label = "Income",
                    value = financialSummary.allTimeIncome,
                    color = incomeColor,
                    icon = Icons.AutoMirrored.Outlined.TrendingUp
                )
                NetWorthSide(
                    label = "Expense",
                    value = financialSummary.allTimeExpense,
                    color = expenseColor,
                    icon = Icons.AutoMirrored.Outlined.TrendingDown
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Additional stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HeroMiniStat("Meal Cost", "\u09F3${"%.0f".format(financialSummary.totalMealCost)}")
                HeroMiniStat("Loan", "\u09F3${"%.0f".format(financialSummary.totalLoan)}")
                HeroMiniStat("Overtime", "${"%.1f".format(financialSummary.overtimeHours)}h")
            }
        }
    }
}

@Composable
private fun NetWorthSide(label: String, value: Double, color: Color, icon: ImageVector) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        AnimatedDoubleCounter(
            targetValue = value,
            prefix = "\u09F3",
            color = color,
            fontSize = 18.sp,
            durationMillis = 800
        )
    }
}

@Composable
private fun HeroMiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
