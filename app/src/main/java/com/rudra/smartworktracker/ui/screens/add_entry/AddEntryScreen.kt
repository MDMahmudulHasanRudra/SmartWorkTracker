package com.rudra.smartworktracker.ui.screens.add_entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rudra.smartworktracker.model.ExpenseCategory
import com.rudra.smartworktracker.model.WorkType
import com.rudra.smartworktracker.ui.EntryType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(onNavigateBack: () -> Boolean) {
    val viewModel: AddEntryViewModel = viewModel(factory = AddEntryViewModel.Factory)
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            coroutineScope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.isEntrySaved) {
        if (uiState.isEntrySaved) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Entry saved successfully") }
            onNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add New Entry") },
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
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            EntryTypeSelector(uiState.selectedEntryType, onEntryTypeSelect = viewModel::onEntryTypeChange)
            Spacer(modifier = Modifier.height(16.dp))

            when (uiState.selectedEntryType) {
                EntryType.EXPENSE -> ExpenseEntryForm(
                    amount = uiState.expenseAmount,
                    onAmountChange = viewModel::onExpenseAmountChange,
                    category = uiState.expenseCategory,
                    onCategoryChange = viewModel::onExpenseCategoryChange,
                    notes = uiState.expenseNotes,
                    onNotesChange = viewModel::onExpenseNotesChange,
                    onSave = viewModel::saveExpense
                )
                EntryType.WORK_TIME -> WorkTimeEntryForm(
                    workType = uiState.workType,
                    onWorkTypeChange = viewModel::onWorkTypeChange,
                    startTime = uiState.workStartTime,
                    onStartTimeChange = viewModel::onWorkStartTimeChange,
                    endTime = uiState.workEndTime,
                    onEndTimeChange = viewModel::onWorkEndTimeChange,
                    onSave = viewModel::saveWorkLog
                )
                EntryType.MEAL -> MealEntryForm(
                    amount = uiState.mealAmount,
                    onAmountChange = viewModel::onMealAmountChange,
                    notes = uiState.mealNotes,
                    onNotesChange = viewModel::onMealNotesChange,
                    onSave = viewModel::saveMeal
                )
            }
        }
    }
}

@Composable
fun EntryTypeSelector(selected: EntryType, onEntryTypeSelect: (EntryType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EntryType.values().forEach { entryType ->
            FilterChip(
                selected = selected == entryType,
                onClick = { onEntryTypeSelect(entryType) },
                label = { Text(entryType.name) }
            )
        }
    }
}

@Composable
fun ExpenseEntryForm(
    amount: String,
    onAmountChange: (String) -> Unit,
    category: ExpenseCategory,
    onCategoryChange: (ExpenseCategory) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = amount,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || (newValue.all { it.isDigit() || it == '.' } && newValue.count { it == '.' } <= 1)) onAmountChange(newValue)
            },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpenseCategory.values().forEach { expenseCategory ->
                FilterChip(
                    selected = category == expenseCategory,
                    onClick = { onCategoryChange(expenseCategory) },
                    label = { Text(expenseCategory.name) }
                )
            }
        }
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Save Expense")
        }
    }
}

@Composable
fun WorkTimeEntryForm(
    workType: WorkType,
    onWorkTypeChange: (WorkType) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    endTime: String,
    onEndTimeChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkType.values().forEach { type ->
                FilterChip(
                    selected = workType == type,
                    onClick = { onWorkTypeChange(type) },
                    label = { Text(type.name) }
                )
            }
        }
        OutlinedTextField(
            value = startTime,
            onValueChange = onStartTimeChange,
            label = { Text("Start Time") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = endTime,
            onValueChange = onEndTimeChange,
            label = { Text("End Time") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Save Work Log")
        }
    }
}

@Composable
fun MealEntryForm(
    amount: String,
    onAmountChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = amount,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || (newValue.all { it.isDigit() || it == '.' } && newValue.count { it == '.' } <= 1)) onAmountChange(newValue)
            },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Save Meal")
        }
    }
}
