package com.rudra.smartworktracker.ui.screens.transfer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.data.entity.Account
import com.rudra.smartworktracker.data.entity.AccountCategory
import com.rudra.smartworktracker.data.entity.displayName
import com.rudra.smartworktracker.utils.CurrencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val viewModel: TransferViewModel = viewModel(factory = TransferViewModelFactory(application))
    val accounts by viewModel.accounts.collectAsState()
    val transferState by viewModel.transferState.collectAsState()
    val error by viewModel.error.collectAsState()

    // Selections are kept as ids so balance updates from the database never reset them,
    // and the displayed balances always come from the latest account list.
    var fromAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var toAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
    var amount by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(accounts) {
        if (accounts.isEmpty()) return@LaunchedEffect
        if (fromAccountId == null || accounts.none { it.id == fromAccountId }) {
            fromAccountId = (accounts.find { it.provider.name == "CASH" } ?: accounts.first()).id
        }
        if (toAccountId == null || accounts.none { it.id == toAccountId } || toAccountId == fromAccountId) {
            toAccountId = accounts.firstOrNull { it.id != fromAccountId }?.id
        }
    }

    val fromAccount = accounts.find { it.id == fromAccountId }
    val toAccount = accounts.find { it.id == toAccountId }

    var showFromSelector by remember { mutableStateOf(false) }
    var showToSelector by remember { mutableStateOf(false) }

    val validationResult = viewModel.validateTransfer(fromAccount, toAccount, amount)
    val validationMessage = (validationResult as? ValidationResult.Error)?.message
    val isLoading = transferState is TransferState.Loading

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Transfer Money", fontWeight = FontWeight.Bold) })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (accounts.size < 2) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "You need at least two accounts to make a transfer. Add one from the Accounts screen.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            AccountSelectorCard(
                label = "From",
                selectedAccount = fromAccount,
                onClick = { showFromSelector = true }
            )

            // Swap source and destination
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FilledTonalIconButton(
                    onClick = {
                        val previousFrom = fromAccountId
                        fromAccountId = toAccountId
                        toAccountId = previousFrom
                    },
                    enabled = fromAccount != null && toAccount != null
                ) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Swap accounts")
                }
            }

            AccountSelectorCard(
                label = "To",
                selectedAccount = toAccount,
                onClick = { showToSelector = true }
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amount = input
                        if (error != null) viewModel.resetState()
                    }
                },
                label = { Text("Amount") },
                prefix = { Text(CurrencyManager.symbol()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = amount.isNotEmpty() && validationMessage != null,
                supportingText = {
                    when {
                        amount.isNotEmpty() && validationMessage != null -> Text(validationMessage)
                        fromAccount != null -> Text("Available: ${CurrencyManager.format(fromAccount.balance)}")
                    }
                }
            )

            // Quick amount presets
            fromAccount?.let { account ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(500.0, 1000.0, 5000.0).forEach { preset ->
                        SuggestionChip(
                            onClick = { amount = preset.toLong().toString() },
                            label = { Text(CurrencyManager.formatWhole(preset)) },
                            enabled = account.balance >= preset
                        )
                    }
                    SuggestionChip(
                        onClick = { amount = String.format(java.util.Locale.US, "%.2f", account.balance).trimEnd('0').trimEnd('.') },
                        label = { Text("All") },
                        enabled = account.balance > 0
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            error?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text(text = message, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val from = fromAccount ?: return@Button
                    val to = toAccount ?: return@Button
                    val value = amount.toDoubleOrNull() ?: return@Button
                    viewModel.makeTransfer(from, to, value, notes.trim().ifEmpty { null })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = validationResult is ValidationResult.Valid && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = amount.toDoubleOrNull()?.let { "Transfer ${CurrencyManager.format(it)}" } ?: "Transfer",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (showFromSelector) {
            AccountSelectionSheet(
                title = "Transfer from",
                accounts = accounts.filter { it.id != toAccountId },
                onSelect = {
                    fromAccountId = it.id
                    showFromSelector = false
                },
                onDismiss = { showFromSelector = false }
            )
        }

        if (showToSelector) {
            AccountSelectionSheet(
                title = "Transfer to",
                accounts = accounts.filter { it.id != fromAccountId },
                onSelect = {
                    toAccountId = it.id
                    showToSelector = false
                },
                onDismiss = { showToSelector = false }
            )
        }

        (transferState as? TransferState.Success)?.let { state ->
            TransferSuccessDialog(
                amount = state.amount,
                fromAccount = state.fromAccount,
                toAccount = state.toAccount,
                onDismiss = {
                    amount = ""
                    notes = ""
                    viewModel.resetState()
                }
            )
        }
    }
}

private fun Account.label(): String = nickname?.takeIf { it.isNotBlank() } ?: name

private fun Account.categoryIcon(): ImageVector = when (type) {
    AccountCategory.WALLET -> Icons.Default.AccountBalanceWallet
    AccountCategory.BANK -> Icons.Default.AccountBalance
    AccountCategory.MOBILE_BANKING -> Icons.Default.PhoneAndroid
}

@Composable
fun AccountSelectorCard(
    label: String,
    selectedAccount: Account?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = selectedAccount?.categoryIcon() ?: Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = selectedAccount?.label() ?: "Select account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedAccount != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                selectedAccount?.let {
                    Text(
                        text = "Balance: ${CurrencyManager.format(it.balance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Choose $label account",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelectionSheet(
    title: String,
    accounts: List<Account>,
    onSelect: (Account) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (accounts.isEmpty()) {
                Text(
                    "No other accounts available",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(accounts, key = { it.id }) { account ->
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(account) },
                        leadingContent = {
                            Icon(account.categoryIcon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        headlineContent = {
                            Text(account.label(), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = {
                            val limit = account.getEffectiveLimit()
                            Text(
                                if (limit != null) "${account.provider.displayName()} • limit ${CurrencyManager.formatWhole(limit)}/day"
                                else account.provider.displayName()
                            )
                        },
                        trailingContent = {
                            Text(
                                text = CurrencyManager.format(account.balance),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    )
                }
            }
        }
    }
}

@Composable
fun TransferSuccessDialog(
    amount: Double,
    fromAccount: Account,
    toAccount: Account,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Transfer successful") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${CurrencyManager.format(amount)} moved from ${fromAccount.label()} to ${toAccount.label()}.",
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider()
                Text(text = "New balances", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${fromAccount.label()}: ${CurrencyManager.format(fromAccount.balance)} (was ${CurrencyManager.format(fromAccount.balance + amount)})",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${toAccount.label()}: ${CurrencyManager.format(toAccount.balance)} (was ${CurrencyManager.format(toAccount.balance - amount)})",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
