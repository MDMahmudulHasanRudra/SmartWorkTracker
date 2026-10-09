package com.rudra.smartworktracker.ui.screens.add_entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.model.ExpenseCategory
import com.rudra.smartworktracker.model.WorkType
import com.rudra.smartworktracker.ui.EntryType
import com.rudra.smartworktracker.utils.CurrencyManager
import com.rudra.smartworktracker.utils.DateTimeUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(onNavigateBack: () -> Boolean) {
    val viewModel: AddEntryViewModel = viewModel(factory = AddEntryViewModel.Factory)
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.isEntrySaved) {
        if (uiState.isEntrySaved) onNavigateBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Edit Work Log" else "Add New Entry", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!viewModel.isEditing) {
                EntryTypeSelector(uiState.selectedEntryType, onEntryTypeSelect = viewModel::onEntryTypeChange)
            }

            when (uiState.selectedEntryType) {
                EntryType.EXPENSE -> ExpenseEntryForm(
                    amount = uiState.expenseAmount,
                    onAmountChange = viewModel::onExpenseAmountChange,
                    category = uiState.expenseCategory,
                    onCategoryChange = viewModel::onExpenseCategoryChange,
                    notes = uiState.expenseNotes,
                    onNotesChange = viewModel::onExpenseNotesChange,
                    isSaving = uiState.isLoading,
                    onSave = viewModel::saveExpense
                )
                EntryType.WORK_TIME -> WorkTimeEntryForm(
                    workType = uiState.workType,
                    onWorkTypeChange = viewModel::onWorkTypeChange,
                    startTime = uiState.workStartTime,
                    onStartTimeChange = viewModel::onWorkStartTimeChange,
                    endTime = uiState.workEndTime,
                    onEndTimeChange = viewModel::onWorkEndTimeChange,
                    isSaving = uiState.isLoading,
                    saveLabel = if (viewModel.isEditing) "Update Work Log" else "Save Work Log",
                    onSave = viewModel::saveWorkLog
                )
                EntryType.MEAL -> MealEntryForm(
                    amount = uiState.mealAmount,
                    onAmountChange = viewModel::onMealAmountChange,
                    notes = uiState.mealNotes,
                    onNotesChange = viewModel::onMealNotesChange,
                    isSaving = uiState.isLoading,
                    onSave = viewModel::saveMeal
                )
            }
        }
    }
}

private fun EntryType.label(): String = when (this) {
    EntryType.EXPENSE -> "Expense"
    EntryType.WORK_TIME -> "Work"
    EntryType.MEAL -> "Meal"
}

private fun WorkType.label(): String = when (this) {
    WorkType.OFFICE -> "Office"
    WorkType.HOME_OFFICE -> "Home Office"
    WorkType.OFF_DAY -> "Off Day"
    WorkType.EXTRA_WORK -> "Extra Work"
    WorkType.OVERTIME -> "Overtime"
}

/** Accepts only a positive decimal with at most two fraction digits. */
private fun sanitizeAmount(input: String): String? =
    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) input else null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryTypeSelector(selected: EntryType, onEntryTypeSelect: (EntryType) -> Unit) {
    val types = EntryType.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        types.forEachIndexed { index, entryType ->
            SegmentedButton(
                selected = selected == entryType,
                onClick = { onEntryTypeSelect(entryType) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                label = { Text(entryType.label()) }
            )
        }
    }
}

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun SaveButton(text: String, isSaving: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled && !isSaving,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        if (isSaving) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseEntryForm(
    amount: String,
    onAmountChange: (String) -> Unit,
    category: ExpenseCategory,
    onCategoryChange: (ExpenseCategory) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    isSaving: Boolean = false,
    onSave: () -> Unit
) {
    FormCard {
        OutlinedTextField(
            value = amount,
            onValueChange = { input -> sanitizeAmount(input)?.let(onAmountChange) },
            label = { Text("Amount") },
            prefix = { Text(CurrencyManager.symbol()) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        // Wraps onto several lines: twelve chips never fit one row
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ExpenseCategory.entries.forEach { expenseCategory ->
                FilterChip(
                    selected = category == expenseCategory,
                    onClick = { onCategoryChange(expenseCategory) },
                    label = { Text(expenseCategory.displayName) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(expenseCategory.color, CircleShape)
                        )
                    }
                )
            }
        }
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
    }
    SaveButton("Save Expense", isSaving, enabled = (amount.toDoubleOrNull() ?: 0.0) > 0, onClick = onSave)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkTimeEntryForm(
    workType: WorkType,
    onWorkTypeChange: (WorkType) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    endTime: String,
    onEndTimeChange: (String) -> Unit,
    isSaving: Boolean = false,
    saveLabel: String = "Save Work Log",
    onSave: () -> Unit
) {
    val needsTimes = workType != WorkType.OFF_DAY
    FormCard {
        Text("Work type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            WorkType.entries.forEach { type ->
                FilterChip(
                    selected = workType == type,
                    onClick = { onWorkTypeChange(type) },
                    label = { Text(type.label()) }
                )
            }
        }
        if (needsTimes) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeField(
                    label = "Start",
                    value = startTime,
                    onValueChange = onStartTimeChange,
                    modifier = Modifier.weight(1f)
                )
                TimeField(
                    label = "End",
                    value = endTime,
                    onValueChange = onEndTimeChange,
                    modifier = Modifier.weight(1f)
                )
            }
            val duration = DateTimeUtils.formatDuration(startTime, endTime)
            if (duration != "-") {
                Text(
                    text = "Duration: $duration" + if ((DateTimeUtils.minutesOfDay(endTime) ?: 0) < (DateTimeUtils.minutesOfDay(startTime) ?: 0)) " (overnight)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
    SaveButton(
        saveLabel,
        isSaving,
        enabled = !needsTimes || (DateTimeUtils.isValidTime(startTime) && DateTimeUtils.isValidTime(endTime)),
        onClick = onSave
    )
}

/** Read-only field that opens a Material time picker; avoids free-text times that can't be parsed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("HH:mm") },
            trailingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        // Transparent overlay: a read-only TextField swallows clicks itself
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showPicker = true }
        )
    }

    if (showPicker) {
        val minutes = DateTimeUtils.minutesOfDay(value) ?: (9 * 60)
        val state = rememberTimePickerState(initialHour = minutes / 60, initialMinute = minutes % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("$label time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = state)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(String.format(Locale.US, "%02d:%02d", state.hour, state.minute))
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MealEntryForm(
    amount: String,
    onAmountChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    isSaving: Boolean = false,
    onSave: () -> Unit
) {
    FormCard {
        OutlinedTextField(
            value = amount,
            onValueChange = { input -> sanitizeAmount(input)?.let(onAmountChange) },
            label = { Text("Meal cost") },
            prefix = { Text(CurrencyManager.symbol()) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
    }
    SaveButton("Save Meal", isSaving, enabled = (amount.toDoubleOrNull() ?: 0.0) > 0, onClick = onSave)
}
