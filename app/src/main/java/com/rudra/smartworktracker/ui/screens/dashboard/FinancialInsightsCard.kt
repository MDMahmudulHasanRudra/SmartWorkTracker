package com.rudra.smartworktracker.ui.screens.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.smartworktracker.ui.FinancialSummary
import com.rudra.smartworktracker.ui.components.AnimatedDoubleCounter
import com.rudra.smartworktracker.ui.components.SectionHeader

private val IncomeGreen = Color(0xFF10B981)
private val AmberColor = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialInsightsCard(financialSummary: FinancialSummary) {
    val totalLoan = financialSummary.totalLoan
    val mealCost = financialSummary.totalMealCost
    val overtimeHours = financialSummary.overtimeHours
    val overtimeEarnings = financialSummary.overtimeEarnings
    val savingsRate = financialSummary.savingsPercentage.toFloat()

    val expenseColor = MaterialTheme.colorScheme.error
    val primaryColor = MaterialTheme.colorScheme.primary

    var animProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animProgress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "insightBar"
    )
    LaunchedEffect(Unit) { animProgress = 1f }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeader(title = "Financial Insights")

            Spacer(modifier = Modifier.height(16.dp))

            // Savings Rate + Monthly Savings
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Circular progress via Canvas
                val progressColor = when {
                    savingsRate >= 20f -> IncomeGreen
                    savingsRate >= 0f -> AmberColor
                    else -> expenseColor
                }

                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        val strokeWidth = 8f
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                        val arcSize = Size(diameter, diameter)

                        // Track
                        drawArc(
                            color = Color.Gray.copy(alpha = 0.15f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                        // Progress
                        val sweep = 360f * (savingsRate / 100f).coerceIn(0f, 1f) * animatedProgress
                        drawArc(
                            color = progressColor,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${"%.0f".format(savingsRate)}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = progressColor
                        )
                        Text(
                            "Rate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monthly Savings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedDoubleCounter(
                        targetValue = financialSummary.monthlyNetSavings,
                        prefix = "\u09F3",
                        color = if (financialSummary.monthlyNetSavings >= 0) IncomeGreen else expenseColor,
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

            // Metric cards row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InsightMetricCard(
                    icon = Icons.Default.Restaurant,
                    label = "Meals",
                    value = mealCost,
                    color = primaryColor,
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    icon = Icons.Default.Schedule,
                    label = "OT Hours",
                    value = overtimeHours,
                    suffix = "h",
                    color = AmberColor,
                    modifier = Modifier.weight(1f)
                )
                InsightMetricCard(
                    icon = Icons.Default.AttachMoney,
                    label = "OT Pay",
                    value = overtimeEarnings,
                    color = IncomeGreen,
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

            Spacer(modifier = Modifier.height(14.dp))

            // Savings rate bar
            val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            val barColor = when {
                savingsRate >= 20f -> IncomeGreen
                savingsRate >= 0f -> AmberColor
                else -> expenseColor
            }
            val barProgress = (savingsRate / 100f).coerceIn(0f, 1f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                drawRoundRect(trackColor, Offset.Zero, size, CornerRadius(3f, 3f))
                val fillW = size.width * barProgress * animatedProgress
                if (fillW > 0f) {
                    drawRoundRect(barColor, Offset.Zero, Size(fillW, size.height), CornerRadius(3f, 3f))
                }
            }
        }
    }
}

@Composable
private fun InsightMetricCard(
    icon: ImageVector,
    label: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier,
    suffix: String = ""
) {
    Surface(
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedDoubleCounter(
                targetValue = value,
                prefix = if (suffix.isEmpty()) "\u09F3" else "",
                suffix = suffix,
                color = color,
                fontSize = 13.sp,
                durationMillis = 600
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
