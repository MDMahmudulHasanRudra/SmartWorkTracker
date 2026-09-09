package com.rudra.smartworktracker.ui.screens.savings

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.data.entity.Account
import com.rudra.smartworktracker.data.entity.Savings
import com.rudra.smartworktracker.data.entity.displayName
import com.rudra.smartworktracker.data.entity.icon
import com.rudra.smartworktracker.ui.components.EmptyStateCard
import com.rudra.smartworktracker.utils.CurrencyManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen() {
    val context = LocalContext.current
    val viewModel: SavingsViewModel = viewModel(factory = SavingsViewModelFactory(context.applicationContext as Application))
    val uiState by viewModel.uiState.collectAsState()

    var showHistory by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(TimeRange.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Savings Management",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    Button(
                        onClick = { showHistory = !showHistory },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.History, contentDescription = "History")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(if (showHistory) "Hide" else "Show")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Total savings card
                item {
                    AnimatedSavingsCard(savings = uiState.savings)
                }

                // Per-account savings cards
                if (uiState.accounts.isNotEmpty()) {
                    item {
                        AccountSavingsOverview(
                            accounts = uiState.accounts,
                            accountSavings = uiState.accountSavings,
                            selectedAccountId = uiState.selectedAccountId,
                            onAccountSelected = { viewModel.selectAccount(it) }
                        )
                    }
                }

                // Stats
                item {
                    SavingsStatsCards(stats = uiState.stats)
                }

                // Filter chips
                item {
                    FilterChips(
                        selectedRange = selectedFilter,
                        onRangeSelected = {
                            selectedFilter = it
                            viewModel.filterByTimeRange(it)
                        }
                    )
                }

                // Chart
                item {
                    SavingsHistoryChart(history = uiState.filteredHistory.ifEmpty { uiState.savingsHistory })
                }

                // Add button
                item {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Add Transaction")
                    }
                }

                // History list
                if (showHistory && uiState.filteredHistory.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { viewModel.toggleSortOrder() }) {
                                Icon(
                                    if (uiState.sortOrder == SortOrder.DESCENDING)
                                        Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = "Sort"
                                )
                            }
                        }
                    }
                    items(uiState.filteredHistory) { savings ->
                        SavingsHistoryItem(
                            savings = savings,
                            accounts = uiState.accounts,
                            onDelete = { viewModel.deleteTransaction(savings) }
                        )
                    }
                } else if (showHistory) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Savings,
                            title = "No transactions yet",
                            message = "Add a deposit or withdrawal to start tracking your savings."
                        )
                    }
                }
            }
        }

        // Add Transaction Dialog
        if (showAddDialog) {
            AddTransactionDialog(
                accounts = uiState.accounts,
                onDismiss = { showAddDialog = false },
                onAdd = { amt, nte, isWtd, accountId ->
                    if (isWtd) {
                        viewModel.withdrawFromSavings(amt, nte, accountId)
                    } else {
                        viewModel.addToSavings(amt, nte, accountId)
                    }
                    showAddDialog = false
                }
            )
        }

        // Error/Success Messages
        uiState.errorMessage?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }

        uiState.successMessage?.let { success ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Snackbar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(success)
                }
            }
        }
    }
}

