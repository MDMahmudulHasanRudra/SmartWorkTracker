package com.rudra.smartworktracker.ui.screens.creditcard

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.data.entity.CreditCard
import com.rudra.smartworktracker.ui.components.AmountInputField
import com.rudra.smartworktracker.ui.components.ConfirmDeleteDialog
import com.rudra.smartworktracker.ui.components.EmptyStateCard
import com.rudra.smartworktracker.utils.CurrencyManager
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

private val cardGradients = listOf(
    listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)),
    listOf(Color(0xFF0F766E), Color(0xFF059669)),
    listOf(Color(0xFFB45309), Color(0xFFDC2626)),
    listOf(Color(0xFF1E293B), Color(0xFF475569)),
    listOf(Color(0xFFBE185D), Color(0xFF9333EA))
)

private fun ordinal(day: Int): String {
    val suffix = if (day in 11..13) "th" else when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return "$day$suffix"
}

/** Days until the next occurrence of [dayOfMonth] (clamped to short months). */
private fun daysUntil(dayOfMonth: Int): Long {
    val today = LocalDate.now()
    var next = today.withDayOfMonth(dayOfMonth.coerceIn(1, today.lengthOfMonth()))
    if (next.isBefore(today)) {
        val nextMonth = today.plusMonths(1)
        next = nextMonth.withDayOfMonth(dayOfMonth.coerceIn(1, nextMonth.lengthOfMonth()))
    }
    return ChronoUnit.DAYS.between(today, next)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardScreen() {
    val context = LocalContext.current
    val viewModel: CreditCardViewModel = viewModel(factory = CreditCardViewModelFactory(context.applicationContext as Application))
    val creditCards by viewModel.creditCards.collectAsState()
    var showAddCardDialog by remember { mutableStateOf(false) }
    var purchaseCard by remember { mutableStateOf<CreditCard?>(null) }
    var payCard by remember { mutableStateOf<CreditCard?>(null) }
    var historyCard by remember { mutableStateOf<CreditCard?>(null) }
    var deleteCard by remember { mutableStateOf<CreditCard?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Credit Cards", fontWeight = FontWeight.Bold) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCardDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add card") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (creditCards.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.CreditCard,
                        title = "No credit cards",
                        message = "Add a card to track purchases, utilization and due dates.",
                        actionText = "Add card",
                        onAction = { showAddCardDialog = true }
                    )
                }
            } else {
                item { CreditSummaryCard(creditCards) }
                items(creditCards, key = { it.id }) { card ->
                    CreditCardItem(
                        card = card,
                        gradient = cardGradients[(card.id % cardGradients.size + cardGradients.size) % cardGradients.size],
                        onAddTransactionClick = { purchaseCard = card },
                        onPayBillClick = { payCard = card },
                        onHistoryClick = { historyCard = card },
                        onDeleteClick = { deleteCard = card }
                    )
                }
            }
        }
    }

    if (showAddCardDialog) {
        AddCreditCardDialog(
            onDismiss = { showAddCardDialog = false },
            onConfirm = {
                viewModel.addCreditCard(it)
                showAddCardDialog = false
            }
        )
    }
    purchaseCard?.let { card ->
        AddTransactionDialog(
            card = card,
            onDismiss = { purchaseCard = null },
            onConfirm = { amount, description ->
                viewModel.addCardTransaction(card, amount, description)
                purchaseCard = null
            }
        )
    }
    payCard?.let { card ->
        PayBillDialog(
            card = card,
            onDismiss = { payCard = null },
            onConfirm = { amount ->
                viewModel.payCreditCardBill(card, amount)
                payCard = null
            }
        )
    }
    historyCard?.let { card ->
        CardHistorySheet(card = card, viewModel = viewModel, onDismiss = { historyCard = null })
    }
    deleteCard?.let { card ->
        ConfirmDeleteDialog(
            title = "Remove ${card.cardName}?",
            message = "The card and its transaction history will be deleted.",
            confirmText = "Remove",
            onConfirm = {
                viewModel.deleteCreditCard(card)
                deleteCard = null
            },
            onDismiss = { deleteCard = null }
        )
    }
}

