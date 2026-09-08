package com.rudra.smartworktracker.ui.screens.recurring

import java.util.Calendar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.rudra.smartworktracker.data.entity.RecurringRule
import com.rudra.smartworktracker.data.entity.RecurringTransaction
import com.rudra.smartworktracker.data.entity.RecurringTransactionStatus
import com.rudra.smartworktracker.data.entity.TransactionType
import com.rudra.smartworktracker.ui.components.EmptyStateCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesTab(
    rules: List<RecurringRule>,
    searchQuery: String,
    selectedFilter: RuleFilter,
    isRefreshing: Boolean,
    isMultiSelectMode: Boolean,
    selectedRuleIds: Set<Long>,
    accounts: List<com.rudra.smartworktracker.data.entity.Account> = emptyList(),
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (RuleFilter) -> Unit,
    onToggleRule: (RecurringRule) -> Unit,
    onEditRule: (RecurringRule) -> Unit,
    onDeleteRule: (RecurringRule) -> Unit,
    onExecuteNow: (RecurringRule) -> Unit,
    onRefresh: () -> Unit,
    onToggleMultiSelect: () -> Unit,
    onToggleRuleSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onToggleSelected: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search rules...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            IconButton(onClick = onToggleMultiSelect) {
                Icon(
                    if (isMultiSelectMode) Icons.Default.Close else Icons.Default.Edit,
                    contentDescription = if (isMultiSelectMode) "Exit multi-select" else "Multi-select"
                )
            }
        }

        AnimatedVisibility(visible = isMultiSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${selectedRuleIds.size} selected",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onSelectAll) { Text("All") }
                    TextButton(onClick = onDeselectAll) { Text("None") }
                    IconButton(onClick = onToggleSelected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Toggle active", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDeleteSelected) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(RuleFilter.values()) { filter ->
                FilterChip(
                    label = when (filter) {
                        RuleFilter.ALL -> "All"
                        RuleFilter.ACTIVE -> "Active"
                        RuleFilter.INACTIVE -> "Inactive"
                        RuleFilter.INCOME -> "Income"
                        RuleFilter.EXPENSE -> "Expense"
                        RuleFilter.SAVINGS -> "Savings"
                        RuleFilter.TRANSFER -> "Transfer"
                    },
                    isSelected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (rules.isEmpty() && isRefreshing) {
            LoadingSkeleton()
        } else if (rules.isEmpty() && !isRefreshing) {
            EmptyStateCard(
                icon = Icons.Default.Repeat,
                title = "No rules found",
                message = "Tap + to add your first recurring rule"
            )
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rules, key = { it.id }) { rule ->
                        if (isMultiSelectMode) {
                            RuleCard(
                                rule = rule,
                                accounts = accounts,
                                onToggle = { onToggleRule(rule) },
                                onEdit = { onEditRule(rule) },
                                onDelete = { onDeleteRule(rule) },
                                onExecuteNow = { onExecuteNow(rule) },
                                isSelected = selectedRuleIds.contains(rule.id),
                                isMultiSelectMode = true,
                                onToggleSelection = { onToggleRuleSelection(rule.id) }
                            )
                        } else {
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        onDeleteRule(rule)
                                        true
                                    } else false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                },
                                enableDismissFromStartToEnd = false
                            ) {
                                RuleCard(
                                    rule = rule,
                                    accounts = accounts,
                                    onToggle = { onToggleRule(rule) },
                                    onEdit = { onEditRule(rule) },
                                    onDelete = { onDeleteRule(rule) },
                                    onExecuteNow = { onExecuteNow(rule) }
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
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .semantics {
                contentDescription = "$label filter, ${if (isSelected) "selected" else "not selected"}"
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun RuleCard(
    rule: RecurringRule,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExecuteNow: () -> Unit,
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false,
    onToggleSelection: () -> Unit = {},
    accounts: List<com.rudra.smartworktracker.data.entity.Account> = emptyList()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val expandState = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isMultiSelectMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelection() },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(getTransactionTypeColor(rule.transactionType)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getTransactionTypeIcon(rule.transactionType),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rule.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = getFrequencyText(rule.frequency),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Switch(
                    checked = rule.isActive,
                    onCheckedChange = { onToggle() }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "$${String.format("%.2f", rule.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (rule.transactionType == TransactionType.INCOME) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Next: ${dateFormat.format(Date(rule.nextExecutionDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PriorityBadge(priority = rule.priority)

                Row {
                    TooltipWrapper(tooltipText = "Expand details") {
                        IconButton(onClick = { expandState.value = !expandState.value }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (expandState.value) Icons.Default.Close else Icons.Default.Edit,
                                contentDescription = "Details",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    TooltipWrapper(tooltipText = "Execute now") {
                        IconButton(onClick = onExecuteNow, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Send, contentDescription = "Execute", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    TooltipWrapper(tooltipText = "Edit rule") {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    TooltipWrapper(tooltipText = "Delete rule") {
                        IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = expandState.value,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider()
                    DetailRow("Category", rule.category ?: "Not set")
                    val accountName = rule.accountId?.let { id -> accounts.find { it.id == id }?.name }
                    DetailRow("Source", accountName ?: rule.sourceAccount.name)
                    DetailRow("Auto Execute", if (rule.autoExecute) "Yes" else "No")
                    if (rule.minimumBalanceRequired != null) {
                        DetailRow("Min Balance", "$${String.format("%.2f", rule.minimumBalanceRequired)}")
                    }
                    DetailRow("Created", dateFormat.format(Date(rule.createdAt)))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Rule") },
            text = { Text("Are you sure you want to delete '${rule.name}'?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PriorityBadge(priority: com.rudra.smartworktracker.data.entity.RecurringPriority) {
    val (color, text) = when (priority) {
        com.rudra.smartworktracker.data.entity.RecurringPriority.CRITICAL -> Pair(Color(0xFFD32F2F), "CRITICAL")
        com.rudra.smartworktracker.data.entity.RecurringPriority.HIGH -> Pair(Color(0xFFF57C00), "HIGH")
        com.rudra.smartworktracker.data.entity.RecurringPriority.MEDIUM -> Pair(Color(0xFF1976D2), "MEDIUM")
        com.rudra.smartworktracker.data.entity.RecurringPriority.LOW -> Pair(Color(0xFF388E3C), "LOW")
        com.rudra.smartworktracker.data.entity.RecurringPriority.OPTIONAL -> Pair(Color(0xFF757575), "OPTIONAL")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .semantics { contentDescription = "Priority: $text" }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
fun TransactionsTab(
    transactions: List<RecurringTransaction>,
    isRefreshing: Boolean,
    pendingConfirmations: List<RecurringTransaction>,
    onSkipTransaction: (RecurringTransaction) -> Unit,
    onConfirmTransaction: (RecurringTransaction) -> Unit,
    onConfirmAllPending: () -> Unit,
    onSnoozeTransaction: (RecurringTransaction) -> Unit,
    onSnoozeAllFailed: () -> Unit,
    onRefresh: () -> Unit
) {
    val failedCount = transactions.count { it.status == RecurringTransactionStatus.FAILED }

    Column(modifier = Modifier.fillMaxSize()) {
        if (pendingConfirmations.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${pendingConfirmations.size} pending confirmation",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Button(onClick = onConfirmAllPending, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("Confirm All", fontSize = 12.sp)
                    }
                }
            }
        }

        if (failedCount > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$failedCount failed transaction(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(onClick = onSnoozeAllFailed, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("Retry Tomorrow", fontSize = 12.sp)
                    }
                }
            }
        }

        if (transactions.isEmpty() && !isRefreshing) {
            EmptyStateCard(
                icon = Icons.Default.Schedule,
                title = "No transactions yet",
                message = "Transactions will appear here when rules execute"
            )
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onSkip = { onSkipTransaction(transaction) },
                            onConfirm = { onConfirmTransaction(transaction) },
                            onSnooze = { onSnoozeTransaction(transaction) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: RecurringTransaction,
    onSkip: () -> Unit,
    onConfirm: () -> Unit,
    onSnooze: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateFormat.format(Date(transaction.scheduledDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                StatusBadge(status = transaction.status)
            }

            Text(
                text = "$${String.format("%.2f", transaction.amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (transaction.transactionType == TransactionType.INCOME) Color(0xFF4CAF50) else Color(0xFFFF5252)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (transaction.status == RecurringTransactionStatus.PENDING && !transaction.isConfirmed) {
                    IconButton(onClick = onConfirm, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Confirm", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    }
                }
                if (transaction.status == RecurringTransactionStatus.FAILED) {
                    IconButton(onClick = onSnooze, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Schedule, contentDescription = "Snooze 1 day", tint = Color(0xFFF57C00), modifier = Modifier.size(16.dp))
                    }
                }
                if (transaction.status == RecurringTransactionStatus.PENDING || transaction.status == RecurringTransactionStatus.CONFIRMED) {
                    IconButton(onClick = onSkip, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Skip", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: RecurringTransactionStatus) {
    val (color, text) = when (status) {
        RecurringTransactionStatus.PENDING -> Pair(Color(0xFF1976D2), "Pending")
        RecurringTransactionStatus.CONFIRMED -> Pair(Color(0xFF388E3C), "Confirmed")
        RecurringTransactionStatus.EXECUTING -> Pair(Color(0xFFF57C00), "Executing")
        RecurringTransactionStatus.EXECUTED -> Pair(Color(0xFF4CAF50), "Executed")
        RecurringTransactionStatus.FAILED -> Pair(Color(0xFFD32F2F), "Failed")
        RecurringTransactionStatus.SKIPPED -> Pair(Color(0xFF757575), "Skipped")
        RecurringTransactionStatus.CANCELLED -> Pair(Color(0xFF616161), "Cancelled")
    }

    Box(
        modifier = Modifier
            .padding(top = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 9.sp
        )
    }
}

@Composable
fun CalendarTab(
    rules: List<RecurringRule>,
    transactions: List<RecurringTransaction>
) {
    val calendar = remember { mutableStateOf(Calendar.getInstance()) }
    val dateFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("dd", Locale.getDefault()) }

    val year = calendar.value.get(Calendar.YEAR)
    val month = calendar.value.get(Calendar.MONTH)
    val daysInMonth = calendar.value.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.value.apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)

    val transactionsByDay = remember(transactions, year, month) {
        transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.scheduledDate }
            txCal.get(Calendar.YEAR) == year && txCal.get(Calendar.MONTH) == month
        }.groupBy { tx ->
            Calendar.getInstance().apply { timeInMillis = tx.scheduledDate }.get(Calendar.DAY_OF_MONTH)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                calendar.value = (calendar.value.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            }) {
                Text("<", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = dateFormat.format(calendar.value.time),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                calendar.value = (calendar.value.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            }) {
                Text(">", style = MaterialTheme.typography.titleLarge)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(day, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val totalCells = firstDayOfWeek - 1 + daysInMonth
        val rows = (totalCells + 6) / 7

        Column {
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (col in 0 until 7) {
                        val dayIndex = row * 7 + col
                        val day = dayIndex - (firstDayOfWeek - 1) + 1
                        if (day in 1..daysInMonth) {
                            val dayTransactions = transactionsByDay[day] ?: emptyList()
                            val hasTransactions = dayTransactions.isNotEmpty()
                            val isToday = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == day &&
                                    Calendar.getInstance().get(Calendar.MONTH) == month &&
                                    Calendar.getInstance().get(Calendar.YEAR) == year

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            hasTransactions -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isToday || hasTransactions) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (hasTransactions) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                            dayTransactions.take(3).forEach { tx ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (tx.transactionType == TransactionType.INCOME) Color(0xFF4CAF50) else Color(0xFFFF5252)
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val selectedDayTransactions = transactionsByDay[Calendar.getInstance().get(Calendar.DAY_OF_MONTH)]?.takeIf {
            Calendar.getInstance().get(Calendar.MONTH) == month && Calendar.getInstance().get(Calendar.YEAR) == year
        } ?: emptyList()

        if (selectedDayTransactions.isNotEmpty()) {
            Text("Today's Transactions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            selectedDayTransactions.forEach { tx ->
                CalendarTransactionItem(transaction = tx)
            }
        } else if (transactions.isNotEmpty()) {
            Text("Upcoming", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            transactions.take(5).forEach { tx ->
                CalendarTransactionItem(transaction = tx)
            }
        }
    }
}

@Composable
fun CalendarTransactionItem(transaction: RecurringTransaction) {
    val dateFormat = remember { SimpleDateFormat("EEE, MMM dd", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                Text(
                    text = dateFormat.format(Date(transaction.scheduledDate)),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(32.dp)
                    .background(
                        if (transaction.transactionType == TransactionType.INCOME) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$${String.format("%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (transaction.transactionType == TransactionType.INCOME) Color(0xFF4CAF50) else Color(0xFFFF5252),
                    fontSize = 11.sp
                )
            }

            StatusBadge(status = transaction.status)
        }
    }
}

@Composable
fun LoadingSkeleton(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha.value))
                        )
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha.value))
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha.value))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha.value))
                        )
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha.value))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TooltipWrapper(
    tooltipText: String,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = if (enabled) Modifier.clickable { showTooltip = !showTooltip } else Modifier
        ) {
            content()
        }

        AnimatedVisibility(
            visible = showTooltip && enabled,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .widthIn(max = 200.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface)
            ) {
                Text(
                    text = tooltipText,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    }
}
