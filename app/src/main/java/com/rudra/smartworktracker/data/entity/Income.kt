package com.rudra.smartworktracker.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "incomes",
    indices = [Index(value = ["timestamp"])]
)
data class Income(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // UUID field for future primary key transition - Rule 1.1
    val uuid: String? = null,

    val amount: Double,
    val description: String,
    val category: String,
    val timestamp: Long,
    val source: String,

    // Audit fields - Rule 1.2
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val isDeleted: Boolean = false,
    override val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    // Account whose balance this income was added to (null = not linked); added in DB v13
    val accountId: Long? = null
) : BaseEntity
