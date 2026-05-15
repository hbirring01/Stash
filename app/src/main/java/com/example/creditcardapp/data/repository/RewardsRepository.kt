package com.example.creditcardapp.data.repository

import com.example.creditcardapp.data.local.RewardBalanceDao
import com.example.creditcardapp.data.local.RotatingCategoryDao
import com.example.creditcardapp.data.mapper.toDomain
import com.example.creditcardapp.data.mapper.toEntity
import com.example.creditcardapp.domain.model.RewardBalance
import com.example.creditcardapp.domain.model.RotatingCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistence for the "rewards hub": manually-entered points balances and
 * time-bounded rotating bonus categories.
 */
@Singleton
class RewardsRepository @Inject constructor(
    private val balanceDao: RewardBalanceDao,
    private val rotatingDao: RotatingCategoryDao,
) {
    // --- Reward balances (points) ---

    fun observeBalances(): Flow<List<RewardBalance>> =
        balanceDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeBalancesForCard(cardId: Long): Flow<List<RewardBalance>> =
        balanceDao.observeForCard(cardId).map { list -> list.map { it.toDomain() } }

    suspend fun saveBalance(balance: RewardBalance): Long =
        balanceDao.insert(balance.toEntity())

    suspend fun deleteBalance(id: Long) = balanceDao.deleteById(id)

    // --- Rotating categories ---

    fun observeRotating(): Flow<List<RotatingCategory>> =
        rotatingDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActiveRotating(now: Long = System.currentTimeMillis()): Flow<List<RotatingCategory>> =
        rotatingDao.observeActive(now).map { list -> list.map { it.toDomain() } }

    suspend fun saveRotating(category: RotatingCategory): Long =
        rotatingDao.insert(category.toEntity())

    suspend fun deleteRotating(id: Long) = rotatingDao.deleteById(id)
}
