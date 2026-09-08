package com.rudra.smartworktracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a single instance of a recurring transaction.
 * Generated automatically by the RecurringEngine.
 */
@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = RecurringRule::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ruleId"]),
        Index(value = ["status"]),
        Index(value = ["scheduledDate"])
    ]
)
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Reference to the parent recurring rule
    val ruleId: Long,
    
    // UUID for future sync
    val uuid: String? = null,
    
    // Transaction details (copied from rule)
    val name: String,
    val description: String? = null,
    val transactionType: TransactionType,
    val amount: Double,
    val category: String? = null,
    val sourceAccount: AccountType,
    val destinationAccount: AccountType? = null,
    
    // Account IDs for multi-account support
    val accountId: Long? = null,
    val destinationAccountId: Long? = null,
    
    // Scheduled execution date
    val scheduledDate: Long,
    
    // Actual execution date (null if not yet executed)
    val executedDate: Long? = null,
    
    // Status of this transaction instance
    val status: RecurringTransactionStatus = RecurringTransactionStatus.PENDING,
    
    // Failure reason if execution failed
    val failureReason: String? = null,
    
    // Number of retry attempts
    val retryCount: Int = 0,
    
    // Maximum retry attempts allowed
    val maxRetries: Int = 3,
    
    // Whether user has manually confirmed this transaction
    val isConfirmed: Boolean = false,
    
    // Whether user skipped this transaction
    val isSkipped: Boolean = false,
    
    // Skip reason if skipped by user
    val skipReason: String? = null,
    
    // Note added by user
    val userNote: String? = null,
    
    // Related transaction IDs (if this generated actual income/expense)
    val relatedIncomeId: Long? = null,
    val relatedExpenseId: Long? = null,
    val relatedFinancialTransactionId: Int? = null,
    
    // Audit fields
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val isDeleted: Boolean = false,
    override val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY
) : BaseEntity

/**
 * Status of a recurring transaction instance
 */
enum class RecurringTransactionStatus {
    PENDING,        // Scheduled but not yet executed
    CONFIRMED,      // User confirmed, ready for execution
    EXECUTING,      // Currently being executed
    EXECUTED,       // Successfully executed
    FAILED,         // Execution failed
    SKIPPED,        // Skipped by user or system
    CANCELLED       // Cancelled by user
}
