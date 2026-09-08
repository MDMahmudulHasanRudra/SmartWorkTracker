package com.rudra.smartworktracker.ui.screens.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.smartworktracker.data.entity.Account
import com.rudra.smartworktracker.data.entity.AccountCategory
import com.rudra.smartworktracker.data.entity.AccountProvider
import com.rudra.smartworktracker.data.entity.displayName
import com.rudra.smartworktracker.data.entity.icon
import com.rudra.smartworktracker.utils.CurrencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (String, AccountCategory, AccountProvider, String, String?, Double, Double?, Boolean, Double?) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(AccountCategory.WALLET) }
    var selectedProvider by remember { mutableStateOf(AccountProvider.CASH) }
    var accountNumber by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var initialBalance by remember { mutableStateOf("0") }
    var maxBalance by remember { mutableStateOf("") }
    var hasLimit by remember { mutableStateOf(false) }
    var dailyLimit by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }
    var showMaxBalanceField by remember { mutableStateOf(false) }

    val providersForCategory = when (selectedCategory) {
        AccountCategory.WALLET -> listOf(AccountProvider.CASH)
        AccountCategory.BANK -> listOf(AccountProvider.BANK, AccountProvider.SAVINGS, AccountProvider.CREDIT_CARD, AccountProvider.LOAN, AccountProvider.DBBL, AccountProvider.CITY_BANK, AccountProvider.BRAC_BANK, AccountProvider.BKB, AccountProvider.SONALI_BANK)
        AccountCategory.MOBILE_BANKING -> listOf(AccountProvider.BKASH, AccountProvider.NAGAD, AccountProvider.ROCKET, AccountProvider.UCASH)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("➕ Add New Account", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Account Type", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName(),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        AccountCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.displayName()) },
                                onClick = {
                                    selectedCategory = category
                                    selectedProvider = when (category) {
                                        AccountCategory.WALLET -> AccountProvider.CASH
                                        AccountCategory.BANK -> AccountProvider.BANK
                                        AccountCategory.MOBILE_BANKING -> AccountProvider.BKASH
                                    }
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Provider", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = !providerExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProvider.displayName(),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false }
                    ) {
                        providersForCategory.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.displayName()) },
                                onClick = {
                                    selectedProvider = provider
                                    providerExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Account Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) initialBalance = it },
                    label = { Text("Initial Balance") },
                    prefix = { Text("${CurrencyManager.symbol()} ") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Set Max Balance", style = MaterialTheme.typography.labelMedium)
                        Text("Show progress bar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = showMaxBalanceField,
                        onCheckedChange = { showMaxBalanceField = it }
                    )
                }

                if (showMaxBalanceField) {
                    OutlinedTextField(
                        value = maxBalance,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) maxBalance = it },
                        label = { Text("Max Balance (for progress bar)") },
                        prefix = { Text("${CurrencyManager.symbol()} ") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("e.g., 50000") }
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Daily Transfer Limit", style = MaterialTheme.typography.labelMedium)
                        Text("Set a limit for daily transfers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = hasLimit,
                        onCheckedChange = { hasLimit = it }
                    )
                }

                if (hasLimit) {
                    OutlinedTextField(
                        value = dailyLimit,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) dailyLimit = it },
                        label = { Text("Limit Amount") },
                        prefix = { Text("${CurrencyManager.symbol()} ") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("e.g., 25000") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val balance = initialBalance.toDoubleOrNull() ?: 0.0
                    val max = if (showMaxBalanceField && maxBalance.isNotBlank()) maxBalance.toDoubleOrNull() else null
                    val limit = if (hasLimit && dailyLimit.isNotBlank()) dailyLimit.toDoubleOrNull() else null
                    onConfirm(
                        selectedProvider.displayName(),
                        selectedCategory,
                        selectedProvider,
                        accountNumber,
                        nickname.ifEmpty { null },
                        balance,
                        max,
                        hasLimit,
                        limit
                    )
                },
                enabled = accountNumber.isNotBlank() && (!hasLimit || dailyLimit.isNotBlank())
            ) {
                Text("Add Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: (Account) -> Unit
) {
    var nickname by remember { mutableStateOf(account.nickname ?: "") }
    var accountNumber by remember { mutableStateOf(account.accountNumber) }
    var balance by remember { mutableStateOf(account.balance.toString()) }
    var maxBalance by remember { mutableStateOf(account.maxBalance?.toString() ?: "") }
    var hasLimit by remember { mutableStateOf(account.hasLimit) }
    var dailyLimit by remember { mutableStateOf(account.dailyTransferLimit?.toString() ?: "") }
    var showMaxBalance by remember { mutableStateOf(account.maxBalance != null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("✏️ Edit Account", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(account.provider.icon(), fontSize = 32.sp)
                        Column {
                            Text(account.name, fontWeight = FontWeight.Medium)
                            Text(account.type.displayName(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Account Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                OutlinedTextField(
                    value = balance,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) balance = it },
                    label = { Text("Current Balance") },
                    prefix = { Text("${CurrencyManager.symbol()} ") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Set Max Balance", style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (account.maxBalance != null) "Current: ${CurrencyManager.format(account.maxBalance)}" else "For progress bar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showMaxBalance,
                        onCheckedChange = { showMaxBalance = it }
                    )
                }

                if (showMaxBalance) {
                    OutlinedTextField(
                        value = maxBalance,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) maxBalance = it },
                        label = { Text("Max Balance") },
                        prefix = { Text("${CurrencyManager.symbol()} ") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("e.g., 50000") }
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Daily Transfer Limit", style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (account.getEffectiveLimit() != null) "Current: ${CurrencyManager.format(account.getEffectiveLimit()!!)}" else "No limit set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = hasLimit,
                        onCheckedChange = { hasLimit = it }
                    )
                }

                if (hasLimit) {
                    OutlinedTextField(
                        value = dailyLimit,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) dailyLimit = it },
                        label = { Text("Limit Amount") },
                        prefix = { Text("${CurrencyManager.symbol()} ") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("e.g., 25000") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newBalance = balance.toDoubleOrNull() ?: account.balance
                    val max = if (showMaxBalance && maxBalance.isNotBlank()) maxBalance.toDoubleOrNull() else null
                    val limit = if (hasLimit && dailyLimit.isNotBlank()) dailyLimit.toDoubleOrNull() else null
                    val updatedAccount = account.copy(
                        nickname = nickname.ifEmpty { null },
                        accountNumber = accountNumber,
                        balance = newBalance,
                        maxBalance = max,
                        hasLimit = hasLimit,
                        dailyTransferLimit = limit,
                        lastUpdated = System.currentTimeMillis()
                    )
                    onConfirm(updatedAccount)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountDialog(
    account: Account,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var selectedTargetAccount by remember { mutableStateOf<Account?>(null) }
    var targetExpanded by remember { mutableStateOf(false) }
    val canDeleteDirectly = account.balance == 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🗑️ Delete Account", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(account.provider.icon(), fontSize = 32.sp)
                        Column {
                            Text(account.nickname ?: account.name, fontWeight = FontWeight.Medium)
                            Text("Balance: ${CurrencyManager.format(account.balance)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (!canDeleteDirectly) {
                    Text(
                        text = "⚠️ This account has balance. Transfer to another account first:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    ExposedDropdownMenuBox(
                        expanded = targetExpanded,
                        onExpandedChange = { targetExpanded = !targetExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedTargetAccount?.nickname ?: selectedTargetAccount?.name ?: "Select target account",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = targetExpanded,
                            onDismissRequest = { targetExpanded = false }
                        ) {
                            accounts.forEach { targetAccount ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(targetAccount.nickname ?: targetAccount.name)
                                            Text("Balance: ${CurrencyManager.format(targetAccount.balance)}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                    onClick = {
                                        selectedTargetAccount = targetAccount
                                        targetExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "✅ This account has zero balance. You can delete it directly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canDeleteDirectly) {
                        onConfirm(-1)
                    } else {
                        selectedTargetAccount?.let { onConfirm(it.id) }
                    }
                },
                enabled = canDeleteDirectly || selectedTargetAccount != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
