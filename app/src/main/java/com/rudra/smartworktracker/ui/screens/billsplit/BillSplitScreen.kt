package com.rudra.smartworktracker.ui.screens.billsplit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.rudra.smartworktracker.data.entity.BillSplit
import com.rudra.smartworktracker.ui.components.AmountInputField
import com.rudra.smartworktracker.ui.components.ConfirmDeleteDialog
import com.rudra.smartworktracker.ui.components.EmptyStateCard
import com.rudra.smartworktracker.ui.components.sanitizeAmountInput
import com.rudra.smartworktracker.utils.CurrencyManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.round

/** Splits [total] into [count] parts rounded to cents that add up exactly to the total. */
internal fun equalShares(total: Double, count: Int): List<Double> {
    if (count <= 0) return emptyList()
    val totalCents = round(total * 100).toLong()
    val base = totalCents / count
    val remainder = (totalCents - base * count).toInt()
    return List(count) { index -> (base + if (index < remainder) 1 else 0) / 100.0 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitScreen(
    onNavigateBack: () -> Unit,
    billSplits: List<BillSplit>,
    onAddBillSplit: (BillSplit) -> Unit,
    onMarkSettled: (BillSplit) -> Unit,
    onDelete: (BillSplit) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BillSplit?>(null) }
    val outstanding = billSplits.filter { !it.isSettled }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bill Split", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Split a bill") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (billSplits.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Receipt,
                        title = "No bill splits yet",
                        message = "Split a dinner, trip or rent with friends and track who has settled.",
                        actionText = "Split a bill",
                        onAction = { showAddDialog = true }
                    )
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Still to settle", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    CurrencyManager.format(outstanding.sumOf { it.totalAmount }),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                "${outstanding.size} open • ${billSplits.size - outstanding.size} settled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                // Open bills first, newest first within each group
                items(billSplits.sortedWith(compareBy<BillSplit> { it.isSettled }.thenByDescending { it.createdAt }), key = { it.id }) { billSplit ->
                    BillSplitItem(
                        billSplit = billSplit,
                        onMarkSettled = { onMarkSettled(billSplit) },
                        onDelete = { pendingDelete = billSplit }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddBillSplitDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { billSplit ->
                onAddBillSplit(billSplit)
                showAddDialog = false
            }
        )
    }

    pendingDelete?.let { bill ->
        ConfirmDeleteDialog(
            title = "Delete \"${bill.transactionName}\"?",
            message = "This bill split will be removed permanently.",
            onConfirm = {
                onDelete(bill)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillSplitItem(
    billSplit: BillSplit,
    onMarkSettled: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
    val settledColor = Color(0xFF10B981)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (billSplit.isSettled) MaterialTheme.colorScheme.surfaceContainerLowest
            else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        billSplit.transactionName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (billSplit.isSettled) TextDecoration.LineThrough else null
                    )
                    Text(
                        Instant.ofEpochMilli(billSplit.createdAt).atZone(ZoneId.systemDefault()).format(dateFormatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    CurrencyManager.format(billSplit.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (billSplit.isSettled) settledColor else MaterialTheme.colorScheme.primary
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                billSplit.participants.forEachIndexed { index, name ->
                    val share = billSplit.amounts.getOrNull(index)
                    AssistChip(
                        onClick = {},
                        label = { Text(if (share != null) "$name • ${CurrencyManager.format(share)}" else name) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (billSplit.isSettled) {
                    TextButton(onClick = onMarkSettled) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reopen")
                    }
                } else {
                    FilledTonalButton(onClick = onMarkSettled) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mark settled")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddBillSplitDialog(
    onDismiss: () -> Unit,
    onAdd: (BillSplit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    val participants = remember { mutableStateListOf<String>() }
    val customShares = remember { mutableStateMapOf<Int, String>() }
    var currentParticipant by remember { mutableStateOf("") }
    var splitEqually by remember { mutableStateOf(true) }

    val total = totalAmount.toDoubleOrNull() ?: 0.0
    val shares: List<Double> = if (splitEqually) {
        equalShares(total, participants.size)
    } else {
        participants.indices.map { customShares[it]?.toDoubleOrNull() ?: 0.0 }
    }
    val sharesSum = shares.sum()
    val sharesMatch = kotlin.math.abs(sharesSum - total) < 0.01
    val canSave = name.isNotBlank() && total > 0 && participants.isNotEmpty() && sharesMatch

    fun addParticipant() {
        // Commas are the storage separator for participant lists
        val cleaned = currentParticipant.replace(",", " ").trim()
        if (cleaned.isNotEmpty() && participants.none { it.equals(cleaned, ignoreCase = true) }) {
            participants.add(cleaned)
        }
        currentParticipant = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split a bill") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("What was it for?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                AmountInputField(value = totalAmount, onValueChange = { totalAmount = it }, label = "Total amount")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = currentParticipant,
                        onValueChange = { currentParticipant = it },
                        label = { Text("Add person") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addParticipant() }),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { addParticipant() }, enabled = currentParticipant.isNotBlank()) {
                        Icon(Icons.Default.Add, contentDescription = "Add participant")
                    }
                }

                if (participants.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Split equally", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = splitEqually, onCheckedChange = { splitEqually = it })
                    }
                    participants.forEachIndexed { index, participant ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(28.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(participant.take(1).uppercase(), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            Text(participant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            if (splitEqually) {
                                Text(CurrencyManager.format(shares.getOrElse(index) { 0.0 }), style = MaterialTheme.typography.bodyMedium)
                            } else {
                                OutlinedTextField(
                                    value = customShares[index] ?: "",
                                    onValueChange = { input -> sanitizeAmountInput(input)?.let { customShares[index] = it } },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(96.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    participants.removeAt(index)
                                    // Re-key custom shares after removal
                                    val remaining = participants.indices.associateWith { i -> customShares[if (i >= index) i + 1 else i] ?: "" }
                                    customShares.clear()
                                    customShares.putAll(remaining)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove $participant", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (!splitEqually && !sharesMatch && total > 0) {
                        Text(
                            "Shares add up to ${CurrencyManager.format(sharesSum)} of ${CurrencyManager.format(total)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(
                        BillSplit(
                            transactionName = name.trim(),
                            totalAmount = total,
                            participants = participants.toList(),
                            amounts = shares
                        )
                    )
                },
                enabled = canSave
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
