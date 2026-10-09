package com.rudra.smartworktracker.ui.screens.expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.model.ExpenseCategory
import com.rudra.smartworktracker.ui.components.AccountPickerField
import com.rudra.smartworktracker.ui.components.AmountInputField
import com.rudra.smartworktracker.ui.components.EmptyStateCard
import com.rudra.smartworktracker.ui.components.EntryDateField
import com.rudra.smartworktracker.ui.components.EntryHeroCard
import com.rudra.smartworktracker.ui.components.EntrySectionCard
import com.rudra.smartworktracker.ui.components.RecentEntryRow
import com.rudra.smartworktracker.ui.components.formatEntryDate
import com.rudra.smartworktracker.utils.CurrencyManager

fun ExpenseCategory.icon(): ImageVector = when (this) {
    ExpenseCategory.MEAL -> Icons.Default.Restaurant
    ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsCar
    ExpenseCategory.SHOPPING -> Icons.Default.ShoppingBag
    ExpenseCategory.ENTERTAINMENT -> Icons.Default.Movie
    ExpenseCategory.BILLS -> Icons.Default.Receipt
    ExpenseCategory.HEALTHCARE -> Icons.Default.LocalHospital
    ExpenseCategory.EDUCATION -> Icons.Default.School
    ExpenseCategory.PERSONAL_CARE -> Icons.Default.Face
    ExpenseCategory.GIFTS -> Icons.Default.CardGiftcard
    ExpenseCategory.TRAVEL -> Icons.Default.Flight
    ExpenseCategory.SUBSCRIPTIONS -> Icons.Default.Subscriptions
    ExpenseCategory.OTHER -> Icons.AutoMirrored.Filled.List
}

private val ExpenseRed = Color(0xFFEF4444)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseScreen(viewModel: ExpenseViewModel = viewModel()) {
    var amount by rememberSaveable { mutableStateOf("") }
    var merchant by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(ExpenseCategory.MEAL) }
    var timestamp by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var accountInitialized by rememberSaveable { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val recentExpenses by viewModel.recentExpenses.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val monthTotal by viewModel.monthTotal.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Default to the cash wallet once, without overriding a later choice when balances change
    LaunchedEffect(accounts) {
        if (!accountInitialized && accounts.isNotEmpty()) {
            selectedAccountId = (accounts.find { it.name == "Cash" } ?: accounts.first()).id
            accountInitialized = true
        }
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    val parsedAmount = amount.toDoubleOrNull()
    val selectedAccount = accounts.find { it.id == selectedAccountId }
    val amountError = when {
        !showErrors -> null
        parsedAmount == null || parsedAmount <= 0 -> "Enter an amount greater than zero"
        else -> null
    }
    val balanceWarning = if (selectedAccount != null && parsedAmount != null && parsedAmount > selectedAccount.balance) {
        "This exceeds the ${selectedAccount.name} balance (${CurrencyManager.format(selectedAccount.balance)})"
    } else null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                EntryHeroCard(
                    title = "Log an Expense",
                    subtitle = "Track where your money goes",
                    icon = Icons.Default.MoneyOff,
                    gradient = listOf(Color(0xFFF97316), ExpenseRed),
                    totalLabel = "Spent this month",
                    totalValue = monthTotal
                )
            }

            item {
                EntrySectionCard {
                    AmountInputField(
                        value = amount,
                        onValueChange = { amount = it },
                        errorText = amountError,
                        supportingText = balanceWarning
                    )
                    Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ExpenseCategory.entries.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category.displayName) },
                                leadingIcon = {
                                    Icon(
                                        category.icon(),
                                        contentDescription = null,
                                        tint = category.color,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                EntrySectionCard {
                    AccountPickerField(
                        accounts = accounts,
                        selectedAccountId = selectedAccountId,
                        onAccountSelected = { selectedAccountId = it },
                        label = "Paid from"
                    )
                    EntryDateField(timestamp = timestamp, onTimestampChange = { timestamp = it })
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text("Merchant (optional)") },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        showErrors = true
                        val value = parsedAmount
                        if (value != null && value > 0) {
                            viewModel.saveExpense(
                                amount = value,
                                category = selectedCategory,
                                merchant = merchant.trim().ifBlank { null },
                                notes = notes.trim().ifBlank { null },
                                timestamp = timestamp,
                                accountId = selectedAccountId
                            )
                            amount = ""
                            merchant = ""
                            notes = ""
                            timestamp = System.currentTimeMillis()
                            showErrors = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Expense", fontWeight = FontWeight.SemiBold)
                }
            }

            item {
                Text("Recent expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (recentExpenses.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.ReceiptLong,
                        title = "No expenses yet",
                        message = "Expenses you log will show up here."
                    )
                }
            } else {
                items(recentExpenses, key = { it.id }) { expense ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        RecentEntryRow(
                            title = expense.merchant?.takeIf { it.isNotBlank() }
                                ?: expense.notes?.takeIf { it.isNotBlank() }
                                ?: expense.category.displayName,
                            subtitle = "${expense.category.displayName} • ${formatEntryDate(expense.timestamp)}",
                            amountText = "-${CurrencyManager.format(expense.amount)}",
                            amountColor = ExpenseRed,
                            icon = expense.category.icon(),
                            iconTint = expense.category.color,
                            onDelete = { viewModel.deleteExpense(expense) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
