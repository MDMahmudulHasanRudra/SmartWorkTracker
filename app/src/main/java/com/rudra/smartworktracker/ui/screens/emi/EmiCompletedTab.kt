package com.rudra.smartworktracker.ui.screens.emi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rudra.smartworktracker.data.entity.AccountType
import com.rudra.smartworktracker.data.entity.Emi
import com.rudra.smartworktracker.data.entity.Loan
import com.rudra.smartworktracker.utils.CurrencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmiBottomSheet(
    loans: List<Loan>,
    onDismiss: () -> Unit,
    onSave: (Int, Double, Double, Double, Int, String?, AccountType) -> Unit
) {
    var selectedLoan by remember { mutableStateOf<Loan?>(null) }
    var amount by remember { mutableStateOf("") }
    var principalAmount by remember { mutableStateOf("") }
    var interestAmount by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("5") }
    var notes by remember { mutableStateOf("") }
    var paymentAccount by remember { mutableStateOf(AccountType.BANK) }

    var isLoansExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Add New EMI",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (loans.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "No active loans found. Please add a loan first.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                ExposedDropdownMenuBox(
                    expanded = isLoansExpanded,
                    onExpandedChange = { isLoansExpanded = !isLoansExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedLoan?.personName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Loan *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isLoansExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isLoansExpanded,
                        onDismissRequest = { isLoansExpanded = false }
                    ) {
                        loans.forEach { loan ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(loan.personName)
                                        Text(
                                            "Remaining: ${loan.remainingAmount}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                },
                                onClick = {
                                    selectedLoan = loan
                                    amount = loan.emiAmount?.toString() ?: ""
                                    principalAmount = loan.emiAmount?.toString() ?: ""
                                    isLoansExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    val total = it.toDoubleOrNull() ?: 0.0
                    val interest = interestAmount.toDoubleOrNull() ?: 0.0
                    principalAmount = (total - interest).coerceAtLeast(0.0).toString()
                },
                label = { Text("Total EMI Amount *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = principalAmount,
                    onValueChange = { principalAmount = it },
                    label = { Text("Principal *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = interestAmount,
                    onValueChange = {
                        interestAmount = it
                        val total = amount.toDoubleOrNull() ?: 0.0
                        val interest = it.toDoubleOrNull() ?: 0.0
                        principalAmount = (total - interest).coerceAtLeast(0.0).toString()
                    },
                    label = { Text("Interest") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = dueDay,
                onValueChange = {
                    val day = it.toIntOrNull()
                    if (day == null || (day in 1..31)) dueDay = it
                },
                label = { Text("Due Day of Month (1-31) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = !accountExpanded }
            ) {
                OutlinedTextField(
                    value = paymentAccount.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false }
                ) {
                    AccountType.entries.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                paymentAccount = account
                                accountExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val loanId = selectedLoan?.id ?: return@Button
                        val emiAmountVal = amount.toDoubleOrNull() ?: 0.0
                        val principal = principalAmount.toDoubleOrNull() ?: emiAmountVal
                        val interest = interestAmount.toDoubleOrNull() ?: 0.0
                        val day = dueDay.toIntOrNull() ?: 5

                        if (loanId > 0 && emiAmountVal > 0 && day in 1..31) {
                            onSave(
                                loanId,
                                emiAmountVal,
                                principal,
                                interest,
                                day,
                                notes.takeIf { it.isNotBlank() },
                                paymentAccount
                            )
                        }
                    },
                    enabled = selectedLoan != null &&
                              (amount.toDoubleOrNull() ?: 0.0) > 0 &&
                              (dueDay.toIntOrNull() ?: 0) in 1..31
                ) {
                    Text("Add EMI")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PayEmiDialog(emiWithLoan: EmiWithLoan, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val emi = emiWithLoan.emi
    val loan = emiWithLoan.loan

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Payment") },
        text = {
            Column {
                loan?.let {
                    Text("Loan: ${it.personName}", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("EMI Amount: ${CurrencyManager.format(emi.amount)}")
                Text("Principal: ${CurrencyManager.format(emi.principalAmount)}")
                if (emi.interestAmount > 0) {
                    Text("Interest: ${CurrencyManager.format(emi.interestAmount)}")
                }
                if (emi.penaltyAmount > 0) {
                    Text("Penalty: ${CurrencyManager.format(emi.penaltyAmount)}", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Total Payable: ${CurrencyManager.format(emi.totalPayable)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(emiWithLoan: EmiWithLoan, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val loan = emiWithLoan.loan

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete EMI") },
        text = {
            Text("Are you sure you want to delete this EMI? ${loan?.let { "Loan: ${it.personName}" } ?: ""}")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

@Composable
fun EmiOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        content()
    }
}
