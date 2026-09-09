package com.rudra.smartworktracker.ui.screens.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.smartworktracker.model.WorkLog
import com.rudra.smartworktracker.ui.components.SectionHeader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle as TextStyleEnum
import java.util.Locale

@Composable
fun WeeklyActivityBars(workLogs: List<WorkLog>) {
    val today = remember { LocalDate.now() }
    val last7Days = remember(today) { (0L..6L).map { today.minusDays(it) }.reversed() }

    val dailyHours = remember(workLogs, last7Days) {
        last7Days.map { date ->
            workLogs.filter { log ->
                val logDate = Instant.ofEpochMilli(log.date.time)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                logDate == date
            }.sumOf { log ->
                val start = log.startTime?.let { parseMinutes(it) } ?: 0
                val end = log.endTime?.let { parseMinutes(it) } ?: 0
                ((end - start).coerceAtLeast(0)) / 60.0
            }
        }
    }

    val maxHours = remember(dailyHours) { (dailyHours.maxOrNull() ?: 1.0).coerceAtLeast(1.0) }
    val totalHours = remember(dailyHours) { dailyHours.sum() }
    val avgHours = remember(dailyHours) { if (dailyHours.any { it > 0 }) totalHours / dailyHours.count { it > 0 } else 0.0 }
    val activeDays = remember(dailyHours) { dailyHours.count { it > 0 } }

    var animProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animProgress,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "bars"
    )
    LaunchedEffect(Unit) { animProgress = 1f }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val todayIndex = last7Days.indexOf(today)
    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeader(title = "Weekly Activity")

            // Summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BarSummaryItem("Total", "${"%.1f".format(totalHours)}h", primaryColor)
                BarSummaryItem("Avg", "${"%.1f".format(avgHours)}h", MaterialTheme.colorScheme.secondary)
                BarSummaryItem("Active", "${activeDays}d", MaterialTheme.colorScheme.tertiary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val barCount = last7Days.size
                val gap = 14f
                val barWidth = (size.width - gap * (barCount + 1)) / barCount
                val chartHeight = size.height - 40f

                // Dashed baseline
                drawLine(
                    color = surfaceVariant.copy(alpha = 0.5f),
                    start = Offset(0f, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )

                last7Days.forEachIndexed { i, date ->
                    val x = gap + i * (barWidth + gap)
                    val normalH = ((dailyHours[i] / maxHours) * chartHeight).toFloat()
                    val barH = normalH * animatedProgress
                    val y = chartHeight - barH
                    val isToday = i == todayIndex

                    // Background bar
                    drawRoundRect(
                        surfaceVariant.copy(alpha = 0.25f),
                        Offset(x, 0f),
                        Size(barWidth, chartHeight),
                        CornerRadius(8f, 8f)
                    )

                    // Filled bar with gradient
                    if (barH > 0f) {
                        val barBrush = if (isToday) {
                            Brush.verticalGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.7f)),
                                startY = y,
                                endY = chartHeight
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.85f),
                                    primaryColor.copy(alpha = 0.5f)
                                ),
                                startY = y,
                                endY = chartHeight
                            )
                        }
                        drawRoundRect(
                            barBrush,
                            Offset(x, y),
                            Size(barWidth, barH),
                            CornerRadius(8f, 8f)
                        )

                        // Top highlight line for today
                        if (isToday) {
                            drawRoundRect(
                                primaryColor,
                                Offset(x, y),
                                Size(barWidth, 3f),
                                CornerRadius(8f, 8f)
                            )
                        }
                    }

                    // Day label
                    val label = date.dayOfWeek.getDisplayName(TextStyleEnum.SHORT, Locale.getDefault())
                    val labelColor = if (isToday) primaryColor else onSurfaceVariant.copy(alpha = 0.6f)
                    val labelStyle = TextStyle(
                        fontSize = 10.sp,
                        color = labelColor,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                    val measured = textMeasurer.measure(label, labelStyle)
                    drawText(
                        measured,
                        topLeft = Offset(x + barWidth / 2 - measured.size.width / 2, chartHeight + 8f)
                    )

                    // Today dot indicator
                    if (isToday) {
                        drawCircle(
                            primaryColor,
                            3f,
                            Offset(x + barWidth / 2, chartHeight + 22f)
                        )
                    }

                    // Hour label on top of bar
                    if (dailyHours[i] > 0) {
                        val hourText = "${"%.1f".format(dailyHours[i])}h"
                        val hourStyle = TextStyle(
                            fontSize = 9.sp,
                            color = if (isToday) primaryColor else onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                        )
                        val hourMeasured = textMeasurer.measure(hourText, hourStyle)
                        drawText(
                            hourMeasured,
                            topLeft = Offset(x + barWidth / 2 - hourMeasured.size.width / 2, y - 16f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarSummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun parseMinutes(time: String): Int {
    return try {
        val parts = time.split(":")
        parts[0].toInt() * 60 + parts[1].toInt()
    } catch (_: Exception) {
        0
    }
}
