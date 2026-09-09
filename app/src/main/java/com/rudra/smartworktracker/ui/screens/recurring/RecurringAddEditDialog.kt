package com.rudra.smartworktracker.ui.screens.recurring

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudra.smartworktracker.data.entity.AccountType
import com.rudra.smartworktracker.data.entity.displayName
import com.rudra.smartworktracker.data.entity.DayOfWeek
import com.rudra.smartworktracker.data.entity.ExpenseCategories
import com.rudra.smartworktracker.data.entity.IncomeCategories
import com.rudra.smartworktracker.data.entity.PreferredTime
import com.rudra.smartworktracker.data.entity.RecurringFrequency
import com.rudra.smartworktracker.data.entity.RecurringPriority
import com.rudra.smartworktracker.data.entity.RecurringRule
import com.rudra.smartworktracker.data.entity.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TemplateSelectionSheet(
    templates: List<RuleTemplate>,
    onSelectTemplate: (RuleTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Quick Templates",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tap a template to add it instantly",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        templates.forEach { template ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelectTemplate(template) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(getTransactionTypeColor(template.transactionType)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                getTransactionTypeIcon(template.transactionType),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = template.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(
                                text = template.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Text(
                        text = "$${String.format("%.0f", template.amount)}",
                        fontWeight = FontWeight.Bold,
                        color = if (template.transactionType == TransactionType.INCOME) Color(0xFF4CAF50) else Color(0xFFFF5252)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleContent(
    existingRule: RecurringRule? = null,
    accounts: List<com.rudra.smartworktracker.data.entity.Account> = emptyList(),
    onSave: (RecurringRule) -> Unit,
    onCancel: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(existingRule?.name ?: "") }
    var description by remember { mutableStateOf(existingRule?.description ?: "") }
    var amount by remember { mutableStateOf(existingRule?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(existingRule?.category ?: "") }
    var transactionType by remember { mutableStateOf(existingRule?.transactionType ?: TransactionType.EXPENSE) }
    var sourceAccount by remember { mutableStateOf(existingRule?.sourceAccount ?: AccountType.BALANCE) }
    var destinationAccount by remember { mutableStateOf(existingRule?.destinationAccount) }
    var selectedAccountId by remember { mutableStateOf(existingRule?.accountId) }
    var selectedDestinationAccountId by remember { mutableStateOf(existingRule?.destinationAccountId) }
    var frequency by remember { mutableStateOf(existingRule?.frequency ?: RecurringFrequency.MONTHLY) }
    var selectedDaysOfWeek by remember { mutableStateOf(existingRule?.selectedDaysOfWeek ?: emptyList()) }
    var priority by remember { mutableStateOf(existingRule?.priority ?: RecurringPriority.MEDIUM) }
    var preferredTime by remember { mutableStateOf(existingRule?.preferredTime ?: PreferredTime.MORNING) }
    var startDate by remember { mutableStateOf(existingRule?.startDate ?: System.currentTimeMillis()) }
    var endDate by remember { mutableStateOf(existingRule?.endDate) }
    var autoExecute by remember { mutableStateOf(existingRule?.autoExecute ?: true) }
    var minimumBalance by remember { mutableStateOf(existingRule?.minimumBalanceRequired?.toString() ?: "") }

    var typeExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var timeExpanded by remember { mutableStateOf(false) }
    var sourceExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    var destinationAccountExpanded by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val context = LocalContext.current

    val transactionTypes = TransactionType.values().filter {
        it != TransactionType.LOAN_BORROW && it != TransactionType.LOAN_LEND &&
        it != TransactionType.LOAN_REPAY && it != TransactionType.LOAN_RECEIVE && it != TransactionType.EMI_PAID
    }
    val categoriesForType = when (transactionType) {
        TransactionType.INCOME -> IncomeCategories.categories
        TransactionType.EXPENSE -> ExpenseCategories.categories
        else -> listOf("Other")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = if (existingRule != null) "Edit Rule" else "Add Rule",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        StepIndicator(currentStep = currentStep, totalSteps = 3)

        when (currentStep) {
            0 -> StepBasicInfo(
                name = name, onNameChange = { name = it },
                description = description, onDescriptionChange = { description = it },
                amount = amount, onAmountChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                transactionType = transactionType, onTypeChange = { transactionType = it },
                transactionTypes = transactionTypes,
                typeExpanded = typeExpanded, onTypeExpandedChange = { typeExpanded = it },
                category = category, onCategoryChange = { category = it },
                categoriesForType = categoriesForType,
                categoryExpanded = categoryExpanded, onCategoryExpandedChange = { categoryExpanded = it }
            )
            1 -> StepSchedule(
                frequency = frequency, onFrequencyChange = { frequency = it },
                frequencyExpanded = frequencyExpanded, onFrequencyExpandedChange = { frequencyExpanded = it },
                selectedDaysOfWeek = selectedDaysOfWeek, onDaysChange = { selectedDaysOfWeek = it },
                preferredTime = preferredTime, onTimeChange = { preferredTime = it },
                timeExpanded = timeExpanded, onTimeExpandedChange = { timeExpanded = it },
                startDate = startDate, onStartDateChange = { startDate = it },
                endDate = endDate, onEndDateChange = { endDate = it },
                context = context, dateFormat = dateFormat
            )
            2 -> StepAdvanced(
                priority = priority, onPriorityChange = { priority = it },
                priorityExpanded = priorityExpanded, onPriorityExpandedChange = { priorityExpanded = it },
                sourceAccount = sourceAccount, onSourceChange = { sourceAccount = it },
                sourceExpanded = sourceExpanded, onSourceExpandedChange = { sourceExpanded = it },
                destinationAccount = destinationAccount, onDestinationChange = { destinationAccount = it },
                transactionType = transactionType,
                minimumBalance = minimumBalance, onMinimumBalanceChange = { minimumBalance = it },
                autoExecute = autoExecute, onAutoExecuteChange = { autoExecute = it },
                accounts = accounts,
                selectedAccountId = selectedAccountId, onAccountIdChange = { selectedAccountId = it },
                selectedDestinationAccountId = selectedDestinationAccountId, onDestinationAccountIdChange = { selectedDestinationAccountId = it },
                accountExpanded = accountExpanded, onAccountExpandedChange = { accountExpanded = it },
                destinationAccountExpanded = destinationAccountExpanded, onDestinationAccountExpandedChange = { destinationAccountExpanded = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep > 0) {
                Button(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) { Text("Back") }
            }

            if (currentStep < 2) {
                Button(
                    onClick = { currentStep++ },
                    modifier = Modifier.weight(1f),
                    enabled = if (currentStep == 0) name.isNotBlank() && amount.isNotBlank() else true
                ) { Text("Next") }
            } else {
                Button(
                    onClick = {
                        val amountDouble = amount.toDoubleOrNull() ?: 0.0
                        val minBalance = minimumBalance.toDoubleOrNull()
                        val initialNextDate = if (frequency == RecurringFrequency.WEEKLY_SPECIFIC_DAYS && selectedDaysOfWeek.isNotEmpty()) {
                            calculateInitialNextDate(selectedDaysOfWeek, startDate)
                        } else { startDate }

                        val rule = RecurringRule(
                            id = existingRule?.id ?: 0,
                            uuid = existingRule?.uuid,
                            name = name,
                            description = description.ifBlank { null },
                            transactionType = transactionType,
                            amount = amountDouble,
                            category = category.ifBlank { null },
                            sourceAccount = sourceAccount,
                            destinationAccount = destinationAccount,
                            accountId = selectedAccountId,
                            destinationAccountId = selectedDestinationAccountId,
                            frequency = frequency,
                            selectedDaysOfWeek = if (frequency == RecurringFrequency.WEEKLY_SPECIFIC_DAYS) selectedDaysOfWeek else null,
                            priority = priority,
                            preferredTime = preferredTime,
                            startDate = startDate,
                            endDate = endDate,
                            nextExecutionDate = existingRule?.nextExecutionDate ?: initialNextDate,
                            minimumBalanceRequired = minBalance,
                            autoExecute = autoExecute,
                            isActive = existingRule?.isActive ?: true
                        )
                        onSave(rule)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank() && amount.isNotBlank()
                ) { Text(if (existingRule != null) "Update" else "Save") }
            }
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }

        if (showStartDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { startDate = it }
                        showStartDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
                }
            ) { DatePicker(state = datePickerState) }
        }
    }
}

@Composable
fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index <= currentStep) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                    if (index < totalSteps - 1) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(
                                    if (index < currentStep) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepBasicInfo(
    name: String, onNameChange: (String) -> Unit,
    description: String, onDescriptionChange: (String) -> Unit,
    amount: String, onAmountChange: (String) -> Unit,
    transactionType: TransactionType, onTypeChange: (TransactionType) -> Unit,
    transactionTypes: List<TransactionType>,
    typeExpanded: Boolean, onTypeExpandedChange: (Boolean) -> Unit,
    category: String, onCategoryChange: (String) -> Unit,
    categoriesForType: List<String>,
    categoryExpanded: Boolean, onCategoryExpandedChange: (Boolean) -> Unit
) {
    Column {
        Text("Basic Information", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Text("Transaction Type", style = MaterialTheme.typography.labelMedium)
        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = onTypeExpandedChange) {
            OutlinedTextField(
                value = transactionType.name.replace("_", " "),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { onTypeExpandedChange(false) }) {
                transactionTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name.replace("_", " ")) },
                        onClick = { onTypeChange(type); onTypeExpandedChange(false) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = name, onValueChange = onNameChange,
            label = { Text("Name *") },
            placeholder = { Text("e.g., Monthly Rent") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = amount, onValueChange = onAmountChange,
            label = { Text("Amount *") },
            placeholder = { Text("0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            leadingIcon = { Text("$") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Category", style = MaterialTheme.typography.labelMedium)
        ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = onCategoryExpandedChange) {
            OutlinedTextField(
                value = category.ifBlank { "Select Category" },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { onCategoryExpandedChange(false) }) {
                categoriesForType.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = { onCategoryChange(cat); onCategoryExpandedChange(false) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepSchedule(
    frequency: RecurringFrequency, onFrequencyChange: (RecurringFrequency) -> Unit,
    frequencyExpanded: Boolean, onFrequencyExpandedChange: (Boolean) -> Unit,
    selectedDaysOfWeek: List<DayOfWeek>, onDaysChange: (List<DayOfWeek>) -> Unit,
    preferredTime: PreferredTime, onTimeChange: (PreferredTime) -> Unit,
    timeExpanded: Boolean, onTimeExpandedChange: (Boolean) -> Unit,
    startDate: Long, onStartDateChange: (Long) -> Unit,
    endDate: Long?, onEndDateChange: (Long?) -> Unit,
    context: android.content.Context,
    dateFormat: SimpleDateFormat
) {
    Column {
        Text("Schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Text("Frequency", style = MaterialTheme.typography.labelMedium)
        ExposedDropdownMenuBox(expanded = frequencyExpanded, onExpandedChange = onFrequencyExpandedChange) {
            OutlinedTextField(
                value = getFrequencyDisplayName(frequency),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = frequencyExpanded, onDismissRequest = { onFrequencyExpandedChange(false) }) {
                RecurringFrequency.values().forEach { freq ->
                    DropdownMenuItem(
                        text = { Text(getFrequencyDisplayName(freq)) },
                        onClick = { onFrequencyChange(freq); onFrequencyExpandedChange(false) }
                    )
                }
            }
        }

        if (frequency == RecurringFrequency.WEEKLY_SPECIFIC_DAYS) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Select Days", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DayOfWeek.values().forEach { day ->
                    val isSelected = selectedDaysOfWeek.contains(day)
                    Card(
                        onClick = {
                            onDaysChange(if (isSelected) selectedDaysOfWeek - day else selectedDaysOfWeek + day)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = day.shortName,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Preferred Time", style = MaterialTheme.typography.labelMedium)
        ExposedDropdownMenuBox(expanded = timeExpanded, onExpandedChange = onTimeExpandedChange) {
            OutlinedTextField(
                value = preferredTime.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = timeExpanded, onDismissRequest = { onTimeExpandedChange(false) }) {
                PreferredTime.values().forEach { t ->
                    DropdownMenuItem(
                        text = { Text(t.name) },
                        onClick = { onTimeChange(t); onTimeExpandedChange(false) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = dateFormat.format(Date(startDate)),
            onValueChange = {},
            label = { Text("Start Date") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = startDate }
                    DatePickerDialog(context, { _, year, month, day ->
                        val newCal = Calendar.getInstance()
                        newCal.set(year, month, day)
                        onStartDateChange(newCal.timeInMillis)
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepAdvanced(
    priority: RecurringPriority, onPriorityChange: (RecurringPriority) -> Unit,
    priorityExpanded: Boolean, onPriorityExpandedChange: (Boolean) -> Unit,
    sourceAccount: AccountType, onSourceChange: (AccountType) -> Unit,
    sourceExpanded: Boolean, onSourceExpandedChange: (Boolean) -> Unit,
    destinationAccount: AccountType?, onDestinationChange: (AccountType?) -> Unit,
    transactionType: TransactionType,
    minimumBalance: String, onMinimumBalanceChange: (String) -> Unit,
    autoExecute: Boolean, onAutoExecuteChange: (Boolean) -> Unit,
    accounts: List<com.rudra.smartworktracker.data.entity.Account> = emptyList(),
    selectedAccountId: Long?, onAccountIdChange: (Long?) -> Unit,
    selectedDestinationAccountId: Long?, onDestinationAccountIdChange: (Long?) -> Unit,
    accountExpanded: Boolean, onAccountExpandedChange: (Boolean) -> Unit,
    destinationAccountExpanded: Boolean, onDestinationAccountExpandedChange: (Boolean) -> Unit
) {
    Column {
        Text("Advanced Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Text("Priority", style = MaterialTheme.typography.labelMedium)
        ExposedDropdownMenuBox(expanded = priorityExpanded, onExpandedChange = onPriorityExpandedChange) {
            OutlinedTextField(
                value = priority.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = priorityExpanded, onDismissRequest = { onPriorityExpandedChange(false) }) {
                RecurringPriority.values().forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p.name) },
                        onClick = { onPriorityChange(p); onPriorityExpandedChange(false) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Link to Account", style = MaterialTheme.typography.labelMedium)
        ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = onAccountExpandedChange) {
            val selectedAccount = accounts.find { it.id == selectedAccountId }
            OutlinedTextField(
                value = selectedAccount?.let { "${it.name} (${com.rudra.smartworktracker.utils.CurrencyManager.format(it.balance)})" } ?: "No account selected",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = accountExpanded, onDismissRequest = { onAccountExpandedChange(false) }) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = { onAccountIdChange(null); onAccountExpandedChange(false) }
                )
                val activeAccounts = accounts.filter { it.isActive }
                val grouped = activeAccounts.groupBy { it.type }
                grouped.forEach { (category, categoryAccounts) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = category.displayName(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {},
                        enabled = false
                    )
                    categoryAccounts.forEach { account ->
                        DropdownMenuItem(
                            text = {
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(account.name)
                                    Text(
                                        text = "${account.provider.displayName()} \u2022 ${com.rudra.smartworktracker.utils.CurrencyManager.format(account.balance)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = { onAccountIdChange(account.id); onAccountExpandedChange(false) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Source Account (Legacy)", style = MaterialTheme.typography.labelMedium)
        ExposedDropdownMenuBox(expanded = sourceExpanded, onExpandedChange = onSourceExpandedChange) {
            OutlinedTextField(
                value = sourceAccount.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = sourceExpanded, onDismissRequest = { onSourceExpandedChange(false) }) {
                AccountType.values().forEach { acc ->
                    DropdownMenuItem(
                        text = { Text(acc.name) },
                        onClick = { onSourceChange(acc); onSourceExpandedChange(false) }
                    )
                }
            }
        }

        if (transactionType == TransactionType.TRANSFER) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Destination Account", style = MaterialTheme.typography.labelMedium)
            ExposedDropdownMenuBox(expanded = destinationAccountExpanded, onExpandedChange = onDestinationAccountExpandedChange) {
                val selectedDestAccount = accounts.find { it.id == selectedDestinationAccountId }
                OutlinedTextField(
                    value = selectedDestAccount?.let { "${it.name} (${com.rudra.smartworktracker.utils.CurrencyManager.format(it.balance)})" } ?: "No account selected",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destinationAccountExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = destinationAccountExpanded, onDismissRequest = { onDestinationAccountExpandedChange(false) }) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = { onDestinationAccountIdChange(null); onDestinationAccountExpandedChange(false) }
                    )
                    val filteredAccounts = accounts.filter { it.isActive && it.id != selectedAccountId }
                    val grouped = filteredAccounts.groupBy { it.type }
                    grouped.forEach { (category, categoryAccounts) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = category.displayName(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {},
                            enabled = false
                        )
                        categoryAccounts.forEach { account ->
                            DropdownMenuItem(
                                text = {
                                    Column(modifier = Modifier.padding(start = 8.dp)) {
                                        Text(account.name)
                                        Text(
                                            text = "${account.provider.displayName()} \u2022 ${com.rudra.smartworktracker.utils.CurrencyManager.format(account.balance)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = { onDestinationAccountIdChange(account.id); onDestinationAccountExpandedChange(false) }
                            )
                        }
                    }
                }
            }
        }

        if (transactionType == TransactionType.EXPENSE) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = minimumBalance,
                onValueChange = { onMinimumBalanceChange(it.filter { c -> c.isDigit() || c == '.' }) },
                label = { Text("Minimum Balance Required") },
                placeholder = { Text("Leave empty for no minimum") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text(com.rudra.smartworktracker.utils.CurrencyManager.symbol()) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Auto Execute", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = autoExecute, onCheckedChange = onAutoExecuteChange)
        }
    }
}

@Composable
fun ManualExecutionDialog(
    onDismiss: () -> Unit,
    onExecute: (List<RecurringRule>) -> Unit,
    rules: List<RecurringRule>
) {
    var selectedRules by remember { mutableStateOf(setOf<RecurringRule>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Execution") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                if (rules.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No active rules", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                } else {
                    LazyColumn {
                        items(rules) { rule ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedRules = if (selectedRules.contains(rule)) selectedRules - rule else selectedRules + rule
                                }.padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selectedRules.contains(rule),
                                        onCheckedChange = {
                                            selectedRules = if (it) selectedRules + rule else selectedRules - rule
                                        }
                                    )
                                    Column {
                                        Text(rule.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text(
                                            "$${String.format("%.2f", rule.amount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onExecute(selectedRules.toList()) }, enabled = selectedRules.isNotEmpty()) {
                Text("Execute (${selectedRules.size})")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ExecutionResultDialog(
    result: RecurringViewModel.ManualExecutionResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (result.success) Icons.Default.Repeat else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (result.success) Color(0xFF4CAF50) else Color(0xFFFF5252)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (result.success) "Success" else "Partial Failure")
            }
        },
        text = {
            Column {
                Text("Successful: ${result.successCount}")
                Text("Failed: ${result.failureCount}")
                Text("Total: $${String.format("%.2f", result.totalAmount)}")
                if (result.failedRules.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    result.failedRules.forEach { (name, reason) ->
                        Text("• $name: $reason", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
    )
}

@Composable
fun RecurringOnboardingDialog(onDismiss: () -> Unit) {
    var step by remember { mutableStateOf(0) }

    val steps = listOf(
        Triple("Welcome to Recurring Transactions", "Automate your regular income and expenses so you never miss a payment.", Icons.Default.Repeat),
        Triple("Smart Execution", "Rules execute automatically based on your schedule. You can also run them manually anytime.", Icons.Default.PlayArrow),
        Triple("Insights & Alerts", "Track spending patterns, get alerts when approaching limits, and see yearly projections.", Icons.Default.TrendingUp)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(steps[step].third, contentDescription = null, modifier = Modifier.size(48.dp)) },
        title = { Text(steps[step].first, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(steps[step].second, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    steps.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (index == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (step > 0) {
                    TextButton(onClick = { step-- }) { Text("Back") }
                }
                Button(onClick = {
                    if (step < steps.lastIndex) step++ else onDismiss()
                }) {
                    Text(if (step < steps.lastIndex) "Next" else "Get Started")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Skip") }
        }
    )
}
