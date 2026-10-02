package com.app.stash.android.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.stash.android.data.repository.CreditCardRepository
import com.app.stash.android.domain.model.CreditCard
import com.app.stash.android.domain.model.RewardCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddCardUiState(
    val nickname: String = "",
    val last4: String = "",
    val creditLimit: String = "",
    val balance: String = "",
    /** Bonus multiplier per category. Missing categories default to 1x. */
    val rewards: Map<RewardCategory, Double> = emptyMap(),
    val saving: Boolean = false,
    val error: String? = null
) {
    val canSave: Boolean
        get() = last4.length == 4 && last4.all { it.isDigit() } &&
            creditLimit.toDoubleOrNull() != null
}

@HiltViewModel
class AddCardViewModel @Inject constructor(
    private val repository: CreditCardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddCardUiState())
    val state: StateFlow<AddCardUiState> = _state.asStateFlow()

    fun update(transform: (AddCardUiState) -> AddCardUiState) {
        _state.update(transform)
    }

    /**
     * Set the bonus multiplier for [category]. Passing a value of 1.0 or less
     * removes the entry (1x is the implicit default, no need to store it).
     */
    fun setMultiplier(category: RewardCategory, multiplier: Double) {
        _state.update { s ->
            val next = s.rewards.toMutableMap()
            if (multiplier <= 1.0) next.remove(category) else next[category] = multiplier
            s.copy(rewards = next)
        }
    }

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                repository.saveCard(
                    CreditCard(
                        cardholderName = "",
                        last4 = s.last4,
                        brand = brandFromLast4(s.last4),
                        expiryMonth = 0,
                        expiryYear = 0,
                        balance = s.balance.toDoubleOrNull() ?: 0.0,
                        creditLimit = s.creditLimit.toDouble(),
                        nickname = s.nickname.trim().ifBlank { null },
                        rewards = s.rewards,
                    )
                )
            }.onSuccess {
                _state.update { it.copy(saving = false) }
                onSaved()
            }.onFailure { e ->
                _state.update { it.copy(saving = false, error = e.message) }
            }
        }
    }
}

private fun brandFromLast4(last4: String): String = when (last4.firstOrNull()) {
    '4' -> "Visa"
    '5', '2' -> "Mastercard"
    '3' -> "Amex"
    '6' -> "Discover"
    else -> "Card"
}
