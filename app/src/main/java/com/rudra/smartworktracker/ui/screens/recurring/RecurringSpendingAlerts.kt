package com.rudra.smartworktracker.ui.screens.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rudra.smartworktracker.data.entity.DayOfWeek
import com.rudra.smartworktracker.data.entity.RecurringFrequency
import com.rudra.smartworktracker.data.entity.TransactionType
import java.util.Calendar
import com.rudra.smartworktracker.utils.CurrencyManager

@Composable
fun RecurringHeader(
    activeRulesCount: Int,
    upcomingTransactionsCount: Int,
    totalIncomeThisMonth: Double,
    totalExpensesThisMonth: Double
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recurring Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HeaderStatItem(label = "Active", value = activeRulesCount.toString())
                    HeaderStatItem(label = "Upcoming", value = upcomingTransactionsCount.toString())
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HeaderStatItem(
                        label = "Income/Month",
                        value = "${CurrencyManager.formatWhole(totalIncomeThisMonth)}",
                        valueColor = Color(0xFFBBF7D0)
                    )
                    HeaderStatItem(
                        label = "Expenses/Month",
                        value = "${CurrencyManager.formatWhole(totalExpensesThisMonth)}",
                        valueColor = Color(0xFFFECACA)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val net = totalIncomeThisMonth - totalExpensesThisMonth
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net Monthly: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${CurrencyManager.formatWhole(net)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (net >= 0) Color(0xFFBBF7D0) else Color(0xFFFECACA),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderStatItem(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { contentDescription = "$label: $value" }
        )
    }
}

fun getTransactionTypeColor(type: TransactionType): Color {
    return when (type) {
        TransactionType.INCOME -> Color(0xFF4CAF50)
        TransactionType.EXPENSE -> Color(0xFFFF5252)
        TransactionType.SAVINGS_ADD -> Color(0xFF2196F3)
        TransactionType.SAVINGS_WITHDRAW -> Color(0xFFFF9800)
        TransactionType.TRANSFER -> Color(0xFF9C27B0)
        else -> Color(0xFF607D8B)
    }
}

fun getTransactionTypeIcon(type: TransactionType): ImageVector {
    return when (type) {
        TransactionType.INCOME -> Icons.Default.AttachMoney
        TransactionType.EXPENSE -> Icons.Default.Savings
        TransactionType.SAVINGS_ADD -> Icons.Default.Savings
        TransactionType.SAVINGS_WITHDRAW -> Icons.Default.SwapHoriz
        TransactionType.TRANSFER -> Icons.Default.SwapHoriz
        else -> Icons.Default.Repeat
    }
}

fun getFrequencyText(frequency: RecurringFrequency): String {
    return when (frequency) {
        RecurringFrequency.DAILY -> "Daily"
        RecurringFrequency.WEEKLY -> "Weekly"
        RecurringFrequency.BIWEEKLY -> "Every 2 Weeks"
        RecurringFrequency.MONTHLY -> "Monthly"
        RecurringFrequency.QUARTERLY -> "Every 3 Months"
        RecurringFrequency.YEARLY -> "Yearly"
        RecurringFrequency.CUSTOM -> "Custom"
        RecurringFrequency.WEEKLY_SPECIFIC_DAYS -> "Specific Days"
    }
}

fun getFrequencyDisplayName(frequency: RecurringFrequency): String = getFrequencyText(frequency)

fun calculateNextExecutionDates(
    selectedDays: List<DayOfWeek>,
    startFrom: Long,
    count: Int
): List<Long> {
    val dates = mutableListOf<Long>()
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = startFrom
    val selectedCalendarDays = selectedDays.map { DayOfWeek.toCalendarDay(it) }.sorted()

    repeat(count) {
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        var nextDay: Int? = null
        for (day in selectedCalendarDays) {
            if (day > currentDayOfWeek) { nextDay = day; break }
        }
        if (nextDay != null) {
            calendar.add(Calendar.DAY_OF_YEAR, nextDay - currentDayOfWeek)
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, (7 - currentDayOfWeek) + selectedCalendarDays.first())
        }
        dates.add(calendar.timeInMillis)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }
    return dates
}

fun calculateInitialNextDate(selectedDays: List<DayOfWeek>, startDate: Long): Long {
    if (selectedDays.isEmpty()) return startDate
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = startDate
    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val selectedCalendarDays = selectedDays.map { DayOfWeek.toCalendarDay(it) }.sorted()

    for (day in selectedCalendarDays) {
        if (day >= currentDayOfWeek) {
            calendar.add(Calendar.DAY_OF_YEAR, day - currentDayOfWeek)
            return calendar.timeInMillis
        }
    }
    calendar.add(Calendar.DAY_OF_YEAR, (7 - currentDayOfWeek) + selectedCalendarDays.first())
    return calendar.timeInMillis
}
