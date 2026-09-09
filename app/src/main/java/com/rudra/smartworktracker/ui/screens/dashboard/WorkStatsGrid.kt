package com.rudra.smartworktracker.ui.screens.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.ui.unit.dp
import com.rudra.smartworktracker.ui.MonthlyStats
import com.rudra.smartworktracker.ui.components.InfoCard
import com.rudra.smartworktracker.ui.components.SectionHeader

@Composable
fun WorkStatsGrid(stats: MonthlyStats) {
    val totalDays = stats.totalDays.coerceAtLeast(1)
    val officePct = remember(stats) { (stats.officeDays.toFloat() / totalDays) }
    val homePct = remember(stats) { (stats.homeOfficeDays.toFloat() / totalDays) }

    var animProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animProgress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "workStats"
    )
    LaunchedEffect(Unit) { animProgress = 1f }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeader(title = "Work Stats")

            Spacer(modifier = Modifier.height(12.dp))

            // Work type distribution bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                val cornerRadius = 4f
                // Office
                val officeW = (size.width * officePct * animatedProgress)
                drawRoundRect(
                    primaryColor,
                    Offset.Zero,
                    Size(officeW, size.height),
                    CornerRadius(cornerRadius, cornerRadius)
                )
                // Home Office
                val homeW = (size.width * homePct * animatedProgress)
                drawRoundRect(
                    secondaryColor,
                    Offset(officeW, 0f),
                    Size(homeW, size.height),
                    CornerRadius(cornerRadius, cornerRadius)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Distribution labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DistLabel(primaryColor, "Office", stats.officeDays)
                DistLabel(secondaryColor, "Home", stats.homeOfficeDays)
                DistLabel(tertiaryColor, "Off", stats.offDays)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    title = "Office",
                    value = "${stats.officeDays}",
                    icon = Icons.Default.Work,
                    color = primaryColor,
                    modifier = Modifier.weight(1f)
                )
                InfoCard(
                    title = "Home",
                    value = "${stats.homeOfficeDays}",
                    icon = Icons.Default.Home,
                    color = secondaryColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    title = "Off Days",
                    value = "${stats.offDays}",
                    icon = Icons.Default.BeachAccess,
                    color = tertiaryColor,
                    modifier = Modifier.weight(1f)
                )
                InfoCard(
                    title = "Extra Hrs",
                    value = "${"%.1f".format(stats.extraHours)}",
                    icon = Icons.Default.Bolt,
                    color = errorColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DistLabel(color: Color, label: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
