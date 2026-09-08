package com.rudra.smartworktracker.ui.screens.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rudra.smartworktracker.data.entity.Account
import com.rudra.smartworktracker.data.entity.AccountCategory
import com.rudra.smartworktracker.data.entity.displayName
import com.rudra.smartworktracker.data.entity.icon
import com.rudra.smartworktracker.utils.CurrencyManager
import java.util.Calendar
import java.util.Locale

@Composable
fun NetWorthCard(netWorth: Double) {
    val greeting = getGreeting()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total Net Worth",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = CurrencyManager.format(netWorth),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun WalletSection(
    wallets: List<Account>,
    total: Double,
    onAccountClick: (Long) -> Unit,
    onEditClick: (Account) -> Unit,
    onDeleteClick: (Account) -> Unit
) {
    AccountSection(
        title = "💳 WALLETS",
        accounts = wallets,
        total = total,
        onAccountClick = onAccountClick,
        onEditClick = onEditClick,
        onDeleteClick = onDeleteClick
    )
}

@Composable
fun BankAccountsSection(
    accounts: List<Account>,
    total: Double,
    onAccountClick: (Long) -> Unit,
    onEditClick: (Account) -> Unit,
    onDeleteClick: (Account) -> Unit
) {
    AccountSection(
        title = "🏦 BANK ACCOUNTS",
        accounts = accounts,
        total = total,
        onAccountClick = onAccountClick,
        onEditClick = onEditClick,
        onDeleteClick = onDeleteClick
    )
}

@Composable
fun MobileBankingSection(
    accounts: List<Account>,
    total: Double,
    onAccountClick: (Long) -> Unit,
    onEditClick: (Account) -> Unit,
    onDeleteClick: (Account) -> Unit
) {
    AccountSection(
        title = "📱 MOBILE BANKING",
        accounts = accounts,
        total = total,
        onAccountClick = onAccountClick,
        onEditClick = onEditClick,
        onDeleteClick = onDeleteClick
    )
}

@Composable
fun AccountSection(
    title: String,
    accounts: List<Account>,
    total: Double,
    onAccountClick: (Long) -> Unit,
    onEditClick: (Account) -> Unit,
    onDeleteClick: (Account) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = CurrencyManager.format(total),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        accounts.forEach { account ->
            AccountCard(
                account = account,
                onClick = { onAccountClick(account.id) },
                onEditClick = { onEditClick(account) },
                onDeleteClick = { onDeleteClick(account) }
            )
        }
    }
}

@Composable
fun AccountCard(
    account: Account,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val cardColor = getAccountCardColor(account)
    val balancePercentage = account.getBalancePercentage()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Column {
                        Text(
                            text = account.nickname ?: account.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = account.provider.displayName(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (account.hasLimit && account.dailyTransferLimit != null) {
                                Text(
                                    text = "• ${account.dailyTransferLimit.toInt()} limit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = maskAccountNumber(account.accountNumber),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyManager.format(account.balance),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    account.maxBalance?.let { max ->
                        Text(
                            text = "of ${CurrencyManager.format(max)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showMenu = false
                                    onEditClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                            )
                        }
                    }
                }
            }

            if (account.maxBalance != null && account.maxBalance > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { balancePercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = getProgressColor(balancePercentage),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(balancePercentage * 100).toInt()}% used",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (account.maxBalance - account.balance > 0) {
                        Text(
                            text = "${CurrencyManager.format(account.maxBalance - account.balance)} left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    onTransfer: () -> Unit,
    onAddAccount: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onTransfer,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Transfer")
        }

        OutlinedButton(
            onClick = onAddAccount,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Account")
        }
    }
}

@Composable
fun getProgressColor(percentage: Float): Color {
    return when {
        percentage >= 0.9f -> Color(0xFFF44336)
        percentage >= 0.7f -> Color(0xFFFF9800)
        percentage >= 0.5f -> Color(0xFFFFC107)
        else -> Color(0xFF4CAF50)
    }
}

fun getAccountCardColor(account: Account): Color {
    return when (account.type) {
        AccountCategory.WALLET -> Color(0xFFF8F5FF)
        AccountCategory.BANK -> Color(0xFFE8F5E9)
        AccountCategory.MOBILE_BANKING -> Color(0xFFFFF3E0)
    }
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "👋 Good morning"
        hour < 17 -> "☀️ Good afternoon"
        else -> "🌙 Good evening"
    }
}

fun formatAmount(amount: Double): String {
    return String.format(Locale.getDefault(), "%,.0f", amount)
}

fun maskAccountNumber(number: String): String {
    return if (number.length > 4) {
        "****${number.takeLast(4)}"
    } else {
        number
    }
}
