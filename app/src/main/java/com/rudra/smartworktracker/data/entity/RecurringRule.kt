package com.rudra.smartworktracker.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a recurring transaction rule.
 * Supports income, expenses, savings, and transfers with flexible scheduling.
 */
@Entity(
    tableName = "recurring_rules",
    indices = [
        Index(value = ["isActive"]),
        Index(value = ["nextExecutionDate"]),
        Index(value = ["transactionType"])
    ]
)
data class RecurringRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // UUID for future sync preparation
    val uuid: String? = null,
    
    // Rule name/description
    val name: String,
    val description: String? = null,
    
    // Transaction type (INCOME, EXPENSE, SAVINGS_ADD, TRANSFER)
    val transactionType: TransactionType,
    
    // Amount
    val amount: Double,
    
    // Category (for expenses/income)
    val category: String? = null,
    
    // Source and destination accounts
    val sourceAccount: AccountType,
    val destinationAccount: AccountType? = null,
    
    // Account IDs for multi-account support (references Account entity)
    val accountId: Long? = null,
    val destinationAccountId: Long? = null,
    
    // Frequency: DAILY, WEEKLY, BIWEEKLY, MONTHLY, YEARLY, CUSTOM, WEEKLY_SPECIFIC_DAYS
    val frequency: RecurringFrequency,
    
    // Interval for custom frequencies (e.g., every 2 weeks, every 3 months)
    val interval: Int = 1,
    
    // Selected days of week for WEEKLY_SPECIFIC_DAYS frequency
    val selectedDaysOfWeek: List<DayOfWeek>? = null,
    
    // Custom cron expression for power users (optional)
    val cronExpression: String? = null,
    
    // Start date - when the rule begins
    val startDate: Long,
    
    // End date - optional, when the rule ends
    val endDate: Long? = null,
    
    // Next execution timestamp
    val nextExecutionDate: Long,
    
    // Preferred execution time (morning, noon, evening)
    val preferredTime: PreferredTime = PreferredTime.MORNING,
    
    // Priority level for execution
    val priority: RecurringPriority = RecurringPriority.MEDIUM,
    
    // Minimum balance required to execute (for expenses)
    val minimumBalanceRequired: Double? = null,
    
    // Auto-execute without confirmation
    val autoExecute: Boolean = true,
    
    // Grace period in minutes for retry on failure
    val gracePeriodMinutes: Int = 60,
    
    // Whether the rule is active
    val isActive: Boolean = true,
    
    // Notification preferences
    val notifyBeforeExecution: Boolean = true,
    val notifyOnExecution: Boolean = false,
    val notifyOnFailure: Boolean = true,
    
    // Number of days to notify before execution
    val notifyDaysBefore: Int = 1,
    
    // Audit fields
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val isDeleted: Boolean = false,
    override val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY
) : BaseEntity

/**
 * Recurring frequency options
 */
enum class RecurringFrequency {
    DAILY,           // Every day
    WEEKLY,          // Every week
    BIWEEKLY,        // Every 2 weeks
    MONTHLY,         // Every month
    QUARTERLY,       // Every 3 months
    YEARLY,          // Every year
    CUSTOM,          // Custom interval
    WEEKLY_SPECIFIC_DAYS  // Specific days of week
}

/**
 * Days of week for recurring transactions
 */
enum class DayOfWeek(val displayName: String, val shortName: String) {
    MONDAY("Monday", "Mon"),
    TUESDAY("Tuesday", "Tue"),
    WEDNESDAY("Wednesday", "Wed"),
    THURSDAY("Thursday", "Thu"),
    FRIDAY("Friday", "Fri"),
    SATURDAY("Saturday", "Sat"),
    SUNDAY("Sunday", "Sun");
    
    companion object {
        fun fromCalendarDay(calendarDay: Int): DayOfWeek {
            return when (calendarDay) {
                java.util.Calendar.MONDAY -> MONDAY
                java.util.Calendar.TUESDAY -> TUESDAY
                java.util.Calendar.WEDNESDAY -> WEDNESDAY
                java.util.Calendar.THURSDAY -> THURSDAY
                java.util.Calendar.FRIDAY -> FRIDAY
                java.util.Calendar.SATURDAY -> SATURDAY
                java.util.Calendar.SUNDAY -> SUNDAY
                else -> MONDAY
            }
        }
        
        fun toCalendarDay(dayOfWeek: DayOfWeek): Int {
            return when (dayOfWeek) {
                MONDAY -> java.util.Calendar.MONDAY
                TUESDAY -> java.util.Calendar.TUESDAY
                WEDNESDAY -> java.util.Calendar.WEDNESDAY
                THURSDAY -> java.util.Calendar.THURSDAY
                FRIDAY -> java.util.Calendar.FRIDAY
                SATURDAY -> java.util.Calendar.SATURDAY
                SUNDAY -> java.util.Calendar.SUNDAY
            }
        }
    }
}

/**
 * Preferred execution time
 */
enum class PreferredTime {
    MORNING,   // 6 AM - 12 PM
    AFTERNOON, // 12 PM - 6 PM
    EVENING,   // 6 PM - 10 PM
    NIGHT      // 10 PM - 6 AM
}

/**
 * Priority levels for recurring transactions
 */
enum class RecurringPriority {
    CRITICAL,   // Must execute (rent, mortgage, critical bills)
    HIGH,       // Important but can be skipped with notice
    MEDIUM,     // Normal priority
    LOW,        // Can be skipped if funds low
    OPTIONAL    // Can be skipped or rescheduled
}
