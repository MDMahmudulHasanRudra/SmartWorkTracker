package com.rudra.smartworktracker.ui.screens.timer

import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.model.SessionType
import com.rudra.smartworktracker.ui.components.AnimatedFAB
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkTimerScreen(viewModel: WorkTimerViewModel = viewModel()) {
    val timerState by viewModel.timerState.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (timerState.mode == TimerMode.RUNNING || timerState.mode == TimerMode.ON_BREAK) {
                AnimatedFAB(
                    expanded = timerState.mode == TimerMode.RUNNING,
                    onToggle = { /* Handled by state */ },
                    onAddBreak = { viewModel.startBreak() },
                    onAddLunch = { viewModel.startLunch() }
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Smart Work Timer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    TimerCard(timerState, viewModel)
                }

                item {
                    StatsCard(timerState.todayStats)
                }

                if (timerState.sessionHistory.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Sessions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    items(timerState.sessionHistory) { session ->
                        SessionHistoryItem(session)
                    }
                }
            }
        }
    }
}

@Composable
fun TimerCard(timerState: TimerState, viewModel: WorkTimerViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Progress Rings
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedProgressRing(
                    progress = if (timerState.totalSeconds > 0) {
                        timerState.workSeconds.toFloat() / timerState.totalSeconds
                    } else 0f,
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 20f,
                    ringSize = 280.dp
                )

                AnimatedProgressRing(
                    progress = if (timerState.totalSeconds > 0) {
                        timerState.breakSeconds.toFloat() / timerState.totalSeconds
                    } else 0f,
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 20f,
                    ringSize = 240.dp,
                    startAngle = 90f
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when (timerState.mode) {
                            TimerMode.RUNNING -> "Working"
                            TimerMode.ON_BREAK -> "On Break"
                            TimerMode.ON_LUNCH -> "Lunch Time"
                            TimerMode.PAUSED -> "Paused"
                            TimerMode.STOPPED -> "Ready to Start"
                        },
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Text(
                        text = formatTime(timerState.totalSeconds),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatChip(
                            icon = Icons.Default.Work,
                            text = formatTime(timerState.workSeconds),
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatChip(
                            icon = Icons.Default.Coffee,
                            text = formatTime(timerState.breakSeconds),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Control Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            when (timerState.mode) {
                TimerMode.STOPPED -> {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.startWorkSession() },
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                        Spacer(Modifier.width(8.dp))
                        Text("Start Work")
                    }
                }

                TimerMode.RUNNING -> {
                    OutlinedButton(
                        onClick = { viewModel.pauseWorkSession() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause")
                        Spacer(Modifier.width(8.dp))
                        Text("Pause")
                    }

                    OutlinedButton(
                        onClick = { viewModel.stopWorkSession() },
                        modifier = Modifier.weight(1f),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.error,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            )
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                        Spacer(Modifier.width(8.dp))
                        Text("Stop")
                    }
                }

                TimerMode.PAUSED -> {
                    OutlinedButton(
                        onClick = { viewModel.resumeWorkSession() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                        Spacer(Modifier.width(8.dp))
                        Text("Resume")
                    }

                    OutlinedButton(
                        onClick = { viewModel.stopWorkSession() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                        Spacer(Modifier.width(8.dp))
                        Text("Stop")
                    }
                }

                TimerMode.ON_BREAK, TimerMode.ON_LUNCH -> {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.endBreak() },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Work, contentDescription = "End Break")
                        Spacer(Modifier.width(8.dp))
                        Text("End ${if (timerState.mode == TimerMode.ON_BREAK) "Break" else "Lunch"}")
                    }
                }
            }
        }
    }
}


@Composable
fun AnimatedProgressRing(
    progress: Float,
    color: Color,
    strokeWidth: Float,
    ringSize: androidx.compose.ui.unit.Dp,
    startAngle: Float = -90f
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        )
    )

    Canvas(modifier = Modifier.size(ringSize)) {
        // Background ring
        drawArc(
            color = color.copy(alpha = 0.2f),
            startAngle = startAngle,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
            size = Size(size.width, size.height)
        )

        // Progress ring
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = animatedProgress * 360f,
            useCenter = false,
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
            size = Size(size.width, size.height)
        )

        // Glow effect
        if (animatedProgress > 0) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.5f), Color.Transparent),
                    radius = strokeWidth / 2
                ),
                radius = strokeWidth / 2,
                center = Offset(
                    x = size.width / 2 + (size.width / 2 - strokeWidth / 2) *
                            kotlin.math.cos(Math.toRadians((startAngle + animatedProgress * 360).toDouble())).toFloat(),
                    y = size.height / 2 + (size.height / 2 - strokeWidth / 2) *
                            kotlin.math.sin(Math.toRadians((startAngle + animatedProgress * 360).toDouble())).toFloat()
                )
            )
        }
    }
}

@Composable
fun StatChip(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StatsCard(stats: TodayStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Today's Stats",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    value = formatTime(stats.totalWorkTime),
                    label = "Work Time",
                    icon = Icons.Default.Timer
                )
                StatItem(
                    value = stats.totalBreaks.toString(),
                    label = "Breaks",
                    icon = Icons.Default.Coffee
                )
                StatItem(
                    value = "%.1f%%".format(stats.productivityScore),
                    label = "Productivity",
                    icon = Icons.Default.TrendingUp
                )
                StatItem(
                    value = stats.sessionsCompleted.toString(),
                    label = "Sessions",
                    icon = Icons.Default.Work
                )
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun SessionHistoryItem(session: SessionItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (session.type) {
                            SessionType.WORK -> Icons.Default.Work
                            SessionType.BREAK -> Icons.Default.Coffee
                            SessionType.LUNCH -> Icons.Default.Restaurant
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = session.type.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Started: ${session.startTime}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = session.duration,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                session.productivityScore?.let { score ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    score >= 80 -> Color.Green.copy(alpha = 0.2f)
                                    score >= 60 -> Color.Yellow.copy(alpha = 0.2f)
                                    else -> Color.Red.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$score%",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60

    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, remainingSeconds)
    } else {
        "%02d:%02d".format(minutes, remainingSeconds)
    }
}