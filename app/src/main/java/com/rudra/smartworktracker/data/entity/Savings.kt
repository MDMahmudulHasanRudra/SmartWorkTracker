package com.rudra.smartworktracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings")
data class Savings(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val note: String = "",
    val category: String = "General",
    val timestamp: Long,
    val accountId: Long? = null,

    // UUID field for future primary key transition - Rule 1.1
    val uuid: String? = null,

    // Audit fields - Rule 1.2
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val isDeleted: Boolean = false,
    override val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY
) : BaseEntity

enum class SavingsCategory(val displayName: String) {
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    INTEREST("Interest"),
    TRANSFER("Transfer"),
    OTHER("Other")
}
