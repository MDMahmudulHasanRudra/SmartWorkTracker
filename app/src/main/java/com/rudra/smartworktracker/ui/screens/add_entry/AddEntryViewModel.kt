package com.rudra.smartworktracker.ui.screens.add_entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.repository.ExpenseRepository
import com.rudra.smartworktracker.data.repository.WorkLogRepository
import com.rudra.smartworktracker.model.Expense
import com.rudra.smartworktracker.model.ExpenseCategory
import com.rudra.smartworktracker.model.WorkLog
import com.rudra.smartworktracker.ui.AddEntryUiState
import com.rudra.smartworktracker.ui.EntryType
import com.rudra.smartworktracker.utils.CurrencyManager
import com.rudra.smartworktracker.utils.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

class AddEntryViewModel(
    private val expenseRepository: ExpenseRepository,
    private val workLogRepository: WorkLogRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEntryUiState())
    val uiState = _uiState.asStateFlow()

    // The nav argument defaults to -1 for "new entry"; only positive ids are real rows
    private val editingWorkLogId: Long? = savedStateHandle.get<Long>("workLogId")?.takeIf { it > 0 }
    private var editingWorkLog: WorkLog? = null

    val isEditing: Boolean get() = editingWorkLogId != null

    init {
        editingWorkLogId?.let { id ->
            viewModelScope.launch {
                val workLog = workLogRepository.getWorkLogByIdOnce(id) ?: return@launch
                editingWorkLog = workLog
                _uiState.update {
                    it.copy(
                        selectedEntryType = EntryType.WORK_TIME,
                        workType = workLog.workType,
                        workStartTime = workLog.startTime ?: "",
                        workEndTime = workLog.endTime ?: ""
                    )
                }
            }
        }
    }

    fun onExpenseAmountChange(amount: String) {
        _uiState.update { it.copy(expenseAmount = amount) }
    }

    fun onExpenseCategoryChange(category: ExpenseCategory) {
        _uiState.update { it.copy(expenseCategory = category) }
    }

    fun onExpenseNotesChange(notes: String) {
        _uiState.update { it.copy(expenseNotes = notes) }
    }

    fun saveExpense() {
        val amount = _uiState.value.expenseAmount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount") }
            return
        }
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(errorMessage = null, isLoading = true) }
        viewModelScope.launch {
            val expense = Expense(
                amount = amount,
                currency = CurrencyManager.getCurrencyCode(),
                category = _uiState.value.expenseCategory,
                merchant = null,
                notes = _uiState.value.expenseNotes.trim(),
                timestamp = System.currentTimeMillis(),
                imageUri = null
            )
            expenseRepository.insertExpense(expense)
            _uiState.update { it.copy(isEntrySaved = true, isLoading = false) }
        }
    }

    fun onWorkTypeChange(workType: com.rudra.smartworktracker.model.WorkType) {
        _uiState.update { it.copy(workType = workType) }
    }

    fun onWorkStartTimeChange(time: String) {
        _uiState.update { it.copy(workStartTime = time) }
    }

    fun onWorkEndTimeChange(time: String) {
        _uiState.update { it.copy(workEndTime = time) }
    }

    fun saveWorkLog() {
        val state = _uiState.value
        val needsTimes = state.workType != com.rudra.smartworktracker.model.WorkType.OFF_DAY
        if (needsTimes && (!DateTimeUtils.isValidTime(state.workStartTime) || !DateTimeUtils.isValidTime(state.workEndTime))) {
            _uiState.update { it.copy(errorMessage = "Please choose valid start and end times") }
            return
        }
        if (state.isLoading) return
        _uiState.update { it.copy(errorMessage = null, isLoading = true) }

        viewModelScope.launch {
            val original = editingWorkLog
            val now = System.currentTimeMillis()
            val isOvertime = state.workType == com.rudra.smartworktracker.model.WorkType.OVERTIME
            if (original != null) {
                // Editing keeps the log's original date (it used to jump to today)
                workLogRepository.updateWorkLog(
                    original.copy(
                        workType = state.workType,
                        startTime = state.workStartTime.takeIf { needsTimes },
                        endTime = state.workEndTime.takeIf { needsTimes },
                        isOvertime = isOvertime || original.isOvertime && state.workType == original.workType,
                        updatedAt = now
                    )
                )
            } else {
                workLogRepository.insertWorkLog(
                    WorkLog(
                        date = Date(now),
                        workType = state.workType,
                        startTime = state.workStartTime.takeIf { needsTimes },
                        endTime = state.workEndTime.takeIf { needsTimes },
                        isOvertime = isOvertime
                    )
                )
            }
            _uiState.update { it.copy(isEntrySaved = true, isLoading = false) }
        }
    }

    fun onMealAmountChange(amount: String) {
        _uiState.update { it.copy(mealAmount = amount) }
    }

    fun onMealNotesChange(notes: String) {
        _uiState.update { it.copy(mealNotes = notes) }
    }

    fun saveMeal() {
        val amount = _uiState.value.mealAmount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid meal amount") }
            return
        }
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(errorMessage = null, isLoading = true) }
        viewModelScope.launch {
            val mealExpense = Expense(
                amount = amount,
                currency = CurrencyManager.getCurrencyCode(),
                category = ExpenseCategory.MEAL,
                merchant = null,
                notes = _uiState.value.mealNotes.trim(),
                timestamp = System.currentTimeMillis(),
                imageUri = null
            )
            expenseRepository.insertExpense(mealExpense)
            _uiState.update { it.copy(isEntrySaved = true, isLoading = false) }
        }
    }

    fun onEntryTypeChange(entryType: EntryType) {
        _uiState.update { it.copy(selectedEntryType = entryType, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val savedStateHandle = extras.createSavedStateHandle()
                val database = AppDatabase.getDatabase(application)
                val expenseRepository = ExpenseRepository(database.expenseDao())
                val workLogRepository = WorkLogRepository(database.workLogDao())
                return AddEntryViewModel(expenseRepository, workLogRepository, savedStateHandle) as T
            }
        }
    }
}
