package com.rudra.smartworktracker.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.smartworktracker.data.entity.Account
import com.rudra.smartworktracker.data.entity.AccountCategory
import com.rudra.smartworktracker.data.entity.displayName
import com.rudra.smartworktracker.model.WorkType
import com.rudra.smartworktracker.ui.FinancialSummary
import com.rudra.smartworktracker.ui.components.AnimatedDoubleCounter
import com.rudra.smartworktracker.utils.CurrencyManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHeroSection(
    userName: String?,
    financialSummary: FinancialSummary,
    accounts: List<Account>,
    todayWorkType: WorkType?,
    onWorkTypeClick: (WorkType) -> Unit,
    onNavigateToAccounts: () -> Unit
) {
    val displayName = remember(userName) { userName ?: "User" }
    val today = remember { LocalDate.now() }
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }
    val dateStr = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
    }

    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var heroExpanded by remember { mutableStateOf(true) }
    var showWorkTypeSelector by remember { mutableStateOf(false) }

    val netWorth = financialSummary.allTimeIncome - financialSummary.allTimeExpense
    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val displayLabel = if (selectedAccountId == null) "Net Worth" else "${selectedAccount?.type?.let { it.displayName() } ?: "Balance"}"
    val displayValue = if (selectedAccountId == null) netWorth else selectedAccount?.balance ?: 0.0
    val isPositive = displayValue >= 0

    val incomeColor = Color(0xFF4CAF50)
    val expenseColor = MaterialTheme.colorScheme.error
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Top Row: Greeting + Account Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$greeting, $displayName",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Account Selector Button
                if (accounts.isNotEmpty()) {
                    AccountSelectorButton(
                        selectedAccountId = selectedAccountId,
                        accounts = accounts,
                        onClick = { accountDropdownExpanded = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Net Worth Display with Animation
            AnimatedDoubleCounter(
                targetValue = displayValue,
                prefix = "\u09F3",
                color = if (isPositive) incomeColor else expenseColor,
                fontSize = 42.sp,
                durationMillis = 1200
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Label
            Text(
                text = displayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Expandable Details Section
            if (heroExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Divider
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    // Summary Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HeroStatItem(
                            label = "Income",
                            value = financialSummary.allTimeIncome,
                            color = incomeColor,
                            icon = Icons.Default.TrendingUp,
                            trend = "+12.5%",
                            incomeColor = incomeColor,
                            expenseColor = expenseColor
                        )
                        HeroStatItem(
                            label = "Expense",
                            value = financialSummary.allTimeExpense,
                            color = expenseColor,
                            icon = Icons.Default.TrendingUp,
                            trend = "+8.2%",
                            isNegative = true,
                            incomeColor = incomeColor,
                            expenseColor = expenseColor
                        )
                        HeroStatItem(
                            label = "Savings",
                            value = financialSummary.monthlyNetSavings,
                            color = if (financialSummary.monthlyNetSavings >= 0) incomeColor else expenseColor,
                            icon = Icons.Default.Savings,
                            trend = "${financialSummary.savingsPercentage}%",
                            incomeColor = incomeColor,
                            expenseColor = expenseColor
                        )
                    }

                    // Quick Actions Row - using simple layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionItem(
                            icon = Icons.Default.SwapHoriz,
                            label = "Accounts",
                            onClick = onNavigateToAccounts
                        )
                        ActionItem(
                            icon = Icons.Default.LocalFireDepartment,
                            label = "Streak: 1",
                            onClick = {}
                        )
                        ActionItem(
                            icon = Icons.Default.AccountBalanceWallet,
                            label = "Today's Work",
                            onClick = { showWorkTypeSelector = true }
                        )
                    }

                    // Today's Work Type Selector
                    if (showWorkTypeSelector) {
                        WorkTypeSelector(
                            currentType = todayWorkType,
                            onTypeSelected = { type ->
                                onWorkTypeClick(type)
                                showWorkTypeSelector = false
                            },
                            onDismiss = { showWorkTypeSelector = false }
                        )
                    }
                }
            }

            // Expand/Collapse Toggle
            IconButton(
                onClick = { heroExpanded = !heroExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = if (heroExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropDown,
                    contentDescription = if (heroExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Account Dropdown Menu
    if (accountDropdownExpanded && accounts.isNotEmpty()) {
        DropdownMenu(
            expanded = accountDropdownExpanded,
            onDismissRequest = { accountDropdownExpanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text("Total Net Worth", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = CurrencyManager.format(netWorth),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                onClick = {
                    selectedAccountId = null
                    accountDropdownExpanded = false
                }
            )
            HorizontalDivider()
            accounts.filter { it.isActive }.forEach { account ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = account.name,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${account.type.displayName()} \u2022 ${CurrencyManager.format(account.balance)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        selectedAccountId = account.id
                        accountDropdownExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AccountSelectorButton(
    selectedAccountId: Long?,
    accounts: List<Account>,
    onClick: () -> Unit
) {
    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val displayText = selectedAccount?.name ?: "All Accounts"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth(0.4f)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun HeroStatItem(
    label: String,
    value: Double,
    color: Color,
    icon: ImageVector,
    trend: String,
    isNegative: Boolean = false,
    incomeColor: Color,
    expenseColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        AnimatedDoubleCounter(
            targetValue = value,
            prefix = "\u09F3",
            color = color,
            fontSize = 16.sp,
            durationMillis = 800
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = trend,
            style = MaterialTheme.typography.labelSmall,
            color = if (isNegative) expenseColor else incomeColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WorkTypeSelector(
    currentType: WorkType?,
    onTypeSelected: (WorkType) -> Unit,
    onDismiss: () -> Unit
) {
    val workTypeOptions = listOf(
        WorkTypeOption(WorkType.OFFICE, Icons.Default.Work, "Office", Color(0xFF2196F3)),
        WorkTypeOption(WorkType.HOME_OFFICE, Icons.Default.Home, "Home Office", Color(0xFFFF9800)),
        WorkTypeOption(WorkType.OFF_DAY, Icons.Default.BeachAccess, "Off Day", Color(0xFF9C27B0)),
        WorkTypeOption(WorkType.OVERTIME, Icons.Default.Bolt, "Overtime", Color(0xFFE91E63))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Select Today's Work Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            workTypeOptions.forEach { option ->
                val isSelected = currentType == option.workType
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { onTypeSelected(option.workType) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) option.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, option.color) else androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(option.icon, contentDescription = null, tint = if (isSelected) option.color else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Text(text = option.label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) option.color else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

data class WorkTypeOption(
    val workType: WorkType,
    val icon: ImageVector,
    val label: String,
    val color: Color
)