@Composable
fun AccountSavingsOverview(
    accounts: List<Account>,
    accountSavings: Map<Long, Double>,
    selectedAccountId: Long?,
    onAccountSelected: (Long?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Savings by Account",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // All accounts option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onAccountSelected(null) }
                    .background(
                        if (selectedAccountId == null)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else Color.Transparent
                    )
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Savings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("All Accounts", style = MaterialTheme.typography.bodyMedium)
                }
                if (selectedAccountId == null) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Per-account rows
            accounts.forEach { account ->
                val savings = accountSavings[account.id] ?: 0.0
                if (savings != 0.0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onAccountSelected(account.id) }
                            .background(
                                if (selectedAccountId == account.id)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(account.provider.icon(), style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    account.nickname ?: account.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    account.type.displayName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                CurrencyManager.format(savings),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (savings >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            )
                            if (selectedAccountId == account.id) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChips(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimeRange.entries.forEach { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = { Text(range.name.replace("_", " ")) }
            )
        }
    }
}

@Composable
fun AnimatedSavingsCard(savings: Double) {
    val animatedSavings by animateFloatAsState(
        targetValue = savings.toFloat(),
        animationSpec = tween(durationMillis = 1500),
        label = "savings_animation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Savings,
                    contentDescription = "Savings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Total Savings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = CurrencyManager.format(animatedSavings.toDouble()),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SavingsHistoryChart(history: List<Savings>) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No savings history yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Savings Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxSavings = history.maxOfOrNull { it.amount } ?: 1.0
                    val minSavings = history.minOfOrNull { it.amount } ?: 0.0
                    val range = maxSavings - minSavings

                    for (i in 0..4) {
                        val y = size.height * (1 - i * 0.25f)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }

                    val path = Path()
                    val gradientPath = Path()

                    for (index in history.indices) {
                        val savings = history[index]
                        val x = (index.toFloat() / (history.size - 1).coerceAtLeast(1).toFloat()) * size.width
                        val y = if (range > 0) {
                            size.height - ((savings.amount - minSavings) / range * size.height).toFloat()
                        } else {
                            size.height / 2
                        }

                        if (index == 0) {
                            path.moveTo(x, y)
                            gradientPath.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            gradientPath.lineTo(x, y)
                        }

                        drawCircle(
                            color = Color(0xFF2196F3),
                            radius = 4f,
                            center = Offset(x, y)
                        )
                    }

                    gradientPath.lineTo(size.width, size.height)
                    gradientPath.lineTo(0f, size.height)
                    gradientPath.close()

                    drawPath(
                        path = gradientPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2196F3).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )

                    drawPath(
                        path = path,
                        color = Color(0xFF2196F3),
                        style = Stroke(width = 3f)
                    )
                }
            }
        }
    }
}

@Composable
fun SavingsHistoryList(
    history: List<Savings>,
    accounts: List<Account>,
    onDelete: (Savings) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Transaction History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(history.reversed()) { savings ->
                    SavingsHistoryItem(savings = savings, accounts = accounts, onDelete = onDelete)
                }
            }
        }
    }
}

@Composable
fun SavingsHistoryItem(
    savings: Savings,
    accounts: List<Account>,
    onDelete: (Savings) -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val accountName = savings.accountId?.let { id ->
        accounts.find { it.id == id }?.let { it.nickname ?: it.name }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = savings.note.ifEmpty { if (savings.amount >= 0) "Deposit" else "Withdrawal" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${dateFormat.format(Date(savings.timestamp))} • ${savings.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (accountName != null) {
                        Text(
                            text = " • $accountName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Text(
                text = CurrencyManager.format(kotlin.math.abs(savings.amount)),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (savings.amount >= 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
            if (onDelete != {}) {
                IconButton(onClick = { onDelete(savings) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun SavingsStatsCards(stats: SavingsStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Deposits",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    CurrencyManager.format(stats.totalDeposits),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Withdrawals",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    CurrencyManager.format(stats.totalWithdrawals),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Transactions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${stats.transactionCount}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AddTransactionDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onAdd: (Double, String, Boolean, Long?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isWithdrawal by remember { mutableStateOf(false) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }

    val savingsAccounts = remember(accounts) {
        accounts.filter { it.provider == com.rudra.smartworktracker.data.entity.AccountProvider.SAVINGS }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isWithdrawal) "Withdraw Money" else "Add Money") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                            amount = it
                        }
                    },
                    label = { Text("Amount (${CurrencyManager.symbol()})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = !isWithdrawal,
                        onClick = { isWithdrawal = false },
                        label = { Text("Deposit") }
                    )
                    FilterChip(
                        selected = isWithdrawal,
                        onClick = { isWithdrawal = true },
                        label = { Text("Withdrawal") }
                    )
                }

                // Account picker
                if (savingsAccounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Link to Account (Optional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    savingsAccounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedAccountId = if (selectedAccountId == account.id) null else account.id
                                }
                                .background(
                                    if (selectedAccountId == account.id)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else Color.Transparent
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(account.provider.icon())
                            Text(
                                account.nickname ?: account.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedAccountId == account.id) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amount.toDoubleOrNull()?.let { validAmount ->
                        onAdd(validAmount, note, isWithdrawal, selectedAccountId)
                    }
                },
                enabled = amount.toDoubleOrNull() ?: 0.0 > 0
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
