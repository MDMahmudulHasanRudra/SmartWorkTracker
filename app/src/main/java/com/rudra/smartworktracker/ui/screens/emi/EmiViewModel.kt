package com.rudra.smartworktracker.ui.screens.emi

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.entity.AccountType
import com.rudra.smartworktracker.data.entity.Emi
import com.rudra.smartworktracker.data.entity.EmiStatus
import com.rudra.smartworktracker.data.entity.Loan
import com.rudra.smartworktracker.data.repository.EmiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

data class EmiWithLoan(
    val emi: Emi,
    val loan: Loan?
)

data class EmiStatistics(
    val totalPending: Double = 0.0,
    val pendingCount: Int = 0,
    val overdueCount: Int = 0,
    val thisMonthTotal: Double = 0.0,
    val totalPenaltyCollected: Double = 0.0
)

data class EmiUiState(
    val emis: List<EmiWithLoan> = emptyList(),
    val filteredEmis: List<EmiWithLoan> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showAddEmiDialog: Boolean = false,
    val showPayEmiDialog: EmiWithLoan? = null,
    val showDeleteConfirmation: EmiWithLoan? = null,
    // ALL by default: a newly added EMI due within 5 days is "Due", not "Upcoming"
    val selectedTab: EmiTab = EmiTab.ALL,
    val searchQuery: String = "",
    val statistics: EmiStatistics = EmiStatistics(),
    val availableLoans: List<Loan> = emptyList()
)

enum class EmiTab(val title: String) {
    ALL("All"),
    UPCOMING("Upcoming"),
    DUE("Due"),
    OVERDUE("Overdue"),
    PAID("Paid")
}

class EmiViewModel(private val emiRepository: EmiRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(EmiUiState())
    val uiState: StateFlow<EmiUiState> = _uiState.asStateFlow()

    init {
        loadEmis()
        loadStatistics()
        loadAvailableLoans()
    }

    private fun loadEmis() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            combine(
                emiRepository.getActiveEmis(),
                emiRepository.getPaidEmis()
            ) { activeEmis, paidEmis ->
                val allEmis = (activeEmis + paidEmis)
                allEmis.mapNotNull { emi ->
                    val loan = emiRepository.getEmiWithLoan(emi.id).second
                    EmiWithLoan(emi, loan)
                }
            }
            .catch { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message
                )
            }
            .collect { emisWithLoans ->
                _uiState.value = _uiState.value.copy(
                    emis = emisWithLoans,
                    filteredEmis = filterEmis(emisWithLoans, _uiState.value.selectedTab, _uiState.value.searchQuery),
                    isLoading = false
                )
            }
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            combine(
                emiRepository.getTotalPendingAmount(),
                emiRepository.getPendingEmiCount(),
                emiRepository.getActiveEmis(),
                emiRepository.getTotalPenaltyCollected(),
                emiRepository.getEmisDueThisMonth()
            ) { totalPending, pendingCount, activeEmis, penaltyCollected, thisMonthEmis ->
                EmiStatistics(
                    totalPending = totalPending ?: 0.0,
                    pendingCount = pendingCount,
                    // Same rule as the Overdue tab (EmiStatus), not a raw "past due date" count
                    overdueCount = activeEmis.count { it.status == EmiStatus.OVERDUE },
                    thisMonthTotal = thisMonthEmis.sumOf { it.amount },
                    totalPenaltyCollected = penaltyCollected ?: 0.0
                )
            }.collect { stats ->
                _uiState.value = _uiState.value.copy(statistics = stats)
            }
        }
    }

    // Any open loan can get an EMI; previously only loans that already had one were offered
    private fun loadAvailableLoans() {
        viewModelScope.launch {
            emiRepository.getActiveLoans().collect { loans ->
                _uiState.value = _uiState.value.copy(
                    availableLoans = loans.filter { it.isActive && !it.isFullyPaid }
                )
            }
        }
    }

    private fun filterEmis(emis: List<EmiWithLoan>, tab: EmiTab, query: String): List<EmiWithLoan> {
        var filtered = when (tab) {
            EmiTab.ALL -> emis
            EmiTab.UPCOMING -> emis.filter { it.emi.status == EmiStatus.UPCOMING }
            EmiTab.DUE -> emis.filter { it.emi.status == EmiStatus.DUE }
            EmiTab.OVERDUE -> emis.filter { it.emi.status == EmiStatus.OVERDUE }
            EmiTab.PAID -> emis.filter { it.emi.status == EmiStatus.PAID }
        }

        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.loan?.personName?.contains(query, ignoreCase = true) == true ||
                it.emi.notes?.contains(query, ignoreCase = true) == true
            }
        }

        return filtered
    }

    fun setSelectedTab(tab: EmiTab) {
        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            filteredEmis = filterEmis(_uiState.value.emis, tab, _uiState.value.searchQuery)
        )
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredEmis = filterEmis(_uiState.value.emis, _uiState.value.selectedTab, query)
        )
    }

    fun addEmi(
        loanId: Int,
        amount: Double,
        principalAmount: Double,
        interestAmount: Double,
        dueDateOfMonth: Int,
        notes: String?,
        paymentAccount: AccountType
    ) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, dueDateOfMonth.coerceIn(1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)))
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.MONTH, 1)
            }

            val emi = Emi(
                loanId = loanId,
                amount = amount,
                principalAmount = principalAmount,
                interestAmount = interestAmount,
                dueDateOfMonth = dueDateOfMonth,
                nextDueDate = calendar.timeInMillis,
                notes = notes,
                paymentAccount = paymentAccount
            )
            emiRepository.insertEmi(emi)
            closeAddEmiDialog()
        }
    }

    fun payEmi(emi: Emi) {
        viewModelScope.launch {
            emiRepository.payEmi(emi)
            closePayEmiDialog()
        }
    }

    fun skipEmi(emi: Emi) {
        viewModelScope.launch {
            emiRepository.skipEmi(emi)
        }
    }

    fun deleteEmi(emi: Emi) {
        viewModelScope.launch {
            emiRepository.deleteEmi(emi)
            closeDeleteConfirmation()
        }
    }

    fun openAddEmiDialog() {
        _uiState.value = _uiState.value.copy(showAddEmiDialog = true)
    }

    fun closeAddEmiDialog() {
        _uiState.value = _uiState.value.copy(showAddEmiDialog = false)
    }

    fun openPayEmiDialog(emiWithLoan: EmiWithLoan) {
        _uiState.value = _uiState.value.copy(showPayEmiDialog = emiWithLoan)
    }

    fun closePayEmiDialog() {
        _uiState.value = _uiState.value.copy(showPayEmiDialog = null)
    }

    fun openDeleteConfirmation(emiWithLoan: EmiWithLoan) {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = emiWithLoan)
    }

    fun closeDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = null)
    }
}
