package com.app.stash.android.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.stash.android.data.local.TransactionDao
import com.app.stash.android.data.local.TransactionEntity
import com.app.stash.android.data.plaid.PlaidRepository
import com.app.stash.android.data.repository.CreditCardRepository
import com.app.stash.android.domain.model.CreditCard
import com.app.stash.android.ui.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardRepository: CreditCardRepository,
    private val plaidRepository: PlaidRepository,
    private val transactionDao: TransactionDao,
) : ViewModel() {

    private val cardId: Long = checkNotNull(savedStateHandle[Destination.Transactions.ARG_CARD_ID])

    private val _card = MutableStateFlow<CreditCard?>(null)
    val card: StateFlow<CreditCard?> = _card.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val transactions: StateFlow<List<TransactionEntity>> = _card
        .flatMapLatest { c ->
            val acct = c?.sourceAccountId
            if (acct.isNullOrBlank()) flowOf(emptyList()) else transactionDao.observeByAccount(acct)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _card.value = cardRepository.getCard(cardId)
            // Auto-refresh if we have a linked account
            if (_card.value?.sourceAccountId != null && plaidRepository.isLinked()) {
                refresh()
            }
        }
    }

    fun refresh() {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            val result = plaidRepository.syncTransactions()
            result.onFailure { _error.value = it.message ?: "Failed to sync transactions" }
            _busy.value = false
        }
    }

    fun clearError() { _error.value = null }
}
