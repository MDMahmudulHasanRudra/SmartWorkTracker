package com.rudra.smartworktracker.data.repository

import com.rudra.smartworktracker.data.dao.WorkLogDao
import com.rudra.smartworktracker.model.WorkLog
import com.rudra.smartworktracker.model.WorkType
import com.rudra.smartworktracker.ui.MonthlyStats
import com.rudra.smartworktracker.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkLogRepository(private val workLogDao: WorkLogDao) {

    fun getTodayWorkLog(): Flow<WorkLog?> {
        return workLogDao.getTodayWorkLog()
    }

    /** Live stats for the current month; recomputed whenever a work log changes. */
    fun getMonthlyStats(): Flow<MonthlyStats> {
        val (monthStart, monthEnd) = DateTimeUtils.monthRange()
        return workLogDao.getWorkLogsBetween(monthStart, monthEnd).map { logs ->
            val officeDays = logs.count { it.workType == WorkType.OFFICE }
            val homeOfficeDays = logs.count { it.workType == WorkType.HOME_OFFICE }
            val offDays = logs.count { it.workType == WorkType.OFF_DAY }
            val extraHours = logs
                .filter { it.workType == WorkType.EXTRA_WORK || it.workType == WorkType.OVERTIME }
                .sumOf { DateTimeUtils.hoursBetween(it.startTime, it.endTime) }
            MonthlyStats(
                officeDays = officeDays,
                homeOfficeDays = homeOfficeDays,
                offDays = offDays,
                extraHours = extraHours,
                totalWorkDays = officeDays + homeOfficeDays
            )
        }
    }

    suspend fun getWorkLogForDay(millis: Long = System.currentTimeMillis()): WorkLog? {
        val (start, end) = DateTimeUtils.dayRange(millis)
        return workLogDao.getFirstWorkLogBetween(start, end)
    }

    suspend fun getWorkLogByIdOnce(id: Long): WorkLog? = workLogDao.getWorkLogByIdOnce(id)

    fun getWorkLogsBetween(start: Long, end: Long): Flow<List<WorkLog>> = workLogDao.getWorkLogsBetween(start, end)

    fun getRecentActivities(): Flow<List<WorkLog>> {
        return workLogDao.getRecentWorkLogs()
    }

    fun getOvertimeLogs(): Flow<List<WorkLog>> {
        return workLogDao.getOvertimeLogs()
    }

    fun getOvertimeLogsByMonth(monthYear: String): Flow<List<WorkLog>> {
        return workLogDao.getOvertimeLogsByMonth(monthYear)
    }

    fun getOvertimeLogsByYear(year: String): Flow<List<WorkLog>> {
        return workLogDao.getOvertimeLogsByYear(year)
    }

    suspend fun insertWorkLog(workLog: WorkLog) {
        workLogDao.insertWorkLog(workLog)
    }

    suspend fun updateWorkLog(workLog: WorkLog) {
        workLogDao.updateWorkLog(workLog)
    }

    fun getAllWorkLogs(): Flow<List<WorkLog>> {
        return workLogDao.getAllWorkLogs()
    }

    fun getWorkLogs(page: Int, pageSize: Int): Flow<List<WorkLog>> {
        val offset = (page - 1) * pageSize
        return workLogDao.getPaginatedWorkLogs(offset, pageSize)
    }

    suspend fun deleteWorkLog(workLog: WorkLog) {
        workLogDao.deleteWorkLog(workLog)
    }

    suspend fun deleteWorkLogById(id: Long) {
        workLogDao.deleteWorkLogById(id)
    }

    fun getWorkLogById(id: Long): Flow<WorkLog?> {
        return workLogDao.getWorkLogById(id)
    }

    suspend fun clearAll() {
        workLogDao.clearAll()
    }
}
