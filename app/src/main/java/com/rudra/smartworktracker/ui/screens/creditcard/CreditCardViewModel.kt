package com.rudra.smartworktracker.ui.screens.creditcard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.smartworktracker.data.AppDatabase
import com.rudra.smartworktracker.data.entity.AccountType
import com.rudra.smartworktracker.data.entity.CreditCard
import com.rudra.smartworktracker.data.entity.CreditCardTransaction
import com.rudra.smartworktracker.data.entity.FinancialTransaction
import com.rudra.smartworktracker.data.entity.TransactionType
import com.rudra.smartworktracker.utils.CurrencyManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CreditCardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val creditCardDao = db.creditCardDao()
    private val creditCardTransactionDao = db.creditCardTransactionDao()
    private val financialTransactionDao = db.financialTransactionDao()

    val creditCards: StateFlow<List<CreditCard>> = creditCardDao.getAllCards()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun transactionsFor(cardId: Int): Flow<List<CreditCardTransaction>> =
        creditCardTransactionDao.getTransactionsForCard(cardId)

    fun addCreditCard(card: CreditCard) {
        viewModelScope.launch {
            creditCardDao.insertCard(card)
            _messages.tryEmit("${card.cardName} added")
        }
    }

    fun deleteCreditCard(card: CreditCard) {
        viewModelScope.launch {
            creditCardDao.deleteCard(card)
            _messages.tryEmit("${card.cardName} removed")
        }
    }

    /** Records a purchase; always works from the latest stored balance, not a stale UI copy. */
    fun addCardTransaction(card: CreditCard, amount: Double, description: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            val current = creditCardDao.getCardById(card.id).first() ?: return@launch
            val timestamp = System.currentTimeMillis()
            creditCardDao.updateCard(
                current.copy(currentBalance = current.currentBalance + amount, updatedAt = timestamp)
            )

            creditCardTransactionDao.insertTransaction(
                CreditCardTransaction(
                    cardId = card.id,
                    amount = amount,
                    description = description.ifBlank { "Purchase" },
                    date = timestamp
                )
            )

            financialTransactionDao.insertTransaction(
                FinancialTransaction(
                    type = TransactionType.EXPENSE, // A purchase with a credit card is an expense
                    amount = amount,
                    source = AccountType.CREDIT_CARD,
                    destination = null,
                    note = description.ifBlank { "Purchase on ${card.cardName}" },
                    date = timestamp
                )
            )

            val newBalance = current.currentBalance + amount
            _messages.tryEmit(
                if (newBalance > current.cardLimit) "Purchase recorded — ${card.cardName} is over its limit"
                else "Purchase of ${CurrencyManager.format(amount)} recorded"
            )
        }
    }

    /** Pays the bill; the payment is capped at the outstanding balance and logged in card history. */
    fun payCreditCardBill(card: CreditCard, amount: Double) {
        if (amount <= 0) return
        viewModelScope.launch {
            val current = creditCardDao.getCardById(card.id).first() ?: return@launch
            val payment = amount.coerceAtMost(current.currentBalance)
            if (payment <= 0) {
                _messages.tryEmit("Nothing to pay on ${card.cardName}")
                return@launch
            }
            val timestamp = System.currentTimeMillis()
            creditCardDao.updateCard(
                current.copy(currentBalance = current.currentBalance - payment, updatedAt = timestamp)
            )

            // Negative amount = payment, so the card history shows both purchases and payments
            creditCardTransactionDao.insertTransaction(
                CreditCardTransaction(
                    cardId = card.id,
                    amount = -payment,
                    description = "Bill payment",
                    date = timestamp
                )
            )

            financialTransactionDao.insertTransaction(
                FinancialTransaction(
                    type = TransactionType.TRANSFER,
                    amount = payment,
                    source = AccountType.BALANCE,
                    destination = AccountType.CREDIT_CARD,
                    note = "Paid bill for ${card.cardName}",
                    date = timestamp
                )
            )
            _messages.tryEmit("Paid ${CurrencyManager.format(payment)} to ${card.cardName}")
        }
    }
}
