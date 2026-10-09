package com.rudra.smartworktracker.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.compose.ui.graphics.Color
import com.rudra.smartworktracker.data.entity.BaseEntity
import com.rudra.smartworktracker.data.entity.SyncStatus
import java.util.UUID

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amount: Double = 0.0,
    val currency: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val merchant: String? = null,
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    
    // UUID for future sync preparation - Rule 1.1
    val uuid: String = id,

    // Audit fields - Rule 1.2
    override val createdAt: Long = System.currentTimeMillis(),
    override val updatedAt: Long = System.currentTimeMillis(),
    override val isDeleted: Boolean = false,
    override val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    // Account this expense was paid from (null = not linked); added in DB v13
    val accountId: Long? = null
) : BaseEntity

enum class ExpenseCategory(val displayName: String) {
    MEAL("Food & Dining"),
    TRANSPORT("Transportation"),
    SHOPPING("Shopping"),
    ENTERTAINMENT("Entertainment"),
    BILLS("Bills & Utilities"),
    HEALTHCARE("Healthcare"),
    EDUCATION("Education"),
    PERSONAL_CARE("Personal Care"),
    GIFTS("Gifts"),
    TRAVEL("Travel"),
    SUBSCRIPTIONS("Subscriptions"),
    OTHER("Other");

    val color: Color
        get() = when (this) {
            MEAL -> Color(0xFFFF9800)
            TRANSPORT -> Color(0xFF2196F3)
            SHOPPING -> Color(0xFF9C27B0)
            ENTERTAINMENT -> Color(0xFFE91E63)
            BILLS -> Color(0xFF4CAF50)
            HEALTHCARE -> Color(0xFF00BCD4)
            EDUCATION -> Color(0xFF3F51B5)
            PERSONAL_CARE -> Color(0xFF009688)
            GIFTS -> Color(0xFFE91E63)
            TRAVEL -> Color(0xFF00BCD4)
            SUBSCRIPTIONS -> Color(0xFF9C27B0)
            OTHER -> Color(0xFF9E9E9E)
        }
}
