package com.rudra.smartworktracker.ui.screens.breaks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.repository.HealthRepository
import kotlinx.coroutines.launch

@Composable
fun MindfulBreakScreen(
    onNavigateBack: () -> Unit = {}
) {
    val breathingState = remember { Animatable(0.3f) }
    val pulseState = remember { Animatable(0f) }
    var instruction by remember { mutableStateOf("Breathe In") }
    var cycleCount by remember { mutableIntStateOf(1) }
    var isRunning by remember { mutableStateOf(true) }
    val startedAt = remember { System.currentTimeMillis() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Records the session in Health (a break plus meditation minutes), then leaves
    fun finishSession() {
        val minutes = ((System.currentTimeMillis() - startedAt) / 60_000.0).coerceAtLeast(1.0)
        val appContext = context.applicationContext
        scope.launch {
            val repository = HealthRepository(AppDatabase.getDatabase(appContext).healthMetricDao())
            repository.logBreak("Mindful break")
            repository.logMeditation(kotlin.math.round(minutes), "Guided breathing ($cycleCount cycles)")
            Toast.makeText(appContext, "Mindful break logged", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF667eea),
            Color(0xFF764ba2),
            Color(0xFFf093fb)
        )
    )

    // Breathing cycle logic
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isActive) {
                instruction = "Breathe In"
                breathingState.animateTo(1f, animationSpec = tween(4000, easing = LinearEasing))
                instruction = "Hold"
                delay(4000)
                instruction = "Breathe Out"
                breathingState.animateTo(0.3f, animationSpec = tween(6000, easing = LinearEasing))
                instruction = "Rest"
                delay(1000)
                cycleCount++
            }
        }
    }

    // Pulse animation
    LaunchedEffect(isRunning) {
        if (isRunning) {
            pulseState.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        // Top-left back button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                PulsingCircle(progress = pulseState.value)
                BreathingCircle(progress = breathingState.value)
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = "Mindfulness",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = instruction,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Cycle $cycleCount",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = getBreathingGuide(instruction),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.7f),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Pause/Resume button
                FilledTonalButton(onClick = { isRunning = !isRunning }) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Resume",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(if (isRunning) "Pause" else "Resume")
                }
                Button(
                    onClick = { finishSession() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF764ba2))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Finish")
                }
            }
        }
    }
}

@Composable
fun BreathingCircle(progress: Float) {
    val size = 280.dp
    val primaryColor = Color(0xFFffecd2)
    val secondaryColor = Color(0xFFfcb69f)

    Canvas(modifier = Modifier.size(size)) {
        val radius = (this.size.minDimension / 2f) * progress

        val gradient = Brush.radialGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.8f),
                secondaryColor.copy(alpha = 0.4f),
                Color.Transparent
            ),
            center = center,
            radius = radius
        )

        drawCircle(brush = gradient, radius = radius * 1.1f)
        drawCircle(color = primaryColor, style = Stroke(width = 6.dp.toPx()), radius = radius)
        drawCircle(color = primaryColor.copy(alpha = 0.3f), radius = radius * 0.7f)
    }
}

@Composable
fun PulsingCircle(progress: Float) {
    val size = 350.dp
    val pulseColor = Color(0xFFa8edea).copy(alpha = progress * 0.3f)

    Canvas(modifier = Modifier.size(size)) {
        val radius = (this.size.minDimension / 2f) * (0.8f + progress * 0.2f)
        drawCircle(color = pulseColor, style = Stroke(width = 3.dp.toPx()), radius = radius)
    }
}

private fun getBreathingGuide(instruction: String): String {
    return when (instruction) {
        "Breathe In" -> "Fill your lungs completely\nFeel your chest expand"
        "Hold" -> "Hold the breath gently\nMaintain relaxation"
        "Breathe Out" -> "Release slowly and completely\nLet go of tension"
        "Rest" -> "Notice the stillness\nPrepare for next cycle"
        else -> "Follow the breathing pattern"
    }
}