@Composable
private fun CreditSummaryCard(cards: List<CreditCard>) {
    val totalLimit = cards.sumOf { it.cardLimit }
    val totalOutstanding = cards.sumOf { it.currentBalance }
    val utilization = if (totalLimit > 0) (totalOutstanding / totalLimit).toFloat().coerceIn(0f, 1f) else 0f
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total outstanding", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CurrencyManager.format(totalOutstanding), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Available credit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        CurrencyManager.format((totalLimit - totalOutstanding).coerceAtLeast(0.0)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            LinearProgressIndicator(
                progress = { utilization },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = utilizationColor(utilization),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Text(
                "${(utilization * 100).toInt()}% utilization across ${cards.size} card${if (cards.size == 1) "" else "s"}" +
                    if (utilization > 0.3f) " — keeping it under 30% helps your credit score" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun utilizationColor(utilization: Float): Color = when {
    utilization >= 0.9f -> MaterialTheme.colorScheme.error
    utilization >= 0.5f -> Color(0xFFF59E0B)
    else -> Color(0xFF10B981)
}

@Composable
fun CreditCardItem(
    card: CreditCard,
    gradient: List<Color>,
    onAddTransactionClick: () -> Unit,
    onPayBillClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val utilization = if (card.cardLimit > 0) (card.currentBalance / card.cardLimit).toFloat().coerceIn(0f, 1f) else 0f
    val dueIn = daysUntil(card.dueDate)

    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(card.cardName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Row {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Transaction history", tint = Color.White)
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove card", tint = Color.White)
                    }
                }
            }
            Text("•••• •••• •••• ${card.cardNumber}", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f), letterSpacing = MaterialTheme.typography.titleMedium.letterSpacing)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Outstanding", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
                    Text(CurrencyManager.format(card.currentBalance), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Limit", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
                    Text(CurrencyManager.format(card.cardLimit), style = MaterialTheme.typography.titleSmall, color = Color.White)
                }
            }
            LinearProgressIndicator(
                progress = { utilization },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Text(
                buildString {
                    append("Statement ${ordinal(card.statementDate)} • Due ${ordinal(card.dueDate)}")
                    if (card.currentBalance > 0) append(if (dueIn == 0L) " (today)" else " (in $dueIn days)")
                    if (card.currentBalance > card.cardLimit) append(" • Over limit")
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = onAddTransactionClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Purchase")
                }
                Button(
                    onClick = onPayBillClick,
                    modifier = Modifier.weight(1f),
                    enabled = card.currentBalance > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = gradient.first())
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pay bill")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardHistorySheet(card: CreditCard, viewModel: CreditCardViewModel, onDismiss: () -> Unit) {
    val transactions by remember(card.id) { viewModel.transactionsFor(card.id) }.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("${card.cardName} history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (transactions.isEmpty()) {
                Text(
                    "No transactions yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                items(transactions, key = { it.id }) { tx ->
                    val isPayment = tx.amount < 0
                    ListItem(
                        headlineContent = { Text(tx.description) },
                        supportingContent = { Text(dateFormat.format(Date(tx.date))) },
                        trailingContent = {
                            Text(
                                (if (isPayment) "-" else "+") + CurrencyManager.format(kotlin.math.abs(tx.amount)),
                                fontWeight = FontWeight.Bold,
                                color = if (isPayment) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
fun AddCreditCardDialog(onDismiss: () -> Unit, onConfirm: (CreditCard) -> Unit) {
    var cardName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardLimit by remember { mutableStateOf("") }
    var statementDate by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }

    val statementDay = statementDate.toIntOrNull()
    val dueDay = dueDate.toIntOrNull()
    val limit = cardLimit.toDoubleOrNull()
    val isValid = cardName.isNotBlank() && cardNumber.length == 4 && (limit ?: 0.0) > 0 &&
        statementDay in 1..31 && dueDay in 1..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add credit card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = cardName,
                    onValueChange = { cardName = it },
                    label = { Text("Card name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { input -> if (input.length <= 4 && input.all(Char::isDigit)) cardNumber = input },
                    label = { Text("Last 4 digits") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                AmountInputField(value = cardLimit, onValueChange = { cardLimit = it }, label = "Credit limit")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayField("Statement day", statementDate, { statementDate = it }, Modifier.weight(1f))
                    DayField("Due day", dueDate, { dueDate = it }, Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        CreditCard(
                            cardName = cardName.trim(),
                            cardNumber = cardNumber,
                            cardLimit = limit ?: 0.0,
                            statementDate = statementDay ?: 1,
                            dueDate = dueDay ?: 1
                        )
                    )
                },
                enabled = isValid
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DayField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val day = value.toIntOrNull()
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.length <= 2 && input.all(Char::isDigit)) onValueChange(input) },
        label = { Text(label) },
        singleLine = true,
        isError = value.isNotEmpty() && day !in 1..31,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
fun AddTransactionDialog(card: CreditCard, onDismiss: () -> Unit, onConfirm: (Double, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val value = amount.toDoubleOrNull()
    val available = (card.cardLimit - card.currentBalance).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Purchase on ${card.cardName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountInputField(
                    value = amount,
                    onValueChange = { amount = it },
                    supportingText = if (value != null && value > available) "Exceeds available credit (${CurrencyManager.format(available)})"
                    else "Available: ${CurrencyManager.format(available)}"
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { value?.let { onConfirm(it, description.trim()) } }, enabled = (value ?: 0.0) > 0) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PayBillDialog(card: CreditCard, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    val value = amount.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay ${card.cardName} bill") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Outstanding: ${CurrencyManager.format(card.currentBalance)}", style = MaterialTheme.typography.bodyMedium)
                AmountInputField(
                    value = amount,
                    onValueChange = { amount = it },
                    errorText = if (value != null && value > card.currentBalance) "More than the outstanding balance" else null
                )
                AssistChip(
                    onClick = { amount = String.format(Locale.US, "%.2f", card.currentBalance) },
                    label = { Text("Pay full balance") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { value?.let(onConfirm) },
                enabled = value != null && value > 0 && value <= card.currentBalance
            ) { Text("Pay") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
