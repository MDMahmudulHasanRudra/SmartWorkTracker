package com.rudra.smartworktracker.ui.screens.creditcard

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.data.entity.CreditCard
import com.rudra.smartworktracker.ui.components.EmptyStateCard
import com.rudra.smartworktracker.utils.CurrencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardScreen() {
    val context = LocalContext.current
    val viewModel: CreditCardViewModel = viewModel(factory = CreditCardViewModelFactory(context.applicationContext as Application))
    val creditCards by viewModel.creditCards.collectAsState()
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf<CreditCard?>(null) }
    var showPayBillDialog by remember { mutableStateOf<CreditCard?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Credit Card Management") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddCardDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Credit Card")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            if (showAddCardDialog) {
                AddCreditCardDialog(
                    onDismiss = { showAddCardDialog = false },
                    onConfirm = {
                        viewModel.addCreditCard(it)
                        showAddCardDialog = false
                    }
                )
            }
            showAddTransactionDialog?.let { card ->
                AddTransactionDialog(
                    card = card,
                    onDismiss = { showAddTransactionDialog = null },
                    onConfirm = { amount, description ->
                        viewModel.addCardTransaction(card, amount, description)
                        showAddTransactionDialog = null
                    }
                )
            }
            showPayBillDialog?.let { card ->
                PayBillDialog(
                    card = card,
                    onDismiss = { showPayBillDialog = null },
                    onConfirm = { amount ->
                        viewModel.payCreditCardBill(card, amount)
                        showPayBillDialog = null
                    }
                )
            }

            LazyColumn {
                if (creditCards.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Add,
                            title = "No credit cards",
                            message = "Tap + to add your first credit card for tracking."
                        )
                    }
                } else {
                    items(creditCards) { card ->
                        CreditCardItem(
                            card = card,
                            onAddTransactionClick = { showAddTransactionDialog = card },
                            onPayBillClick = { showPayBillDialog = card }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreditCardItem(card: CreditCard, onAddTransactionClick: () -> Unit, onPayBillClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${card.cardName} - **** ${card.cardNumber}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Limit: ${CurrencyManager.format(card.cardLimit)}")
            Text("Balance: ${CurrencyManager.format(card.currentBalance)}")
            Text("Statement Date: ${card.statementDate}th")
            Text("Due Date: ${card.dueDate}th")
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = onAddTransactionClick) { Text("Add Transaction") }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onPayBillClick) { Text("Pay Bill") }
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Add New Credit Card", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = cardName, onValueChange = { cardName = it }, label = { Text("Card Name") })
                OutlinedTextField(value = cardNumber, onValueChange = { cardNumber = it }, label = { Text("Card Number (Last 4 Digits)") })
                OutlinedTextField(value = cardLimit, onValueChange = { cardLimit = it }, label = { Text("Card Limit") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = statementDate, onValueChange = { statementDate = it }, label = { Text("Statement Date (Day of Month)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Due Date (Day of Month)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Button(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = {
                        onConfirm(
                            CreditCard(
                                cardName = cardName,
                                cardNumber = cardNumber,
                                cardLimit = cardLimit.toDoubleOrNull() ?: 0.0,
                                statementDate = statementDate.toIntOrNull() ?: 0,
                                dueDate = dueDate.toIntOrNull() ?: 0
                            )
                        )
                    }) { Text("Add") }
                }
            }
        }
    }
}

@Composable
fun AddTransactionDialog(card: CreditCard, onDismiss: () -> Unit, onConfirm: (Double, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Add Transaction to ${card.cardName}", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0, description) }) { Text("Add") }
                }
            }
        }
    }
}

@Composable
fun PayBillDialog(card: CreditCard, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Pay Bill for ${card.cardName}", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0) }) { Text("Pay") }
                }
            }
        }
    }
}
