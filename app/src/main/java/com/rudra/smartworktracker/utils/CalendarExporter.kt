package com.rudra.smartworktracker.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.rudra.smartworktracker.data.entity.RecurringRule
import com.rudra.smartworktracker.data.entity.RecurringFrequency
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CalendarExporter {
    
    fun exportToIcs(context: Context, rules: List<RecurringRule>): Uri? {
        return try {
            val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
            val now = dateFormat.format(Calendar.getInstance().time)
            
            val icsContent = buildString {
                appendLine("BEGIN:VCALENDAR")
                appendLine("VERSION:2.0")
                appendLine("PRODID:-//SmartWorkTracker//Recurring//EN")
                appendLine("CALSCALE:GREGORIAN")
                appendLine("METHOD:PUBLISH")
                
                rules.filter { it.isActive }.forEach { rule ->
                    // Start the series at the next occurrence so past dates aren't added to calendars
                    val nextExec = dateFormat.format(rule.nextExecutionDate)
                    val description = "${rule.description ?: ""} | Amount: ${CurrencyManager.format(rule.amount)} | Type: ${rule.transactionType}"

                    appendLine("BEGIN:VEVENT")
                    appendLine("DTSTART:$nextExec")
                    appendLine("DTEND:$nextExec")
                    appendLine("DTSTAMP:$now")
                    appendLine("UID:${rule.id}@smartworktracker")
                    appendLine("SUMMARY:${escapeText(rule.name)}")
                    appendLine("DESCRIPTION:${escapeText(description)}")
                    
                    val rrule = getRRule(rule)
                    if (rrule != null) {
                        appendLine("RRULE:$rrule")
                    }
                    
                    appendLine("END:VEVENT")
                }
                
                appendLine("END:VCALENDAR")
            }
            
            val file = File(context.cacheDir, "recurring_transactions.ics")
            file.writeText(icsContent)
            
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /** RFC 5545 TEXT escaping: backslash, semicolon, comma and newlines. */
    private fun escapeText(text: String): String = text
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n")

    private fun getRRule(rule: RecurringRule): String? {
        val interval = rule.interval.coerceAtLeast(1)
        return when (rule.frequency) {
            RecurringFrequency.DAILY -> "FREQ=DAILY;INTERVAL=$interval"
            RecurringFrequency.WEEKLY -> "FREQ=WEEKLY;INTERVAL=$interval"
            RecurringFrequency.BIWEEKLY -> "FREQ=WEEKLY;INTERVAL=${2 * interval}"
            RecurringFrequency.MONTHLY -> "FREQ=MONTHLY;INTERVAL=$interval"
            RecurringFrequency.QUARTERLY -> "FREQ=MONTHLY;INTERVAL=${3 * interval}"
            RecurringFrequency.YEARLY -> "FREQ=YEARLY;INTERVAL=$interval"
            RecurringFrequency.CUSTOM -> "FREQ=DAILY;INTERVAL=${rule.interval}"
            RecurringFrequency.WEEKLY_SPECIFIC_DAYS -> {
                val days = rule.selectedDaysOfWeek?.joinToString(",") { it.name.take(2) } ?: ""
                if (days.isNotEmpty()) "FREQ=WEEKLY;BYDAY=$days" else null
            }
        }
    }
    
    fun shareCalendar(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Recurring Transactions")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Calendar"))
    }
}
