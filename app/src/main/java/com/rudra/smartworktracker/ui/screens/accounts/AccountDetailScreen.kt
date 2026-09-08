package com.rudra.smartworktracker.ui.screens.accounts

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.data.entity.Account
import com.rudra.smartworktracker.data.entity.displayName
import com.rudra.smartworktracker.data.entity.icon
import com.rudra.smartworktracker.utils.CurrencyManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToTransfer: () -> Unit = {}
) {
    val viewModel: AccountDetailViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var showCashOutDialog by remember { mutableStateOf(false) }
    var showSendDialog by remember { mutableStateOf(false) }

    LaunchedEffect(accountId) {
        viewModel.loadAccountDetails(accountId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.account?.nickname ?: uiState.account?.name ?: "Account") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            uiState.account?.let { account ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        AccountBalanceCard(account = account)
                    }

                    item {
                        QuickActionsRow(
                            account = account,
                            onAddMoney = { showAddMoneyDialog = true },
                            onCashOut = { showCashOutDialog = true },
                            onSend = { showSendDialog = true }
                        )
                    }

                    item {
                        BalanceHistoryChart(history = uiState.balanceHistory)
                    }

                    item {
                        RecentTransactionsSection(transactions = uiState.recentTransactions)
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    uiState.account?.let { account ->
        if (showAddMoneyDialog) {
            AccountAmountDialog(
                title = "Add Money",
                accountName = account.nickname ?: account.name,
                icon = Icons.Default.Add,
                onDismiss = { showAddMoneyDialog = false },
                onConfirm = { amount ->
                    viewModel.addMoneyToAccount(account.id, amount) { success, message ->
                        showAddMoneyDialog = false
                    }
                }
            )
        }

        if (showCashOutDialog) {
            AccountAmountDialog(
                title = "Cash Out",
                accountName = account.nickname ?: account.name,
                icon = Icons.Default.Remove,
                onDismiss = { showCashOutDialog = false },
                onConfirm = { amount ->
                    viewModel.cashOutFromAccount(account.id, amount) { success, message ->
                        showCashOutDialog = false
                    }
                }
            )
        }

        LaunchedEffect(showSendDialog) {
            if (showSendDialog) {
                onNavigateToTransfer()
                showSendDialog = false
            }
        }
    }
}

@Composable
fun AccountBalanceCard(account: Account) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = account.provider.icon(),
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = account.nickname ?: account.name,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = CurrencyManager.format(account.balance),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            val lastUpdated = remember(account.lastUpdated) {
                val sdf = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
                sdf.format(Date(account.lastUpdated))
            }
            Text(
                text = "Last updated: $lastUpdated",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun QuickActionsRow(
    account: Account,
    onAddMoney: () -> Unit,
    onCashOut: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            icon = Icons.Default.Add,
            label = "Add Money",
            onClick = onAddMoney
        )
        QuickActionButton(
            icon = Icons.Default.Remove,
            label = "Cash Out",
            onClick = onCashOut
        )
        QuickActionButton(
            icon = Icons.Default.Send,
            label = "Send",
            onClick = onSend
        )
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun BalanceHistoryChart(history: List<BalanceHistoryItem>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
                Text(
                    text = "Balance History (Last 7 days)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val maxBalance = history.maxOfOrNull { it.balance } ?: 4000.0
                val minBalance = history.minOfOrNull { it.balance } ?: 2000.0
                val range = maxBalance - minBalance

                history.forEach { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        val heightFraction = if (range > 0) {
                            ((item.balance - minBalance) / range).toFloat()
                        } else 0.5f

                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height((60 * heightFraction + 20).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.dayLabel,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                history.forEach { item ->
                    Text(
                        text = "${(item.balance / 1000).toInt()}k",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun RecentTransactionsSection(transactions: List<com.rudra.smartworktracker.data.entity.FinancialTransaction>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (transactions.isEmpty()) {
                Text(
                    text = "No transactions yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                val dateFormat = remember { SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()) }
                transactions.forEachIndexed { index, transaction ->
                    val isIncome = transaction.type == com.rudra.smartworktracker.data.entity.TransactionType.INCOME ||
                            transaction.type == com.rudra.smartworktracker.data.entity.TransactionType.LOAN_RECEIVE
                    val amountPrefix = if (isIncome) "+ " else "- "
                    val amountColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
                    val icon = if (isIncome) Icons.Default.ArrowBack else Icons.Default.ArrowForward

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dateFormat.format(Date(transaction.date)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = amountColor
                                )
                                Text(
                                    text = transaction.note.ifEmpty { transaction.type.name },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Text(
                            text = "$amountPrefix${CurrencyManager.format(transaction.amount)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = amountColor
                            )
                        )
                    }
                    if (index < transactions.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountAmountDialog(
    title: String,
    accountName: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Account: $accountName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (
                            newValue.all { c -> c.isDigit() || c == '.' } &&
                            newValue.count { it == '.' } <= 1
                        )) amount = newValue
                    },
                    label = { Text("Amount") },
                    prefix = { Text("${CurrencyManager.symbol()} ") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    if (parsedAmount > 0) onConfirm(parsedAmount)
                },
                enabled = amount.toDoubleOrNull() != null && (amount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}