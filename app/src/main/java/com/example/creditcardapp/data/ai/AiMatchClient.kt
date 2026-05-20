package com.example.creditcardapp.data.ai

import android.util.Log
import com.example.creditcardapp.data.local.AiMatchCacheDao
import com.example.creditcardapp.data.local.AiMatchCacheEntity
import com.example.creditcardapp.data.local.normalizeMerchant
import com.example.creditcardapp.data.preferences.AiSettingsStore
import com.example.creditcardapp.domain.model.StatementCredit
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
 * Asks an OpenAI-compatible LLM (Gemini Flash by default — see [AiProvider])
 * whether a Plaid transaction qualifies for a given statement credit, when the
 * deterministic pattern/category rules have already missed.
 *
 * Wire format: standard OpenAI Chat Completions — the same request body works
 * against Gemini's compat endpoint, Groq, OpenRouter, Together, vLLM, Ollama,
 * etc. Auth is `Authorization: Bearer <key>`.
 *
 * Returns `null` (not `false`) when AI is disabled, misconfigured, or errors,
 * so callers can distinguish "model said no" from "couldn't ask the model".
 * Conservative: callers treat `null` and `false` identically — only an explicit
 * `true` should auto-log a usage.
 *
 * Side-effects:
 *  - Caches the verdict (per credit, per normalized merchant) in [AiMatchCacheDao]
 *    so repeated transactions from the same merchant don't burn quota.
 *  - Soft per-batch ceiling enforced by the caller via [budgetRemaining].
 */
@Singleton
class AiMatchClient @Inject constructor(
    private val settings: AiSettingsStore,
    private val cache: AiMatchCacheDao,
    private val http: OkHttpClient,
    private val json: Json,
) {
    /**
     * @param networkBudget optional gate consulted only on cache misses; if the
     *        budget is exhausted the call returns `null` without invoking the
     *        model. Cache hits never touch the budget.
     * @return true iff the model answered "YES". `false` for explicit NO,
     *         `null` for disabled / over-budget / network error / unparseable
     *         response.
     */
    suspend fun matchesCredit(
        credit: StatementCredit,
        merchant: String,
        category: String?,
        networkBudget: AiBudget? = null,
    ): Boolean? {
        if (!settings.isEnabled()) return null
        if (merchant.isBlank()) return null

        val key = normalizeMerchant(merchant)
        if (key.isEmpty()) return null

        cache.lookup(credit.id, key)?.let { return it }
        if (networkBudget != null && !networkBudget.consume()) return null

        val verdict = askModel(credit, merchant, category) ?: return null
        cache.upsert(
            AiMatchCacheEntity(
                creditId = credit.id,
                merchantNorm = key,
                matched = verdict,
                modelVersion = settings.effectiveModel(),
                createdAt = System.currentTimeMillis(),
            )
        )
        return verdict
    }

    /** Wipe cache entries for a credit (called when its rules change). */
    suspend fun invalidate(creditId: Long) = cache.clearForCredit(creditId)

    private suspend fun askModel(
        credit: StatementCredit,
        merchant: String,
        category: String?,
    ): Boolean? = withContext(Dispatchers.IO) {
        val baseUrl = settings.effectiveBaseUrl()
        val key = settings.effectiveApiKey()
        val model = settings.effectiveModel()
        if (baseUrl.isBlank() || key.isBlank() || model.isBlank()) return@withContext null

        val rulesSummary = buildString {
            if (!credit.matchPattern.isNullOrBlank()) {
                append("merchant matches one of: ").append(credit.matchPattern)
            }
            if (!credit.matchCategory.isNullOrBlank()) {
                if (isNotEmpty()) append(" OR ")
                append("category = ").append(credit.matchCategory)
            }
            if (isEmpty()) append("(no explicit rules — use the credit name as a guide)")
        }
        val prompt = buildString {
            append("You are matching credit-card statement credits to bank transactions.\n\n")
            append("Credit: \"").append(credit.name).append("\"\n")
            append("Rules: ").append(rulesSummary).append("\n\n")
            append("Transaction merchant: \"").append(merchant).append("\"\n")
            append("Transaction category: ").append(category ?: "UNKNOWN").append("\n\n")
            append("Does this transaction qualify for this credit? ")
            append("Answer with exactly one word: YES or NO.")
        }

        val body = ChatRequest(
            model = model,
            temperature = 0.0,
            maxTokens = 4,
            messages = listOf(ChatMessage(role = "user", content = prompt)),
        )
        val bodyJson = json.encodeToString(ChatRequest.serializer(), body)

        val request = Request.Builder()
            .url(baseUrl + "chat/completions")
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .post(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            // Retry on transient rate-limit (429) and overload (503) responses
            // with exponential backoff. Most free tiers (Gemini Flash, Groq,
            // OpenRouter) burst-throttle aggressively; a short backoff usually
            // turns a 429 into a 200 within a second.
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
                        val parsed = runCatching { json.decodeFromString(ChatResponse.serializer(), text) }
                            .getOrNull() ?: return@withContext null
                        val answer = parsed.choices.firstOrNull()?.message?.content
                            ?.trim()
                            ?.uppercase()
                            ?: return@withContext null
                        return@withContext when {
                            answer.startsWith("YES") -> true
                            answer.startsWith("NO") -> false
                            else -> null
                        }
                    }
                }
                // Honor server-provided Retry-After (seconds) when present;
                // otherwise back off exponentially: 500ms, 1s, 2s.
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

    private companion object {
        const val TAG = "AiMatchClient"
        const val MAX_RETRIES = 3
        const val BASE_BACKOFF_MS = 500L
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

// ---- OpenAI Chat Completions wire format -----------------------------------

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.0,
    @kotlinx.serialization.SerialName("max_tokens") val maxTokens: Int = 4,
)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class ChatResponse(
    val choices: List<ChatChoice> = emptyList(),
)

@Serializable
private data class ChatChoice(
    val message: ChatMessage? = null,
)
