package com.app.stash.android.data.ai

import android.util.Log
import com.app.stash.android.data.local.MerchantCategoryCacheDao
import com.app.stash.android.data.local.MerchantCategoryCacheEntity
import com.app.stash.android.data.local.normalizeMerchant
import com.app.stash.android.data.preferences.AiSettingsStore
import com.app.stash.android.domain.model.RewardCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Asks an OpenAI-compatible LLM (Gemini Flash by default) which high-level
 * [RewardCategory] best describes a merchant. Called as a last-resort tier by
 * [com.app.stash.android.data.repository.TransactionCategorizer] after Plaid's
 * own category and the local [MerchantCategoryRules] both fail to produce a
 * non-OTHER answer.
 *
 * Caches verdicts in [MerchantCategoryCacheDao] keyed by normalized merchant,
 * so the same merchant string never burns quota twice across syncs.
 *
 * Returns `null` (not OTHER) when AI is disabled, over budget, or the call
 * fails — callers can fall back to OTHER themselves and avoid persisting a
 * spurious cache row.
 */
@Singleton
class AiCategoryClient @Inject constructor(
    private val settings: AiSettingsStore,
    private val cache: MerchantCategoryCacheDao,
    private val http: OkHttpClient,
    private val json: Json,
) {
    /**
     * @return [RewardCategory] from cache or LLM, or null when unavailable.
     */
    suspend fun categorize(
        merchant: String,
        plaidCategory: String?,
        networkBudget: AiBudget? = null,
    ): RewardCategory? {
        if (merchant.isBlank()) return null
        val key = normalizeMerchant(merchant)
        if (key.isEmpty()) return null

        cache.lookup(key)?.let { return parseCategory(it) }

        if (!settings.isEnabled()) return null
        if (networkBudget != null && !networkBudget.consume()) return null

        val verdict = askModel(merchant, plaidCategory) ?: return null
        cache.upsert(
            MerchantCategoryCacheEntity(
                merchantNorm = key,
                category = verdict.name,
                fromAi = true,
                modelVersion = settings.effectiveModel(),
                createdAt = System.currentTimeMillis(),
            )
        )
        return verdict
    }

    /** Persist a rule-derived verdict in the cache so future syncs skip rule scans. */
    suspend fun cacheRuleVerdict(merchant: String, category: RewardCategory) {
        val key = normalizeMerchant(merchant)
        if (key.isEmpty()) return
        cache.upsert(
            MerchantCategoryCacheEntity(
                merchantNorm = key,
                category = category.name,
                fromAi = false,
                modelVersion = "rules",
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    private suspend fun askModel(
        merchant: String,
        plaidCategory: String?,
    ): RewardCategory? = withContext(Dispatchers.IO) {
        val baseUrl = settings.effectiveBaseUrl()
        val apiKey = settings.effectiveApiKey()
        val model = settings.effectiveModel()
        if (baseUrl.isBlank() || apiKey.isBlank() || model.isBlank()) return@withContext null

        val prompt = buildString {
            append("Categorize this credit-card transaction into exactly one bucket.\n\n")
            append("Merchant: \"").append(merchant).append("\"\n")
            append("Plaid hint: ").append(plaidCategory ?: "UNKNOWN").append("\n\n")
            append("Allowed answers (reply with exactly one word, uppercase):\n")
            append("DINING, GROCERIES, GAS, TRAVEL, SHOPPING, ENTERTAINMENT, OTHER\n\n")
            append("Reply with only the single word.")
        }

        val body = CatChatRequest(
            model = model,
            temperature = 0.0,
            maxTokens = 4,
            messages = listOf(CatChatMessage(role = "user", content = prompt)),
        )
        val bodyJson = json.encodeToString(CatChatRequest.serializer(), body)

        val request = Request.Builder()
            .url(baseUrl + "chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            var attempt = 0
            while (true) {
                val resp = http.newCall(request).execute()
                val shouldRetry = resp.code == 429 || resp.code == 503
                if (!shouldRetry || attempt >= MAX_RETRIES) {
                    resp.use { r ->
                        if (!r.isSuccessful) {
                            Log.w(TAG, "AI request failed: HTTP ${r.code}")
                            return@withContext null
                        }
                        val text = r.body?.string().orEmpty()
                        val parsed = runCatching { json.decodeFromString(CatChatResponse.serializer(), text) }
                            .getOrNull() ?: return@withContext null
                        val answer = parsed.choices.firstOrNull()?.message?.content
                            ?.trim()
                            ?.uppercase()
                            ?: return@withContext null
                        return@withContext parseCategory(answer)
                    }
                }
                val retryAfter = resp.header("Retry-After")?.toLongOrNull()?.times(1000L)
                resp.close()
                val delayMs = retryAfter ?: (BASE_BACKOFF_MS shl attempt)
                Log.w(TAG, "AI ${if (shouldRetry) "throttled" else "errored"} (attempt ${attempt + 1}); backing off ${delayMs}ms")
                delay(delayMs)
                attempt++
            }
            @Suppress("UNREACHABLE_CODE")
            null
        } catch (t: Throwable) {
            Log.w(TAG, "AI request threw: ${t.message}")
            null
        }
    }

    private fun parseCategory(raw: String): RewardCategory? {
        val trimmed = raw.trim().uppercase()
        // Tolerate models that wrap the answer in quotes or punctuation.
        val word = trimmed.takeWhile { it.isLetter() || it == '_' }
        return RewardCategory.entries.firstOrNull { it.name == word }
    }

    private companion object {
        const val TAG = "AiCategoryClient"
        const val MAX_RETRIES = 3
        const val BASE_BACKOFF_MS = 500L
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

// ---- OpenAI Chat Completions wire format (file-private, unique-named) ----
// Kotlin 2.x flags identically-named top-level `private` declarations in the
// same package as redeclarations across files, so we prefix these with `Cat`
// to distinguish them from the corresponding types in [AiMatchClient].

@Serializable
private data class CatChatRequest(
    val model: String,
    val messages: List<CatChatMessage>,
    val temperature: Double = 0.0,
    @kotlinx.serialization.SerialName("max_tokens") val maxTokens: Int = 4,
)

@Serializable
private data class CatChatMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class CatChatResponse(
    val choices: List<CatChatChoice> = emptyList(),
)

@Serializable
private data class CatChatChoice(
    val message: CatChatMessage? = null,
)
