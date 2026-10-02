package com.app.stash.android.data.plaid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Parses Plaid's JSON error envelope. Returns a short human-readable message,
 * never including the request body (which holds client_id + secret).
 */
@Serializable
internal data class PlaidErrorBody(
    @SerialName("error_type") val errorType: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("error_message") val errorMessage: String? = null,
    @SerialName("display_message") val displayMessage: String? = null,
)

private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

internal fun Throwable.plaidErrorMessage(): String {
    val http = this as? HttpException ?: return message ?: this::class.java.simpleName
    val raw = runCatching { http.response()?.errorBody()?.string() }.getOrNull()
    val parsed = raw?.let { runCatching { errorJson.decodeFromString(PlaidErrorBody.serializer(), it) }.getOrNull() }
    val pieces = listOfNotNull(
        parsed?.errorCode,
        parsed?.displayMessage ?: parsed?.errorMessage,
    )
    return if (pieces.isEmpty()) "HTTP ${http.code()}" else pieces.joinToString(": ")
}
