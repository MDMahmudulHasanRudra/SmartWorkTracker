
package com.rudra.smartworktracker.utils

import android.util.Log
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    fun parseTime(time: String): Long {
        // Handle null/empty input
        val cleanTime = time.trim()
        if (cleanTime.isEmpty()) {
            return getTimeInMillis(0, 0)
        }

        // Parse with safety
        val parts = cleanTime.split(":")
        val hour = parts.getOrElse(0) { "0" }.toIntOrNull()?.coerceIn(0..23) ?: 0
        val minute = parts.getOrElse(1) { "0" }.toIntOrNull()?.coerceIn(0..59) ?: 0

        return getTimeInMillis(hour, minute)
    }

    private fun getTimeInMillis(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /** Minutes since midnight for "HH:mm" (or "H:mm"), or null when the text isn't a valid time. */
    fun minutesOfDay(time: String?): Int? {
        val parts = time?.trim()?.split(":") ?: return null
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val minute = parts[1].take(2).toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        return hour * 60 + minute
    }

    fun isValidTime(time: String?): Boolean = minutesOfDay(time) != null

    /**
     * Hours worked between two "HH:mm" times. An end earlier than the start is treated as an
     * overnight shift (e.g. 22:00 -> 06:00 = 8h) instead of producing a negative duration.
     */
    fun hoursBetween(start: String?, end: String?): Double {
        val startMinutes = minutesOfDay(start) ?: return 0.0
        val endMinutes = minutesOfDay(end) ?: return 0.0
        var diff = endMinutes - startMinutes
        if (diff < 0) diff += 24 * 60
        return diff / 60.0
    }

    /** "8h 30m" style duration for two "HH:mm" times, or "-" when either is missing. */
    fun formatDuration(start: String?, end: String?): String {
        if (!isValidTime(start) || !isValidTime(end)) return "-"
        val totalMinutes = (hoursBetween(start, end) * 60).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
    }

    /** [start, end) epoch millis of the local calendar day containing [millis]. */
    fun dayRange(millis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        return start to calendar.timeInMillis
    }

    /** [start, end) epoch millis of the local calendar month containing [millis]. */
    fun monthRange(millis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        return start to calendar.timeInMillis
    }

    fun parseDate(dateString: String, format: String = "yyyy-MM-dd"): Date? {
        return try {
            SimpleDateFormat(format, Locale.getDefault()).parse(dateString)
        } catch (e: ParseException) {
            Log.e("DateTimeUtils", "Error parsing date: '$dateString' with format: '$format'", e)
            null
        }
    }
}
