package com.rudra.smartworktracker.ui.screens.loans

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.entity.AccountType
import com.rudra.smartworktracker.data.entity.Loan
import com.rudra.smartworktracker.data.entity.LoanCategory
import com.rudra.smartworktracker.data.entity.LoanType
import com.rudra.smartworktracker.data.repository.LoanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class LoanStatistics(
    val totalBorrowed: Double = 0.0,
    val totalLent: Double = 0.0,
    val borrowedCount: Int = 0,
    val lentCount: Int = 0,
    val overdueCount: Int = 0,
    val netPosition: Double = 0.0,
    val totalCount: Int = 0
)

data class LoansUiState(
    val loans: List<Loan> = emptyList(),
    val filteredLoans: List<Loan> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showAddLoanDialog: Boolean = false,
    val showEditLoanDialog: Loan? = null,
    val showRepayDialogForLoan: Loan? = null,
    val showDeleteConfirmationForLoan: Loan? = null,
    val showLoanDetailsDialog: Loan? = null,
    val selectedTab: LoanTab = LoanTab.ALL,
    val searchQuery: String = "",
    val statistics: LoanStatistics = LoanStatistics()
)

enum class LoanTab(val title: String) {
    ALL("All"),
    BORROWED("Borrowed"),
    LENT("Lent"),
    OVERDUE("Overdue"),
    SETTLED("Settled")
}

private val Loan.isOpen: Boolean get() = isActive && !isFullyPaid

class LoanViewModel(private val loanRepository: LoanRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoansUiState())
    val uiState: StateFlow<LoansUiState> = _uiState.asStateFlow()

    init {
        loadLoans()
        loadStatistics()
    }

    private fun loadLoans() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            loanRepository.getAllLoans()
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message
                    )
                }
                .collect { loans ->
                    _uiState.value = _uiState.value.copy(
                        loans = loans,
                        filteredLoans = filterLoans(loans, _uiState.value.selectedTab, _uiState.value.searchQuery),
                        isLoading = false
                    )
                }
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            combine(
                loanRepository.getTotalBorrowed(),
                loanRepository.getTotalLent(),
                loanRepository.getActiveBorrowedCount(),
                loanRepository.getActiveLentCount(),
                loanRepository.getOverdueLoans()
            ) { borrowed, lent, borrowedCount, lentCount, overdueLoans ->
                LoanStatistics(
                    totalBorrowed = borrowed ?: 0.0,
                    totalLent = lent ?: 0.0,
                    borrowedCount = borrowedCount,
                    lentCount = lentCount,
                    overdueCount = overdueLoans.size,
                    netPosition = (lent ?: 0.0) - (borrowed ?: 0.0),
                    totalCount = borrowedCount + lentCount
                )
            }.collect { stats ->
                _uiState.value = _uiState.value.copy(statistics = stats)
            }
        }
    }

    private fun filterLoans(loans: List<Loan>, tab: LoanTab, query: String): List<Loan> {
        var filtered = when (tab) {
            LoanTab.ALL -> loans.filter { it.isOpen }
            LoanTab.BORROWED -> loans.filter { it.isOpen && it.loanType == LoanType.BORROWED }
            LoanTab.LENT -> loans.filter { it.isOpen && it.loanType == LoanType.LENT }
            LoanTab.OVERDUE -> loans.filter { it.isOpen && it.isOverdue }
            LoanTab.SETTLED -> loans.filter { !it.isOpen }
        }

        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.personName.contains(query, ignoreCase = true) ||
                it.notes?.contains(query, ignoreCase = true) == true ||
                it.contactNumber?.contains(query, ignoreCase = true) == true
            }
        }

        return filtered
    }

    fun setSelectedTab(tab: LoanTab) {
        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            filteredLoans = filterLoans(_uiState.value.loans, tab, _uiState.value.searchQuery)
        )
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredLoans = filterLoans(_uiState.value.loans, _uiState.value.selectedTab, query)
        )
    }

    fun addLoan(
        personName: String,
        contactNumber: String?,
        amount: Double,
        loanType: LoanType,
        loanCategory: LoanCategory,
        dueDate: Long?,
        interestRate: Double?,
        emiAmount: Double?,
        totalEmis: Int?,
        notes: String?,
        sourceAccount: AccountType,
        destinationAccount: AccountType
    ) {
        viewModelScope.launch {
            val loan = Loan(
                personName = personName,
                contactNumber = contactNumber,
                initialAmount = amount,
                remainingAmount = amount,
                loanType = loanType,
                loanCategory = loanCategory,
                date = System.currentTimeMillis(),
                dueDate = dueDate,
                interestRate = interestRate,
                emiAmount = emiAmount,
                totalEmis = totalEmis,
                notes = notes,
                sourceAccount = sourceAccount,
                destinationAccount = destinationAccount
            )
            loanRepository.insertLoan(loan)
            closeAddLoanDialog()
        }
    }

    fun updateLoan(loan: Loan) {
        viewModelScope.launch {
            loanRepository.updateLoan(loan)
            closeEditLoanDialog()
        }
    }

    /**
     * Applies an edited principal: the outstanding balance moves by the same difference, so
     * repayments already made are preserved.
     */
    fun updateLoan(original: Loan, edited: Loan, newAmount: Double) {
        val delta = newAmount - original.initialAmount
        val remaining = (original.remainingAmount + delta).coerceAtLeast(0.0)
        updateLoan(
            edited.copy(
                initialAmount = newAmount,
                remainingAmount = remaining,
                isFullyPaid = remaining <= 0.0,
                isActive = remaining > 0.0
            )
        )
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            loanRepository.deleteLoan(loan)
            closeDeleteConfirmationDialog()
        }
    }

    fun repayLoan(loan: Loan, amount: Double) {
        viewModelScope.launch {
            loanRepository.repayLoan(loan, amount)
            closeRepayDialog()
        }
    }

    fun markLoanAsPaid(loan: Loan) {
        viewModelScope.launch {
            loanRepository.markLoanAsPaid(loan)
        }
    }

    fun openAddLoanDialog() {
        _uiState.value = _uiState.value.copy(showAddLoanDialog = true)
    }

    fun closeAddLoanDialog() {
        _uiState.value = _uiState.value.copy(showAddLoanDialog = false)
    }

    fun openEditLoanDialog(loan: Loan) {
        _uiState.value = _uiState.value.copy(showEditLoanDialog = loan)
    }

    fun closeEditLoanDialog() {
        _uiState.value = _uiState.value.copy(showEditLoanDialog = null)
    }

    fun openRepayDialog(loan: Loan) {
        _uiState.value = _uiState.value.copy(showRepayDialogForLoan = loan)
    }

    fun closeRepayDialog() {
        _uiState.value = _uiState.value.copy(showRepayDialogForLoan = null)
    }

    fun openDeleteConfirmationDialog(loan: Loan) {
        _uiState.value = _uiState.value.copy(showDeleteConfirmationForLoan = loan)
    }

    fun closeDeleteConfirmationDialog() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmationForLoan = null)
    }

    fun openLoanDetailsDialog(loan: Loan) {
        _uiState.value = _uiState.value.copy(showLoanDetailsDialog = loan)
    }

    fun closeLoanDetailsDialog() {
        _uiState.value = _uiState.value.copy(showLoanDetailsDialog = null)
    }
}
