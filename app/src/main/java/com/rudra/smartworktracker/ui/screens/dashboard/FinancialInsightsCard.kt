package com.rudra.smartworktracker.ui.screens.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.smartworktracker.ui.FinancialSummary
import com.rudra.smartworktracker.ui.components.AnimatedDoubleCounter
import com.rudra.smartworktracker.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialInsightsCard(financialSummary: FinancialSummary) {
    val totalLoan = financialSummary.totalLoan
    val mealCost = financialSummary.totalMealCost
    val overtimeHours = financialSummary.overtimeHours
    val overtimeEarnings = financialSummary.overtimeEarnings
    val savingsRate = financialSummary.savingsPercentage.toFloat()

    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = MaterialTheme.colorScheme.error
    val primaryColor = MaterialTheme.colorScheme.primary
    val warningColor = Color(0xFFFF9800)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeader(title = "Financial Insights")

            Spacer(modifier = Modifier.height(12.dp))

            // Savings Rate indicator - simple text-based display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val progressColor = if (savingsRate >= 20f) incomeColor else if (savingsRate >= 0f) warningColor else expenseColor

                    // Simple percentage display
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${"%.0f".format(savingsRate)}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = progressColor
                        )
                        Text(
                            "Savings Rate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Monthly Savings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedDoubleCounter(
                        targetValue = financialSummary.monthlyNetSavings,
                        prefix = "\u09F3",
                        color = if (financialSummary.monthlyNetSavings >= 0) incomeColor else expenseColor,
                        fontSize = 22.sp,
                        durationMillis = 800
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Target: 20% savings rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // Financial metrics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InsightMetricCard(
                    icon = Icons.Default.Restaurant,
                    label = "Meal Cost",
                    value = mealCost,
                    color = primaryColor,
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    icon = Icons.Default.Schedule,
                    label = "Overtime",
                    value = overtimeHours,
                    suffix = "h",
                    color = warningColor,
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    icon = Icons.Default.AttachMoney,
                    label = "OT Earnings",
                    value = overtimeEarnings,
                    color = incomeColor,
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    icon = Icons.Default.MoneyOff,
                    label = "Loans",
                    value = totalLoan,
                    color = expenseColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Linear progress bar for savings rate
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .shadow(2.dp, RoundedCornerShape(3.dp)),
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                val progressColor = if (savingsRate >= 20f) incomeColor else if (savingsRate >= 0f) warningColor else expenseColor
                val targetProgress = (savingsRate / 100f).coerceIn(0f, 1f)

                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    // Track
                    drawRoundRect(trackColor, Offset.Zero, size, CornerRadius(3f, 3f))
                    // Fill
                    val fillWidth = size.width * targetProgress
                    if (fillWidth > 0f) {
                        drawRoundRect(progressColor, Offset.Zero, Size(fillWidth, size.height), CornerRadius(3f, 3f))
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier,
    suffix: String = ""
) {
    Surface(
        modifier = modifier
            .height(64.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.material3.Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedDoubleCounter(
                targetValue = value,
                prefix = if (suffix.isEmpty()) "\u09F3" else "",
                suffix = suffix,
                color = color,
                fontSize = 14.sp,
                durationMillis = 600
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}