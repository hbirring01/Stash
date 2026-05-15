package com.example.creditcardapp.di

import android.content.Context
import com.example.creditcardapp.data.plaid.PlaidApi
import com.example.creditcardapp.data.plaid.PlaidCredentialsStore
import com.example.creditcardapp.data.plaid.PlaidErrorInterceptor
import com.example.creditcardapp.data.plaid.PlaidTokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlaidRetrofit

@Module
@InstallIn(SingletonComponent::class)
object PlaidModule {

    private fun baseUrl(credentialsStore: PlaidCredentialsStore): String {
        // env is read from the on-device Keystore-encrypted credentials store,
        // populated by the in-app setup screen. Defaults to sandbox when unset.
        val env = runBlocking { credentialsStore.env() }
        return when (env.lowercase()) {
            "production" -> "https://production.plaid.com/"
            "development" -> "https://development.plaid.com/"
            else -> "https://sandbox.plaid.com/"
        }
    }

    @Provides
    @Singleton
    @PlaidRetrofit
    fun providePlaidRetrofit(json: Json, credentialsStore: PlaidCredentialsStore): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(PlaidErrorInterceptor())
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    // BODY logging would print the secret. Keep it off in every build.
                    level = HttpLoggingInterceptor.Level.NONE
                }
            )
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl(credentialsStore))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun providePlaidApi(@PlaidRetrofit retrofit: Retrofit): PlaidApi =
        retrofit.create(PlaidApi::class.java)

    @Provides
    @Singleton
    fun providePlaidTokenStore(@ApplicationContext context: Context): PlaidTokenStore =
        PlaidTokenStore(context)

    @Provides
    @Singleton
    fun providePlaidCredentialsStore(@ApplicationContext context: Context): PlaidCredentialsStore =
        PlaidCredentialsStore(context)
}
