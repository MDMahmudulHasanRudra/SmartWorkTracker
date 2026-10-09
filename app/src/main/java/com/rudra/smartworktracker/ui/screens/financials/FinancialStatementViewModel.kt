package com.rudra.smartworktracker.ui.screens.financials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.entity.FinancialTransaction
import com.rudra.smartworktracker.data.entity.Income
import com.rudra.smartworktracker.data.entity.TransactionType
import com.rudra.smartworktracker.data.repository.TransactionRepository
import com.rudra.smartworktracker.data.repository.IncomeRepository
import com.rudra.smartworktracker.data.repository.ExpenseRepository
import com.rudra.smartworktracker.model.Expense
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Account constants for consistent double-entry accounting
 * Income → Credit (Revenue Account)
 * Expense → Debit (Expense Account)
 * Cash/Bank → opposite side (asset account)
 */
object Accounts {
    const val CASH = "Cash/Bank"
    const val INCOME = "Income"
    const val EXPENSE = "Expense"
    const val TRANSFER = "Transfer"
}

/**
 * Source type for tracking original data source
 */
enum class TransactionSource {
    FINANCIAL,
    INCOME,
    EXPENSE
}

/**
 * Unified transaction model combining Income, Expense, and FinancialTransaction
 * for double-entry accounting display with proper ID tracking
 */
data class UnifiedTransaction(
    val id: String,
    val originalId: Any,           // Original ID from the source table (Long, Int, or String)
    val sourceType: TransactionSource,  // Source of the transaction
    val amount: Double,
    val type: TransactionType,
    val description: String,
    val category: String,
    val date: Long,
    val entryType: EntryType = EntryType.DEBIT,
    val debitAccount: String? = null,
    val creditAccount: String? = null
)

enum class EntryType {
    DEBIT,
    CREDIT
}

