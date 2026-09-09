package com.rudra.smartworktracker.ui.screens.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.smartworktracker.ui.FinancialSummary
import com.rudra.smartworktracker.ui.components.AnimatedDoubleCounter
import com.rudra.smartworktracker.ui.components.SectionHeader

@Composable
fun SavingsGoalCard(financialSummary: FinancialSummary) {
    val monthlySavings = financialSummary.monthlyNetSavings
    val savingsRate = financialSummary.savingsPercentage
    val isPositive = monthlySavings >= 0

    // Target: 20% savings rate (common financial goal)
    val targetRate = 20.0
    val progress = remember(savingsRate) {
        ((savingsRate / targetRate).coerceIn(0.0, 1.0)).toFloat()
    }

    var animProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animProgress,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "savingsGoal"
    )
    LaunchedEffect(Unit) { animProgress = 1f }

    val primaryColor = MaterialTheme.colorScheme.primary
    val savingsColor = if (isPositive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeader(title = "Savings Goal")

            Spacer(modifier = Modifier.height(12.dp))

            // Circular progress indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        val strokeWidth = 8f
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                        // Track
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Progress
                        val sweep = 360f * progress * animatedProgress
                        drawArc(
                            color = savingsColor,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Text(
                        text = "${"%.0f".format(savingsRate * animatedProgress)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = savingsColor
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monthly Savings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedDoubleCounter(
                        targetValue = monthlySavings,
                        prefix = "\u09F3",
                        color = savingsColor,
                        fontSize = 22.sp,
                        durationMillis = 800
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Target: ${"%.0f".format(targetRate)}% savings rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Linear progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .shadow(2.dp, RoundedCornerShape(3.dp)),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Track
                    drawRoundRect(
                        trackColor,
                        Offset.Zero,
                        size,
                        CornerRadius(3f, 3f)
                    )
                    // Fill
                    val fillWidth = (size.width * progress * animatedProgress)
                    if (fillWidth > 0f) {
                        drawRoundRect(
                            savingsColor,
                            Offset.Zero,
                            Size(fillWidth, size.height),
                            CornerRadius(3f, 3f)
                        )
                    }
                }
            }
        }
    }
}
