package com.rudra.smartworktracker.ui.screens.health

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun ProgressTab(
    weightProgress: List<Pair<LocalDate, Double>>,
    productivityTrend: List<Pair<LocalDate, Double>>,
    sleepPattern: List<Pair<LocalDate, Double>>,
    workHoursTrend: List<Pair<LocalDate, Double>>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { SectionTitle("Trends & Analysis") }
        item { TrendChartCard("Weight Journey (kg)", weightProgress, Color(0xFFE91E63), Icons.Default.MonitorWeight) }
        item { TrendChartCard("Work Hours (h)", workHoursTrend, Color(0xFF2196F3), Icons.Default.WorkHistory) }
        item { TrendChartCard("Focus Efficiency (min)", productivityTrend, Color(0xFF4CAF50), Icons.Default.Psychology) }
        item { TrendChartCard("Sleep Consistency (h)", sleepPattern, Color(0xFF673AB7), Icons.Default.Bedtime) }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun TrendChartCard(title: String, data: List<Pair<LocalDate, Double>>, color: Color, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (data.size >= 2) {
                AnimatedLineChart(dataPoints = data, color = color, modifier = Modifier.fillMaxWidth().height(180.dp))
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                        Text("Not enough data to plot trend", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedLineChart(
    dataPoints: List<Pair<LocalDate, Double>>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(dataPoints) {
        animatable.animateTo(1f, animationSpec = tween(durationMillis = 1500, easing = EaseInOutCubic))
    }

    Canvas(modifier = modifier) {
        if (dataPoints.isEmpty()) return@Canvas
        val path = Path()
        val xMin = dataPoints.first().first.toEpochDay().toFloat()
        val xMax = dataPoints.last().first.toEpochDay().toFloat()
        val yMin = dataPoints.minOf { it.second }.toFloat()
        val yMax = dataPoints.maxOf { it.second }.toFloat()
        val xRange = if (xMax == xMin) 1f else xMax - xMin
        val yRange = if (yMax == yMin) 1f else yMax - yMin

        dataPoints.forEachIndexed { index, pair ->
            val x = (pair.first.toEpochDay().toFloat() - xMin) / xRange * size.width
            val y = (1 - (pair.second.toFloat() - yMin) / yRange) * size.height
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            alpha = animatable.value
        )

        val fillPath = path.apply {
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.3f * animatable.value), Color.Transparent)
            )
        )
    }
}