class FinancialStatementViewModel(
    private val transactionRepository: TransactionRepository,
    private val incomeRepository: IncomeRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialsUiState())
    val uiState: StateFlow<FinancialsUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val filterFlow = MutableStateFlow(TransactionFilter.ALL)
    private val startDateFlow = MutableStateFlow<LocalDate?>(null)
    private val endDateFlow = MutableStateFlow<LocalDate?>(null)
    private val limitFlow = MutableStateFlow(100)

    // Combine data sources separately first (max 5 flows allowed)
    private val dataFlow = combine(
        transactionRepository.getAllTransactions(),
        incomeRepository.getAllIncomes(),
        expenseRepository.getAllExpenses()
    ) { transactions, incomes, expenses ->
        Triple(transactions, incomes, expenses)
    }

    private val filterParamsFlow = combine(
        filterFlow,
        startDateFlow,
        endDateFlow,
        limitFlow
    ) { filter, startDate, endDate, limit ->
        FilterParams(filter, startDate, endDate, limit)
    }

    // Use stateIn for proper lifecycle management with proper typed parameters
    private val transactions: StateFlow<FinancialsUiState> = combine(
        dataFlow,
        filterParamsFlow
    ) { data, params ->
        val (transactions, incomes, expenses) = data
        
        // Convert incomes to unified transactions (double-entry: Cash/Bank DEBIT, Income CREDIT)
        val incomeTransactions = incomes.flatMap { income ->
            listOf(
                // DEBIT entry (money comes in - increases asset)
                UnifiedTransaction(
                    id = "income_${income.id}",
                    originalId = income.id,
                    sourceType = TransactionSource.INCOME,
                    amount = income.amount,
                    type = TransactionType.INCOME,
                    description = income.description,
                    category = income.category,
                    date = income.timestamp,
                    entryType = EntryType.DEBIT,
                    debitAccount = Accounts.CASH,
                    creditAccount = Accounts.INCOME
                ),
                // CREDIT entry (income earned - revenue increase)
                UnifiedTransaction(
                    id = "income_${income.id}_credit",
                    originalId = income.id,
                    sourceType = TransactionSource.INCOME,
                    amount = income.amount,
                    type = TransactionType.INCOME,
                    description = "${income.description} (Revenue)",
                    category = income.category,
                    date = income.timestamp,
                    entryType = EntryType.CREDIT,
                    debitAccount = null,
                    creditAccount = Accounts.INCOME
                )
            )
        }
        
        // Convert expenses to unified transactions (double-entry: Expense DEBIT, Cash/Credit CREDIT)
        val expenseTransactions = expenses.flatMap { expense ->
            listOf(
                // DEBIT entry (expense incurred - expense increase)
                UnifiedTransaction(
                    id = "expense_${expense.id}",
                    originalId = expense.id,
                    sourceType = TransactionSource.EXPENSE,
                    amount = expense.amount,
                    type = TransactionType.EXPENSE,
                    description = expense.merchant ?: expense.notes ?: "Expense",
                    category = expense.category.displayName,
                    date = expense.timestamp,
                    entryType = EntryType.DEBIT,
                    debitAccount = Accounts.EXPENSE,
                    creditAccount = null
                ),
                // CREDIT entry (money goes out - decreases asset)
                UnifiedTransaction(
                    id = "expense_${expense.id}_credit",
                    originalId = expense.id,
                    sourceType = TransactionSource.EXPENSE,
                    amount = expense.amount,
                    type = TransactionType.EXPENSE,
                    description = "${expense.merchant ?: expense.notes ?: "Expense"} (Payment)",
                    category = expense.category.displayName,
                    date = expense.timestamp,
                    entryType = EntryType.CREDIT,
                    debitAccount = null,
                    creditAccount = Accounts.CASH
                )
            )
        }
        
        // Convert financial transactions (double-entry)
        val financialTransactions = transactions.flatMap { ft ->
            val entries = mutableListOf<UnifiedTransaction>()
            
            when (ft.type) {
                TransactionType.INCOME -> {
                    // DEBIT: Money received (Cash/Bank or source account)
                    entries.add(UnifiedTransaction(
                        id = "ft_${ft.id}",
                        originalId = ft.id,
                        sourceType = TransactionSource.FINANCIAL,
                        amount = ft.amount,
                        type = ft.type,
                        description = ft.note.ifEmpty { ft.type.name },
                        category = ft.category ?: "Other",
                        date = ft.date,
                        entryType = EntryType.DEBIT,
                        debitAccount = ft.source.name.ifEmpty { Accounts.CASH },
                        creditAccount = null
                    ))
                    // CREDIT: Revenue recognized (Income account)
                    entries.add(UnifiedTransaction(
                        id = "ft_${ft.id}_credit",
                        originalId = ft.id,
                        sourceType = TransactionSource.FINANCIAL,
                        amount = ft.amount,
                        type = ft.type,
                        description = "${ft.note.ifEmpty { ft.type.name }} (Revenue)",
                        category = ft.category ?: "Other",
                        date = ft.date,
                        entryType = EntryType.CREDIT,
                        debitAccount = null,
                        creditAccount = Accounts.INCOME
                    ))
                }
                TransactionType.EXPENSE -> {
                    // DEBIT: Expense incurred (Expense account)
                    entries.add(UnifiedTransaction(
                        id = "ft_${ft.id}",
                        originalId = ft.id,
                        sourceType = TransactionSource.FINANCIAL,
                        amount = ft.amount,
                        type = ft.type,
                        description = ft.note.ifEmpty { ft.type.name },
                        category = ft.category ?: "Other",
                        date = ft.date,
                        entryType = EntryType.DEBIT,
                        debitAccount = Accounts.EXPENSE,
                        creditAccount = null
                    ))
                    // CREDIT: Payment made (Cash/Bank or source account)
                    entries.add(UnifiedTransaction(
                        id = "ft_${ft.id}_credit",
                        originalId = ft.id,
                        sourceType = TransactionSource.FINANCIAL,
                        amount = ft.amount,
                        type = ft.type,
                        description = "${ft.note.ifEmpty { ft.type.name }} (Payment)",
                        category = ft.category ?: "Other",
                        date = ft.date,
                        entryType = EntryType.CREDIT,
                        debitAccount = null,
                        creditAccount = ft.source.name.ifEmpty { Accounts.CASH }
                    ))
                }
                TransactionType.TRANSFER -> {
                    // DEBIT: Money received (destination account)
                    entries.add(UnifiedTransaction(
                        id = "ft_${ft.id}",
                        originalId = ft.id,
                        sourceType = TransactionSource.FINANCIAL,
                        amount = ft.amount,
                        type = ft.type,
                        description = "${ft.note.ifEmpty { "Transfer" }} - Received",
                        category = ft.category ?: Accounts.TRANSFER,
                        date = ft.date,
                        entryType = EntryType.DEBIT,
                        debitAccount = ft.destination?.name ?: Accounts.CASH,
                        creditAccount = null
                    ))
                    // CREDIT: Money sent (source account)
                    entries.add(UnifiedTransaction(
                        id = "ft_${ft.id}_credit",
                        originalId = ft.id,
                        sourceType = TransactionSource.FINANCIAL,
                        amount = ft.amount,
                        type = ft.type,
                        description = "${ft.note.ifEmpty { "Transfer" }} - Sent",
                        category = ft.category ?: Accounts.TRANSFER,
                        date = ft.date,
                        entryType = EntryType.CREDIT,
                        debitAccount = null,
                        creditAccount = ft.source.name.ifEmpty { Accounts.CASH }
                    ))
                }
                else -> {
                    // Default handling for other transaction types
                    entries.add(UnifiedTransaction(
                        id = "ft_${ft.id}",
                        originalId = ft.id,
                        sourceType = TransactionSource.FINANCIAL,
                        amount = ft.amount,
                        type = ft.type,
                        description = ft.note.ifEmpty { ft.type.name },
                        category = ft.category ?: "Other",
                        date = ft.date,
                        entryType = EntryType.DEBIT,
                        debitAccount = ft.source.name.ifEmpty { Accounts.CASH },
                        creditAccount = ft.destination?.name
                    ))
                }
            }
            entries
        }
        
        // Combine all transactions, restricted to the selected date range (if any) so the
        // totals describe the same period as the list
        val startMillis = params.startDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        val endMillis = params.endDate?.plusDays(1)?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        val allTransactions = (incomeTransactions + expenseTransactions + financialTransactions)
            .filter { (startMillis == null || it.date >= startMillis) && (endMillis == null || it.date < endMillis) }
            .sortedByDescending { it.date }
        
        // Calculate totals - using credit entries for income, debit entries for expenses
        val totalIncome = allTransactions.filter { 
            it.type == TransactionType.INCOME || it.type == TransactionType.LOAN_RECEIVE 
        }.filter { it.entryType == EntryType.CREDIT }.sumOf { it.amount }
        
        val totalExpenses = allTransactions.filter { 
            it.type == TransactionType.EXPENSE || it.type == TransactionType.EMI_PAID 
        }.filter { it.entryType == EntryType.DEBIT }.sumOf { it.amount }

        // Apply type filters
        var filtered = allTransactions

        filtered = when (params.filter) {
            TransactionFilter.INCOME -> filtered.filter { 
                (it.type == TransactionType.INCOME || it.type == TransactionType.LOAN_RECEIVE) && it.entryType == EntryType.CREDIT
            }
            TransactionFilter.EXPENSE -> filtered.filter { 
                (it.type == TransactionType.EXPENSE || it.type == TransactionType.EMI_PAID) && it.entryType == EntryType.DEBIT
            }
            else -> filtered
        }

        // Apply Limit
        val limitedTransactions = filtered.take(params.limit)

        FinancialsUiState(
            transactions = limitedTransactions,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netFlow = totalIncome - totalExpenses,
            isLoading = false,
            filter = params.filter,
            startDate = params.startDate,
            endDate = params.endDate,
            limit = params.limit
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialsUiState(isLoading = true)
    )

    init {
        // Collect from the stateFlow and update UI state with proper lifecycle handling
        viewModelScope.launch {
            transactions.catch { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setFilter(filter: TransactionFilter) {
        filterFlow.value = filter
    }

    fun setDateRange(start: LocalDate?, end: LocalDate?) {
        startDateFlow.value = start
        endDateFlow.value = end
        // Don't change filter to DATE_RANGE - keep the current filter but apply date range
    }

    fun setLimit(newLimit: Int) {
        limitFlow.value = newLimit
    }

    /** Deletes the source record; works from either the debit or the credit row of a pair. */
    fun deleteTransaction(transaction: UnifiedTransaction) {
        viewModelScope.launch {
            try {
                when (transaction.sourceType) {
                    TransactionSource.FINANCIAL -> {
                        // FinancialTransaction.id is Int
                        val id = transaction.originalId
                        if (id is Int) {
                            transactionRepository.deleteTransactionById(id)
                        } else if (id is Long) {
                            transactionRepository.deleteTransactionById(id.toInt())
                        }
                    }
                    TransactionSource.INCOME -> {
                        // Income.id is Long
                        val id = transaction.originalId
                        if (id is Long) {
                            incomeRepository.deleteIncomeById(id)
                        } else if (id is Int) {
                            incomeRepository.deleteIncomeById(id.toLong())
                        }
                    }
                    TransactionSource.EXPENSE -> {
                        val id = transaction.originalId
                        expenseRepository.deleteExpenseById(id.toString())
                    }
                }
                _snackbarMessage.emit("Transaction deleted successfully")
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to delete transaction: ${e.message}")
            }
        }
    }
}

/**
 * Helper data class for filter parameters
 */
data class FilterParams(
    val filter: TransactionFilter,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val limit: Int
)
