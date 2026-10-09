package com.rudra.smartworktracker.ui.screens.billsplit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.entity.BillSplit
import com.rudra.smartworktracker.data.repository.BillSplitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillSplitViewModel(
    private val repository: BillSplitRepository
) : ViewModel() {

    private val _billSplits = MutableStateFlow<List<BillSplit>>(emptyList())
    val billSplits: StateFlow<List<BillSplit>> = _billSplits.asStateFlow()

    init {
        loadBillSplits()
    }

    private fun loadBillSplits() {
        viewModelScope.launch {
            repository.getAll().collect { splits ->
                _billSplits.value = splits
            }
        }
    }

    fun addBillSplit(billSplit: BillSplit) {
        viewModelScope.launch {
            repository.insert(billSplit)
        }
    }

    fun markSettled(billSplit: BillSplit) {
        viewModelScope.launch {
            // Toggles, so a bill marked settled by mistake can be reopened
            repository.update(billSplit.copy(isSettled = !billSplit.isSettled))
        }
    }

    fun deleteBillSplit(billSplit: BillSplit) {
        viewModelScope.launch {
            repository.deleteById(billSplit.id)
        }
    }
}

class BillSplitViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BillSplitViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val repository = BillSplitRepository(database.billSplitDao())
            @Suppress("UNCHECKED_CAST")
            return BillSplitViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
