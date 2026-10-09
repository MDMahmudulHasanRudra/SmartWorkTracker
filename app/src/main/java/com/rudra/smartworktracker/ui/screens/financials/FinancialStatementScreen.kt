package com.rudra.smartworktracker.ui.screens.financials

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.data.entity.FinancialTransaction
import com.rudra.smartworktracker.data.entity.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FinancialStatementScreen(
    onEditTransaction: (FinancialTransaction) -> Unit = {},
    onAddTransaction: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: FinancialStatementViewModel = viewModel(
        factory = FinancialStatementViewModelFactory(context.applicationContext as Application)
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDatePickerRange by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<UnifiedTransaction?>(null) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = { 
                Text("Are you sure you want to delete this transaction? This will remove both the debit and credit entries.")
            },
            confirmButton = {
                TextButton(onClick = {
                    transactionToDelete?.let { viewModel.deleteTransaction(it) }
                    transactionToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDatePickerRange) {
        val datePickerState = rememberDateRangePickerState()
        DateRangePickerDialog(
            state = datePickerState,
            onDismiss = { showDatePickerRange = false },
            onConfirm = {
                // Picker values are UTC midnights; reading them in the local zone shifts the day west of UTC
                val start = datePickerState.selectedStartDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                }
                val end = datePickerState.selectedEndDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                }
                if (start != null && end != null) {
                    viewModel.setDateRange(start, end)
                }
                showDatePickerRange = false
            }
        )
    }

    Scaffold(

        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            SummaryCard(
                totalIncome = uiState.totalIncome,
                totalExpenses = uiState.totalExpenses,
                netFlow = uiState.netFlow
            )

            FilterSection(
                selectedFilter = uiState.filter,
                onFilterSelected = { filter ->
                    if (filter == TransactionFilter.DATE_RANGE) {
                        showDatePickerRange = true
                    } else {
                        viewModel.setFilter(filter)
                    }
                },
                startDate = uiState.startDate,
                endDate = uiState.endDate,
                onClearDateRange = { viewModel.setDateRange(null, null) }
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (uiState.transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    item {
                        Text(
                            text = "Double-Entry Ledger (Debit & Credit)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(items = uiState.transactions, key = { it.id }) { transaction ->
                        DoubleEntryTransactionItem(
                            transaction = transaction,
                            onDelete = { transactionToDelete = transaction }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    selectedFilter: TransactionFilter,
    onFilterSelected: (TransactionFilter) -> Unit,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onClearDateRange: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransactionFilter.values().forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter || (filter == TransactionFilter.DATE_RANGE && startDate != null),
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.displayName) },
                    leadingIcon = if (selectedFilter == filter) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        // Show date range indicator when dates are set (independent of filter type)
        if (startDate != null && endDate != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${startDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))} – ${endDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClearDateRange, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    state: DateRangePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = state.selectedEndDateMillis != null) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(
            state = state,
            title = { Text("Select Date Range", modifier = Modifier.padding(16.dp)) },
            headline = { 
                Text(
                    text = "Select your start and end dates",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            },
            showModeToggle = false,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryCard(totalIncome: Double, totalExpenses: Double, netFlow: Double) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Financial Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            IncomeExpenseBar(income = totalIncome, expense = totalExpenses, modifier = Modifier.fillMaxWidth().height(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryStat(label = "Income", amount = totalIncome, color = MaterialTheme.colorScheme.primary)
                SummaryStat(label = "Expense", amount = totalExpenses, color = MaterialTheme.colorScheme.error)
                SummaryStat(
                    label = "Net Flow", 
                    amount = netFlow, 
                    color = if (netFlow >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun SummaryStat(label: String, amount: Double, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = formatCurrency(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

@Composable
fun DoubleEntryTransactionItem(
    transaction: UnifiedTransaction,
    onDelete: (UnifiedTransaction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)
    
    val isDebit = transaction.entryType == EntryType.DEBIT
    val isIncomeType = transaction.type == TransactionType.INCOME || transaction.type == TransactionType.LOAN_RECEIVE
    val accentColor = if (isIncomeType) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val entryLabel = if (isDebit) "DR" else "CR"
    val entryLabelColor = if (isDebit) Color(0xFF1976D2) else Color(0xFF388E3C)

    Card(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDebit) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
            else 
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Entry Type Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = entryLabelColor.copy(alpha = 0.2f),
                    modifier = Modifier.width(28.dp).height(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = entryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = entryLabelColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Account Info
                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        if (transaction.debitAccount != null) {
                            Text(
                                text = transaction.debitAccount,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (transaction.debitAccount != null && transaction.creditAccount != null) {
                            Icon(
                                Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp).padding(horizontal = 4.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (transaction.creditAccount != null) {
                            Text(
                                text = transaction.creditAccount,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Row {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor
                        )
                        Text(
                            text = " • ${formatTransactionDate(transaction.date)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " (${transaction.sourceType.name.lowercase().replaceFirstChar { it.uppercase() }})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Amount
                Text(
                    text = formatCurrency(transaction.amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                
                // Delete button (only show for non-credit entries to avoid double delete)
                if (!transaction.id.endsWith("_credit")) {
                    IconButton(onClick = { onDelete(transaction) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Delete", 
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(32.dp))
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun IncomeExpenseBar(income: Double, expense: Double, modifier: Modifier = Modifier) {
    val total = (income + expense).coerceAtLeast(1.0)
    val incomeRatio = (income / total).toFloat()
    val expenseRatio = 1f - incomeRatio

    Row(modifier = modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
        // Use weight-based layout to prevent overlap
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(incomeRatio.coerceAtLeast(0.01f))
                .background(MaterialTheme.colorScheme.primary)
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(expenseRatio.coerceAtLeast(0.01f))
                .background(MaterialTheme.colorScheme.error)
        )
    }
}

fun formatTransactionDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
}

@Composable
fun formatCurrency(amount: Double): String {
    // Use proper Bangladeshi Taka currency format
    val currencyFormat = remember { 
        NumberFormat.getCurrencyInstance(Locale("bn", "BD")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        } 
    }
    return currencyFormat.format(amount)
}

/**
 * Non-composable currency formatter for use in non-Compose contexts
 */
fun formatCurrencyStatic(amount: Double): String {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("bn", "BD")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }
    return currencyFormat.format(amount)
}
