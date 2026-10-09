package com.rudra.smartworktracker.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rudra.smartworktracker.ui.theme.SmartWorkTrackerTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var alarmIntent by mutableStateOf<Intent?>(null)

    // AlarmReceiver broadcasts this when the alarm is dismissed/snoozed from the notification
    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            stopAlarm()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the activity show over the lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        alarmIntent = intent
        ContextCompat.registerReceiver(
            this,
            stopReceiver,
            IntentFilter(AlarmReceiver.ACTION_STOP_ALARM),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            val current = alarmIntent ?: intent
            SmartWorkTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AlarmScreen(
                        scheduleTitle = current.getStringExtra(AlarmScheduler.EXTRA_SCHEDULE_TITLE) ?: "Schedule Alarm",
                        snoozeMinutes = current.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, 5),
                        canSnooze = current.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_REMAINING, 3) > 0,
                        onDismiss = {
                            stopAlarm()
                            clearNotification(current)
                            finish()
                        },
                        onSnooze = { minutes ->
                            AlarmReceiver.snooze(this, current, minutes)
                            stopAlarm()
                            clearNotification(current)
                            finish()
                        }
                    )
                }
            }
        }

        startAlarm(intent)
    }

    // singleTask: a second alarm while this one is showing arrives here instead of onCreate
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        alarmIntent?.let { clearNotification(it) }
        setIntent(intent)
        alarmIntent = intent
        stopAlarm()
        startAlarm(intent)
    }

    private fun clearNotification(intent: Intent) {
        val scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        val notificationId = intent.getIntExtra(
            AlarmReceiver.EXTRA_NOTIFICATION_ID,
            AlarmReceiver.notificationIdFor(scheduleId)
        )
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
    }

    private fun startAlarm(intent: Intent) {
        val volume = intent.getIntExtra(AlarmScheduler.EXTRA_VOLUME, 80).coerceIn(0, 100) / 100f
        val customUri = intent.getStringExtra(AlarmScheduler.EXTRA_RINGTONE_URI)?.let { Uri.parse(it) }
        val candidates = listOfNotNull(
            customUri,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        )
        // MediaPlayer.create returns null (rather than throwing) for an unplayable Uri
        for (uri in candidates) {
            val player = try {
                MediaPlayer.create(this, uri)
            } catch (e: Exception) {
                null
            } ?: continue
            player.isLooping = true
            player.setVolume(volume, volume)
            player.start()
            mediaPlayer = player
            break
        }

        if (intent.getStringExtra(AlarmScheduler.EXTRA_VIBRATION) == "none") return
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 1000), 0))
    }

    private fun stopAlarm() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        unregisterReceiver(stopReceiver)
        stopAlarm()
        super.onDestroy()
    }
}

@Composable
fun AlarmScreen(
    scheduleTitle: String,
    snoozeMinutes: Int = 5,
    canSnooze: Boolean = true,
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit
) {
    val currentTime = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 64.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = scheduleTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dismiss Button
                LargeAlarmButton(
                    text = "Dismiss",
                    icon = Icons.Default.AlarmOff,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    onClick = onDismiss
                )

                if (canSnooze) {
                    val longSnooze = (snoozeMinutes * 3).coerceAtLeast(snoozeMinutes + 1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SnoozeButton(
                            text = "Snooze ${snoozeMinutes}m",
                            modifier = Modifier.weight(1f),
                            onClick = { onSnooze(snoozeMinutes) }
                        )

                        SnoozeButton(
                            text = "Snooze ${longSnooze}m",
                            modifier = Modifier.weight(1f),
                            onClick = { onSnooze(longSnooze) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LargeAlarmButton(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun SnoozeButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}
