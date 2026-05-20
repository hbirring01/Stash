package com.app.stash.android.ui.credits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.stash.android.data.repository.CreditCardRepository
import com.app.stash.android.data.repository.StatementCreditsRepository
import com.app.stash.android.domain.model.CreditCard
import com.app.stash.android.domain.model.StatementCredit
import com.app.stash.android.domain.model.StatementCreditUsage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for a single credit row: the underlying definition plus the
 * amount used in the current period (so the row can render a progress bar
 * without each composable doing its own period math).
 */
data class CreditUiState(
    val credit: StatementCredit,
    val card: CreditCard?,
    val usedInPeriod: Double,
) {
    val remaining: Double get() = (credit.amountDollars - usedInPeriod).coerceAtLeast(0.0)
    val percentUsed: Float
        get() = if (credit.amountDollars <= 0.0) 0f
        else (usedInPeriod / credit.amountDollars).toFloat().coerceIn(0f, 1f)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatementCreditsViewModel @Inject constructor(
    private val repository: StatementCreditsRepository,
    private val cardRepository: CreditCardRepository,
) : ViewModel() {

    private val cardsById: StateFlow<Map<Long, CreditCard>> = cardRepository.observeCards()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Flat card list for the editor dialog's card picker. */
    val cards: StateFlow<List<CreditCard>> = cardRepository.observeCards()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Eagerly compute usedInPeriod for every credit. We re-emit on either the
     * list of credits changing OR any single credit's usage changing, so the
     * progress bars stay live as users tap "Mark used".
     */
    val credits: StateFlow<List<CreditUiState>> = repository.observeAll()
        .flatMapLatest { list ->
            if (list.isEmpty()) flowOf(emptyList())
            else combine(list.map { credit -> creditWithUsage(credit) }) { it.toList() }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun creditWithUsage(credit: StatementCredit): Flow<CreditUiState> {
        val window = credit.periodWindow()
        return combine(
            repository.observeUsedInPeriod(credit.id, window.first, window.last + 1),
            cardsById,
        ) { used, cards ->
            CreditUiState(
                credit = credit,
                card = cards[credit.cardId],
                usedInPeriod = used,
            )
        }
    }

    fun observeUsages(creditId: Long, period: LongRange): Flow<List<StatementCreditUsage>> =
        repository.observeUsagesInPeriod(creditId, period.first, period.last + 1)

    fun saveCredit(credit: StatementCredit) {
        viewModelScope.launch { repository.save(credit) }
    }

    fun deleteCredit(id: Long) {
        viewModelScope.launch { repository.deleteCredit(id) }
    }

    fun logUsage(creditId: Long, amount: Double, description: String?) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            repository.logUsage(
                StatementCreditUsage(
                    creditId = creditId,
                    amountDollars = amount,
                    usedAt = System.currentTimeMillis(),
                    description = description?.takeIf { it.isNotBlank() },
                )
            )
        }
    }

    fun deleteUsage(id: Long) {
        viewModelScope.launch { repository.deleteUsage(id) }
    }
}
