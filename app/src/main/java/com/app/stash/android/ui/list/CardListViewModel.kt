package com.app.stash.android.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.stash.android.data.plaid.PlaidRepository
import com.app.stash.android.data.plaid.plaidErrorMessage
import com.app.stash.android.data.repository.CreditCardRepository
import com.app.stash.android.domain.model.CreditCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BankLinkEvent {
    data class StartLink(val linkToken: String) : BankLinkEvent
    data class Message(val text: String) : BankLinkEvent
    data object OpenPlaidSetup : BankLinkEvent
}

@HiltViewModel
class CardListViewModel @Inject constructor(
    private val repository: CreditCardRepository,
    private val plaid: PlaidRepository,
) : ViewModel() {

    val cards: StateFlow<List<CreditCard>> = repository.observeCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableStateFlow<BankLinkEvent?>(null)
    val events: StateFlow<BankLinkEvent?> = _events.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _institutionLogo = MutableStateFlow<String?>(null)
    val institutionLogo: StateFlow<String?> = _institutionLogo.asStateFlow()

    val plaidConfigured: Boolean get() = plaid.isConfigured

    init {
        viewModelScope.launch {
            _institutionLogo.value = plaid.institutionLogoBase64()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _busy.value = true
            repository.refreshFromRemote()
            if (plaid.isLinked()) {
                plaid.sync()
                _institutionLogo.value = plaid.institutionLogoBase64()
            }
            _busy.value = false
        }
    }

    fun connectBank() {
        if (!plaid.isConfigured) {
            _events.value = BankLinkEvent.OpenPlaidSetup
            return
        }
        viewModelScope.launch {
            _busy.value = true
            plaid.createLinkToken()
                .onSuccess { _events.value = BankLinkEvent.StartLink(it) }
                .onFailure { _events.value = BankLinkEvent.Message("Couldn't start Plaid: ${it.plaidErrorMessage()}") }
            _busy.value = false
        }
    }

    fun onLinkSuccess(publicToken: String) {
        viewModelScope.launch {
            _busy.value = true
            plaid.completeLink(publicToken)
                .onSuccess { count ->
                    _events.value = BankLinkEvent.Message("Imported $count card(s).")
                    _institutionLogo.value = plaid.institutionLogoBase64()
                }
                .onFailure { _events.value = BankLinkEvent.Message("Sync failed: ${it.plaidErrorMessage()}") }
            _busy.value = false
        }
    }

    fun onLinkExit(message: String?) {
        if (!message.isNullOrBlank()) _events.value = BankLinkEvent.Message(message)
    }

    fun deleteCard(id: Long) {
        viewModelScope.launch {
            repository.deleteCard(id)
        }
    }

    fun consumeEvent() { _events.value = null }
}
