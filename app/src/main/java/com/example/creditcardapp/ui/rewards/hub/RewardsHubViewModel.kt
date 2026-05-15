package com.example.creditcardapp.ui.rewards.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.creditcardapp.data.local.TransactionDao
import com.example.creditcardapp.data.local.TransactionEntity
import com.example.creditcardapp.data.repository.CreditCardRepository
import com.example.creditcardapp.data.repository.RewardsRepository
import com.example.creditcardapp.domain.model.CreditCard
import com.example.creditcardapp.domain.model.RewardBalance
import com.example.creditcardapp.domain.model.RewardCategory
import com.example.creditcardapp.domain.model.RotatingCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/** Per-card ROI snapshot for the year-to-date period. */
data class CardRoi(
    val card: CreditCard,
    /** Total spend on this card (year-to-date), in dollars. */
    val ytdSpend: Double,
    /** Estimated dollar value of rewards earned year-to-date. */
    val ytdRewardValue: Double,
    /** Sign-up bonus dollar value attributed to this year. */
    val signupValue: Double,
    /** Net = rewards + signup − annualFee. */
    val net: Double,
    /** Suggestion text: "Keep", "Cancel", "Watch". */
    val verdict: String,
)

data class RewardsHubState(
    val cards: List<CreditCard> = emptyList(),
    val balances: List<RewardBalance> = emptyList(),
    val rotating: List<RotatingCategory> = emptyList(),
    val roi: List<CardRoi> = emptyList(),
)

@HiltViewModel
class RewardsHubViewModel @Inject constructor(
    private val cardsRepo: CreditCardRepository,
    private val rewardsRepo: RewardsRepository,
    private val transactionDao: TransactionDao,
) : ViewModel() {

    val state: StateFlow<RewardsHubState> = combine(
        cardsRepo.observeCards(),
        rewardsRepo.observeBalances(),
        rewardsRepo.observeRotating(),
        transactionDao.observeAll(),
    ) { cards, balances, rotating, txs ->
        RewardsHubState(
            cards = cards,
            balances = balances,
            rotating = rotating,
            roi = computeRoi(cards, txs),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RewardsHubState())

    fun saveBalance(b: RewardBalance) {
        viewModelScope.launch { rewardsRepo.saveBalance(b) }
    }

    fun deleteBalance(id: Long) {
        viewModelScope.launch { rewardsRepo.deleteBalance(id) }
    }

    private fun computeRoi(cards: List<CreditCard>, txs: List<TransactionEntity>): List<CardRoi> {
        val yearStart = yearStartIso()
        // Group transactions by accountId once.
        val byAccount = txs.groupBy { it.accountId }
        return cards.map { card ->
            val acctId = card.sourceAccountId
            val accountTxs = acctId?.let { byAccount[it] }.orEmpty()
                .filter { it.date >= yearStart && !it.pending }
            // Plaid amounts: positive = spend, negative = credit/refund. We treat
            // positive amounts as eligible spend.
            var ytdSpend = 0.0
            var ytdRewardValue = 0.0
            accountTxs.forEach { tx ->
                if (tx.amount <= 0.0) return@forEach
                val cat = RewardCategory.fromPlaidPrimary(tx.categoryPrimary)
                val mult = card.multiplierFor(cat)
                ytdSpend += tx.amount
                // Points earned = spend × multiplier; value = points × pointValueCents / 100
                ytdRewardValue += tx.amount * mult * (card.pointValueCents / 100.0)
            }
            val signupValue = if (card.signupBonusEarnedSpend >= card.signupBonusRequiredSpend &&
                card.signupBonusRequiredSpend > 0.0
            ) card.signupBonusValue else 0.0
            val net = ytdRewardValue + signupValue - card.annualFee
            val verdict = when {
                card.annualFee == 0.0 -> "Keep"
                net >= card.annualFee * 0.5 -> "Keep"
                net >= 0.0 -> "Watch"
                else -> "Cancel"
            }
            CardRoi(
                card = card,
                ytdSpend = ytdSpend,
                ytdRewardValue = ytdRewardValue,
                signupValue = signupValue,
                net = net,
                verdict = verdict,
            )
        }
    }

    private fun yearStartIso(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val y = cal.get(Calendar.YEAR)
        return "%04d-01-01".format(y)
    }
}
