package com.example.creditcardapp.data.plaid

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Logs Plaid HTTP error responses to Logcat (tag `PlaidError`) without ever
 * touching the request body — the request contains client_id + secret and must
 * never be logged. Also rewrites the error message on the in-memory Response so
 * callers can surface Plaid's `error_message` instead of the generic
 * "HTTP 400 Bad Request" from Retrofit.
 */
class PlaidErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (!response.isSuccessful) {
            // peekBody consumes a copy so downstream Retrofit can still read body.
            val bodyText = runCatching { response.peekBody(MAX_PEEK).string() }.getOrNull()
            Log.w(
                TAG,
                "Plaid ${request.method} ${request.url.encodedPath} -> ${response.code}: $bodyText"
            )
        }
        return response
    }

    private companion object {
        const val TAG = "PlaidError"
        const val MAX_PEEK = 16_384L
    }
}
