package com.example.creditcardapp.data.plaid

import com.example.creditcardapp.data.local.CreditCardDao
import com.example.creditcardapp.data.local.TransactionDao
import com.example.creditcardapp.data.mapper.toCreditCardEntities
import com.example.creditcardapp.data.mapper.toEntity
import com.example.creditcardapp.data.repository.StatementCreditAutoMatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaidRepository @Inject constructor(
    private val api: PlaidApi,
    private val tokenStore: PlaidTokenStore,
    private val credentialsStore: PlaidCredentialsStore,
    private val dao: CreditCardDao,
    private val transactionDao: TransactionDao,
    private val statementCreditAutoMatcher: StatementCreditAutoMatcher,
) {
    /** True once the user has saved Plaid credentials via the in-app setup screen. */
    val isConfigured: Boolean
        get() = credentialsStore.hasCredentials()

    private suspend fun creds(): Pair<String, String> {
        val id = credentialsStore.clientId()
        val secret = credentialsStore.secret()
        require(!id.isNullOrBlank() && !secret.isNullOrBlank()) {
            "Plaid credentials are not set. Long-press the Wallet title to add them."
        }
        return id to secret
    }

    suspend fun createLinkToken(userId: String = "local-user"): Result<String> = runCatching {
        val (clientId, secret) = creds()
        val resp = api.createLinkToken(
            LinkTokenCreateRequest(
                clientId = clientId,
                secret = secret,
                clientName = "CreditCardApp",
                user = PlaidUser(clientUserId = userId),
            )
        )
        resp.linkToken
    }

    suspend fun completeLink(publicToken: String): Result<Int> = runCatching {
        val (clientId, secret) = creds()
        val exch = api.exchangePublicToken(
            PublicTokenExchangeRequest(
                clientId = clientId,
                secret = secret,
                publicToken = publicToken,
            )
        )
        tokenStore.save(exch.accessToken, exch.itemId)
        val count = syncLiabilitiesInternal(exch.accessToken, exch.itemId)
        runCatching { syncTransactionsInternal(exch.accessToken, exch.itemId) }
        count
    }

    suspend fun institutionLogoBase64(): String? {
        val itemId = tokenStore.itemId() ?: return null
        return tokenStore.institutionLogo(itemId)
    }

    suspend fun sync(): Result<Int> = runCatching {
        creds()
        val token = tokenStore.accessToken() ?: error("No bank linked yet.")
        val itemId = tokenStore.itemId().orEmpty()
        syncLiabilitiesInternal(token, itemId)
    }

    suspend fun syncTransactions(): Result<Int> = runCatching {
        creds()
        val token = tokenStore.accessToken() ?: error("No bank linked yet.")
        val itemId = tokenStore.itemId().orEmpty()
        syncTransactionsInternal(token, itemId)
    }

    suspend fun unlink() {
        val itemId = tokenStore.itemId()
        if (!itemId.isNullOrBlank()) {
            dao.deleteBySourceItemId(itemId)
            transactionDao.deleteByItemId(itemId)
        }
        tokenStore.clear()
    }

    suspend fun isLinked(): Boolean = !tokenStore.accessToken().isNullOrBlank()

    private suspend fun syncLiabilitiesInternal(accessToken: String, itemId: String): Int {
        val (clientId, secret) = creds()
        val resp = api.getLiabilities(
            LiabilitiesGetRequest(
                clientId = clientId,
                secret = secret,
                accessToken = accessToken,
            )
        )
        val resolvedItemId = resp.item?.itemId ?: itemId
        val institutionId = resp.item?.institutionId
        val logo = institutionId?.let { fetchInstitutionLogo(it) }
        if (institutionId != null || logo != null) {
            tokenStore.saveInstitution(resolvedItemId, institutionId, logo)
        }
        val incoming = resp.toCreditCardEntities(resolvedItemId)

        incoming.forEach { newRow ->
            val existing = newRow.sourceAccountId?.let { dao.getBySourceAccountId(it) }
            if (existing == null) {
                dao.insert(newRow)
            } else {
                dao.update(
                    newRow.copy(
                        id = existing.id,
                        nickname = existing.nickname ?: newRow.nickname,
                    )
                )
            }
        }
        return incoming.size
    }

    private suspend fun fetchInstitutionLogo(institutionId: String): String? = runCatching {
        val (clientId, secret) = creds()
        api.getInstitutionById(
            InstitutionGetRequest(
                clientId = clientId,
                secret = secret,
                institutionId = institutionId,
            )
        ).institution?.logo
    }.getOrNull()

    private suspend fun syncTransactionsInternal(accessToken: String, itemId: String): Int {
        var cursor = tokenStore.transactionsCursor(itemId)
        var totalAdded = 0
        var hasMore = true
        while (hasMore) {
            val (clientId, secret) = creds()
            val resp = api.syncTransactions(
                TransactionsSyncRequest(
                    clientId = clientId,
                    secret = secret,
                    accessToken = accessToken,
                    cursor = cursor,
                )
            )
            val upserts = (resp.added + resp.modified).map { it.toEntity(itemId) }
            if (upserts.isNotEmpty()) {
                transactionDao.upsertAll(upserts)
                // Auto-log matching transactions against any statement
                // credits attached to the affected card.
                statementCreditAutoMatcher.matchTransactions(upserts)
            }
            val removedIds = resp.removed.map { it.transactionId }
            if (removedIds.isNotEmpty()) transactionDao.deleteByIds(removedIds)
            totalAdded += resp.added.size
            cursor = resp.nextCursor
            hasMore = resp.hasMore
            tokenStore.saveTransactionsCursor(itemId, cursor)
        }
        return totalAdded
    }
}
