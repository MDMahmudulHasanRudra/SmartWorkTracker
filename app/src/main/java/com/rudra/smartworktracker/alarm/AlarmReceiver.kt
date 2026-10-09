package com.rudra.smartworktracker.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rudra.smartworktracker.R
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.model.Schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val CHANNEL_NAME = "Alarm Notifications"
        const val ACTION_SNOOZE = "com.rudra.smartworktracker.ALARM_SNOOZE"
        const val ACTION_DISMISS = "com.rudra.smartworktracker.ALARM_DISMISS"
        const val ACTION_STOP_ALARM = "com.rudra.smartworktracker.STOP_ALARM"
        const val EXTRA_NOTIFICATION_ID = "NOTIFICATION_ID"
        private const val ACTION_TRIGGER = "com.rudra.smartworktracker.ALARM_TRIGGER"

        fun notificationIdFor(scheduleId: Long): Int = (scheduleId % Int.MAX_VALUE).toInt()

        /** Removes the ongoing alarm notification and stops a visible AlarmActivity. */
        fun dismiss(context: Context, notificationId: Int) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
            context.sendBroadcast(Intent(ACTION_STOP_ALARM).setPackage(context.packageName))
        }

        /** Arms a one-off snooze alarm that will not overwrite the schedule's own alarm. */
        fun snooze(context: Context, intent: Intent, minutes: Int) {
            val scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
            val title = intent.getStringExtra(AlarmScheduler.EXTRA_SCHEDULE_TITLE) ?: "Schedule Alarm"
            val baseId = if (scheduleId >= AlarmScheduler.SNOOZE_ID_OFFSET) scheduleId - AlarmScheduler.SNOOZE_ID_OFFSET else scheduleId
            val snoozeId = if (baseId >= 0) baseId + AlarmScheduler.SNOOZE_ID_OFFSET else System.currentTimeMillis() % AlarmScheduler.SNOOZE_ID_OFFSET + AlarmScheduler.SNOOZE_ID_OFFSET
            val remaining = intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_REMAINING, 3) - 1

            AlarmScheduler(context).schedule(
                Schedule(
                    id = snoozeId,
                    title = title.removePrefix("Snooze: ").let { "Snooze: $it" },
                    time = LocalTime.now().plusMinutes(minutes.toLong()),
                    isEnabled = true,
                    isRepeating = false,
                    ringtoneUri = intent.getStringExtra(AlarmScheduler.EXTRA_RINGTONE_URI),
                    volumeLevel = intent.getIntExtra(AlarmScheduler.EXTRA_VOLUME, 80),
                    vibrationPattern = intent.getStringExtra(AlarmScheduler.EXTRA_VIBRATION) ?: "default",
                    snoozeDuration = minutes,
                    maxSnoozeCount = remaining.coerceAtLeast(0)
                )
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, notificationIdFor(scheduleId))

        when (intent.action) {
            ACTION_SNOOZE -> {
                snooze(context, intent, intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, 5))
                dismiss(context, notificationId)
            }
            ACTION_DISMISS -> dismiss(context, notificationId)
            // null action = alarm armed by an older app version
            ACTION_TRIGGER, null -> if (scheduleId != -1L) handleTrigger(context, intent, scheduleId)
            else -> Unit
        }
    }

    private fun handleTrigger(context: Context, intent: Intent, scheduleId: Long) {
        // Snooze and test alarms are not stored in the database
        if (scheduleId >= AlarmScheduler.SNOOZE_ID_OFFSET) {
            showAlarmNotification(context, intent, scheduleId)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(context).scheduleDao()
                val schedule = dao.getScheduleById(scheduleId)
                // Stale alarm for a deleted/disabled schedule
                if (schedule == null || !schedule.isEnabled) return@launch

                val slot = intent.getIntExtra(AlarmScheduler.EXTRA_DAY_OF_WEEK, AlarmScheduler.ONE_TIME_SLOT)
                val weekdaySchedule = schedule.isRepeating && schedule.repeatingDays.isNotEmpty()
                if (weekdaySchedule && slot != AlarmScheduler.ONE_TIME_SLOT && slot !in schedule.repeatingDays) {
                    return@launch // Day was removed from the schedule after this alarm was armed
                }

                showAlarmNotification(context, intent, scheduleId)

                if (schedule.isRepeating) {
                    AlarmScheduler(context).scheduleNextOccurrence(schedule, slot)
                } else {
                    // One-time alarm has fired; disable it so it is not re-armed for tomorrow
                    dao.updateSchedule(schedule.copy(isEnabled = false, updatedAt = System.currentTimeMillis()))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showAlarmNotification(context: Context, triggerIntent: Intent, scheduleId: Long) {
        val title = triggerIntent.getStringExtra(AlarmScheduler.EXTRA_SCHEDULE_TITLE) ?: "Schedule Alarm"
        val notificationId = notificationIdFor(scheduleId)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm notifications"
                enableVibration(true)
                setShowBadge(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 1. Full Screen Intent (AlarmActivity) – carries the schedule's sound/snooze settings
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtras(triggerIntent)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Dismiss Action
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeMinutes = triggerIntent.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, 5)
        val snoozesLeft = triggerIntent.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_REMAINING, 3)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.playstore)
            .setContentTitle("⏰ $title")
            .setContentText("It's time for your schedule!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

        // 3. Snooze Action (only while snoozes remain)
        if (snoozesLeft > 0) {
            val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtras(triggerIntent)
                action = ACTION_SNOOZE
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 2,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_popup_reminder, "Snooze ${snoozeMinutes}m", snoozePendingIntent)
        }

        notificationManager.notify(notificationId, builder.build())
    }
}
