package com.app.stash.android.di

import com.app.stash.android.BuildConfig
import com.app.stash.android.data.places.FoursquareApi
import com.app.stash.android.data.places.OverpassApi
import com.app.stash.android.data.remote.CreditCardApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://example.com/api/"
    // Identifies the app to API operators. Overpass mirrors return 406/429 for
    // generic User-Agent strings like the default okhttp/4.x.
    private const val USER_AGENT = "StashApp/1.0 (Android; com.app.stash.android)"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        // Plaid requires fields like country_codes / language / products even
        // when they match our DTO defaults, so emit defaults instead of dropping them.
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideCreditCardApi(retrofit: Retrofit): CreditCardApi =
        retrofit.create(CreditCardApi::class.java)

    @Provides
    @Singleton
    fun provideOverpassApi(client: OkHttpClient, json: Json): OverpassApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            // kumi.systems is a friendlier Overpass mirror with looser rate limits.
            .baseUrl("https://overpass.kumi.systems/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(OverpassApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFoursquareApi(client: OkHttpClient, json: Json): FoursquareApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            // New Foursquare Places API (the legacy api.foursquare.com/v3 host
            // rejects newly issued keys with 401).
            .baseUrl("https://places-api.foursquare.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(FoursquareApi::class.java)
    }
}
