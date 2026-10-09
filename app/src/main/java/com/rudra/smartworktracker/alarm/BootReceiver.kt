package com.rudra.smartworktracker.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rudra.smartworktracker.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != "android.intent.action.QUICKBOOT_POWERON") return

        // AlarmManager forgets every alarm on reboot, so re-arm all enabled schedules
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val schedules = AppDatabase.getDatabase(context).scheduleDao().getAllSchedulesOnce()
                AlarmScheduler(context).rescheduleAll(schedules)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
