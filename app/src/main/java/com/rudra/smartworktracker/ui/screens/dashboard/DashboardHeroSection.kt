package com.rudra.smartworktracker.ui.screens.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

private val IncomeGreen = Color(0xFF10B981)
private val ExpenseRed = Color(0xFFEF4444)
private val AmberHighlight = Color(0xFFF59E0B)
private val IndigoAccent = Color(0xFF6366F1)

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
        today.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    }

    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var showWorkTypeSelector by remember { mutableStateOf(false) }

    val netWorth = financialSummary.allTimeIncome - financialSummary.allTimeExpense
    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val displayLabel = if (selectedAccountId == null) "Net Worth" else selectedAccount?.type?.let { it.displayName() } ?: "Balance"
    val displayValue = if (selectedAccountId == null) netWorth else selectedAccount?.balance ?: 0.0
    val isPositive = displayValue >= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.primary
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$greeting,",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    if (accounts.isNotEmpty()) {
                        AccountSelectorPill(
                            selectedAccountId = selectedAccountId,
                            accounts = accounts,
                            onClick = { accountDropdownExpanded = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Net Worth Display
                Column {
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AnimatedDoubleCounter(
                        targetValue = displayValue,
                        prefix = "\u09F3",
                        color = Color.White,
                        fontSize = 40.sp,
                        durationMillis = 1000
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Divider
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.15f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Summary Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroStatPill(
                        label = "Income",
                        value = financialSummary.allTimeIncome,
                        icon = Icons.Default.TrendingUp,
                        containerColor = IncomeGreen.copy(alpha = 0.2f),
                        iconColor = IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatPill(
                        label = "Expense",
                        value = financialSummary.allTimeExpense,
                        icon = Icons.Default.TrendingDown,
                        containerColor = ExpenseRed.copy(alpha = 0.2f),
                        iconColor = ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatPill(
                        label = "Savings",
                        value = financialSummary.monthlyNetSavings,
                        icon = Icons.Default.Savings,
                        containerColor = AmberHighlight.copy(alpha = 0.2f),
                        iconColor = AmberHighlight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroActionChip(
                        icon = Icons.Default.SwapHoriz,
                        label = "Accounts",
                        onClick = onNavigateToAccounts,
                        modifier = Modifier.weight(1f)
                    )
                    HeroActionChip(
                        icon = Icons.Default.LocalFireDepartment,
                        label = "Streak: 1",
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                    HeroActionChip(
                        icon = Icons.Default.AccountBalanceWallet,
                        label = "Work Type",
                        onClick = { showWorkTypeSelector = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Work Type Selector
                if (showWorkTypeSelector) {
                    Spacer(modifier = Modifier.height(12.dp))
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
private fun AccountSelectorPill(
    selectedAccountId: Long?,
    accounts: List<Account>,
    onClick: () -> Unit
) {
    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val displayText = selectedAccount?.name ?: "All"

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "pillScale"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.15f),
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = displayText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(80.dp)
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun HeroStatPill(
    label: String,
    value: Double,
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedDoubleCounter(
                targetValue = value,
                prefix = "\u09F3",
                color = Color.White,
                fontSize = 13.sp,
                durationMillis = 800
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun HeroActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "chipScale"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = if (isPressed) 0.25f else 0.12f),
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
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
        WorkTypeOption(WorkType.OFFICE, Icons.Default.Work, "Office", IndigoAccent),
        WorkTypeOption(WorkType.HOME_OFFICE, Icons.Default.Home, "Home", AmberHighlight),
        WorkTypeOption(WorkType.OFF_DAY, Icons.Default.BeachAccess, "Off Day", Color(0xFF8B5CF6)),
        WorkTypeOption(WorkType.OVERTIME, Icons.Default.Bolt, "Overtime", ExpenseRed)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Today's Work Type",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            workTypeOptions.forEach { option ->
                val isSelected = currentType == option.workType
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.93f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
                    label = "workTypeScale"
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) option.color.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                    border = if (isSelected) BorderStroke(1.5.dp, option.color.copy(alpha = 0.6f)) else null,
                    modifier = Modifier
                        .weight(1f)
                        .scale(scale)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onTypeSelected(option.workType) }
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            option.icon,
                            contentDescription = null,
                            tint = if (isSelected) option.color else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) option.color else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

private data class WorkTypeOption(
    val workType: WorkType,
    val icon: ImageVector,
    val label: String,
    val color: Color
)
