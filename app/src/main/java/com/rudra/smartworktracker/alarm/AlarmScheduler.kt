package com.rudra.smartworktracker.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.rudra.smartworktracker.model.Schedule
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Schedules [Schedule] alarms with AlarmManager.
 *
 * Repeating days are ISO day-of-week numbers (1 = Monday … 7 = Sunday), matching
 * java.time.DayOfWeek used by the scheduler UI. Every alarm is a one-shot exact alarm;
 * [AlarmReceiver] re-arms repeating schedules after they fire.
 */
class AlarmScheduler(private val context: Context) {

    companion object {
        const val EXTRA_SCHEDULE_ID = "SCHEDULE_ID"
        const val EXTRA_SCHEDULE_TITLE = "SCHEDULE_TITLE"
        const val EXTRA_DAY_OF_WEEK = "DAY_OF_WEEK"
        const val EXTRA_RINGTONE_URI = "RINGTONE_URI"
        const val EXTRA_VOLUME = "VOLUME_LEVEL"
        const val EXTRA_VIBRATION = "VIBRATION_PATTERN"
        const val EXTRA_SNOOZE_MINUTES = "SNOOZE_MINUTES"
        const val EXTRA_SNOOZE_REMAINING = "SNOOZE_REMAINING"

        /** Marks the one-time slot of a schedule (as opposed to a weekday slot 1..7). */
        const val ONE_TIME_SLOT = 0

        /** Offsets keep snooze/test alarms from replacing the schedule's real alarm. */
        const val SNOOZE_ID_OFFSET = 1_000_000L
        const val TEST_ID_OFFSET = 2_000_000L

        private const val ACTION_TRIGGER = "com.rudra.smartworktracker.ALARM_TRIGGER"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(schedule: Schedule) {
        val id = schedule.id ?: return
        if (!schedule.isEnabled) return

        if (schedule.isRepeating && schedule.repeatingDays.isNotEmpty()) {
            schedule.repeatingDays.forEach { isoDay ->
                setExact(nextTriggerForDay(schedule.time, isoDay), createPendingIntent(schedule, id, isoDay))
            }
        } else {
            // One-time schedules and "daily" repeating schedules (no days selected)
            setExact(nextTrigger(schedule.time), createPendingIntent(schedule, id, ONE_TIME_SLOT))
        }
    }

    /** Re-arms the slot that just fired, one week (weekday slot) or one day (daily) later. */
    fun scheduleNextOccurrence(schedule: Schedule, slot: Int) {
        val id = schedule.id ?: return
        if (!schedule.isEnabled || !schedule.isRepeating) return
        val triggerAt = if (slot == ONE_TIME_SLOT) nextTrigger(schedule.time) else nextTriggerForDay(schedule.time, slot)
        setExact(triggerAt, createPendingIntent(schedule, id, slot))
    }

    /** Cancels every slot the schedule could have used, regardless of its current days. */
    fun cancel(schedule: Schedule) {
        val id = schedule.id ?: return
        (listOf(ONE_TIME_SLOT) + (1..7)).forEach { slot ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(id, slot),
                Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_TRIGGER
                    data = slotUri(id, slot)
                },
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
        cancelLegacyAlarms(id)
    }

    /** Alarms armed by older app versions used a bare intent with these request codes. */
    private fun cancelLegacyAlarms(id: Long) {
        (listOf(id) + (1..7).map { id * 10 + it }).forEach { code ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                code.toInt(),
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun rescheduleAll(schedules: List<Schedule>) {
        schedules.forEach { cancel(it) }
        schedules.filter { it.isEnabled }.forEach { schedule(it) }
    }

    private fun setExact(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canUseExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            // Exact-alarm access revoked between the check and the call
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun createPendingIntent(schedule: Schedule, id: Long, slot: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER
            // A distinct data Uri per slot keeps PendingIntents from overwriting each other
            data = slotUri(id, slot)
            putExtra(EXTRA_SCHEDULE_ID, id)
            putExtra(EXTRA_SCHEDULE_TITLE, schedule.title)
            putExtra(EXTRA_DAY_OF_WEEK, slot)
            putExtra(EXTRA_RINGTONE_URI, schedule.ringtoneUri)
            putExtra(EXTRA_VOLUME, schedule.volumeLevel)
            putExtra(EXTRA_VIBRATION, schedule.vibrationPattern)
            putExtra(EXTRA_SNOOZE_MINUTES, schedule.snoozeDuration)
            putExtra(EXTRA_SNOOZE_REMAINING, schedule.maxSnoozeCount)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(id, slot),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun slotUri(id: Long, slot: Int): Uri = Uri.parse("smartworktracker://alarm/$id/$slot")

    private fun requestCode(id: Long, slot: Int): Int = (id * 10 + slot).toInt()

    private fun nextTrigger(time: LocalTime): Long {
        val now = LocalDateTime.now()
        var trigger = now.with(time)
        if (!trigger.isAfter(now)) trigger = trigger.plusDays(1)
        return trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun nextTriggerForDay(time: LocalTime, isoDay: Int): Long {
        val now = LocalDateTime.now()
        var trigger = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(isoDay))).with(time)
        if (!trigger.isAfter(now)) trigger = trigger.plusWeeks(1)
        return trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
