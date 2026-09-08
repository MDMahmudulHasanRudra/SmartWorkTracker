package com.rudra.smartworktracker.ui.screens.savings

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.data.entity.Savings
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

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(TimeRange.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isWithdrawal by remember { mutableStateOf(false) }

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Current Savings with animation
                AnimatedSavingsCard(savings = uiState.savings)

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Cards
                SavingsStatsCards(stats = uiState.stats)

                Spacer(modifier = Modifier.height(16.dp))

                // Filter Chips
                FilterChips(
                    selectedRange = selectedFilter,
                    onRangeSelected = {
                        selectedFilter = it
                        viewModel.filterByTimeRange(it)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Savings Chart
                SavingsHistoryChart(history = uiState.filteredHistory.ifEmpty { uiState.savingsHistory })

                Spacer(modifier = Modifier.height(16.dp))

                // Add/Withdraw Button
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

                Spacer(modifier = Modifier.height(16.dp))

                // History List
                if (showHistory && uiState.filteredHistory.isNotEmpty()) {
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
                        androidx.compose.material3.IconButton(onClick = { viewModel.toggleSortOrder() }) {
                            Icon(
                                if (uiState.sortOrder == SortOrder.DESCENDING)
                                    Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = "Sort"
                            )
                        }
                    }
                    SavingsHistoryList(
                        history = uiState.filteredHistory,
                        onDelete = { savings -> viewModel.deleteTransaction(savings) }
                    )
                } else if (showHistory) {
                    EmptyStateCard(
                        icon = Icons.Default.Savings,
                        title = "No transactions yet",
                        message = "Add a deposit or withdrawal to start tracking your savings."
                    )
                }
            }
        }

        // Add Transaction Dialog
        if (showAddDialog) {
            AddTransactionDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { amt: Double, nte: String, isWtd: Boolean ->
                    if (isWtd) {
                        viewModel.withdrawFromSavings(amt, nte)
                    } else {
                        viewModel.addToSavings(amt, nte)
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
                androidx.compose.material3.Snackbar(
                    action = {
                        androidx.compose.material3.TextButton(onClick = { viewModel.clearError() }) {
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
                androidx.compose.material3.Snackbar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(success)
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
            androidx.compose.material3.FilterChip(
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
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
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
                    "Current Savings",
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

                    // Draw grid lines
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

                    // Use a regular for loop instead of forEachIndexed
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

                        // Draw data points
                        drawCircle(
                            color = Color(0xFF2196F3), // Blue color
                            radius = 4f,
                            center = Offset(x, y)
                        )
                    }

                    // Complete gradient path
                    gradientPath.lineTo(size.width, size.height)
                    gradientPath.lineTo(0f, size.height)
                    gradientPath.close()

                    // Draw gradient area
                    drawPath(
                        path = gradientPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2196F3).copy(alpha = 0.3f), // Blue with alpha
                                Color.Transparent
                            )
                        )
                    )

                    // Draw line
                    drawPath(
                        path = path,
                        color = Color(0xFF2196F3), // Blue color
                        style = Stroke(width = 3f)
                    )
                }
            }
        }
    }
}
@Composable
fun QuickAmountButtons(onAmountSelected: (Double) -> Unit) {
    val quickAmounts = listOf(100.0, 500.0, 1000.0, 2000.0, 5000.0)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Quick Amounts",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickAmounts.forEach { amount ->
                Button(
                    onClick = { onAmountSelected(amount) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(CurrencyManager.format(amount.toDouble()))
                }
            }
        }
    }
}

@Composable
fun SavingsHistoryList(
    history: List<Savings>,
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
                    SavingsHistoryItem(savings = savings, onDelete = onDelete)
                }
            }
        }
    }
}

@Composable
fun SavingsHistoryItem(savings: Savings, onDelete: (Savings) -> Unit = {}) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

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
                Text(
                    text = "${dateFormat.format(Date(savings.timestamp))} • ${savings.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
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
    onDismiss: () -> Unit,
    onAdd: (Double, String, Boolean) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isWithdrawal by remember { mutableStateOf(false) }

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
                    androidx.compose.material3.FilterChip(
                        selected = !isWithdrawal,
                        onClick = { isWithdrawal = false },
                        label = { Text("Deposit") }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = isWithdrawal,
                        onClick = { isWithdrawal = true },
                        label = { Text("Withdrawal") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amount.toDoubleOrNull()?.let { validAmount ->
                        onAdd(validAmount, note, isWithdrawal)
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