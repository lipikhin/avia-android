package com.aviatechnik.android.di

import com.aviatechnik.android.BuildConfig
import com.aviatechnik.android.data.api.AviaApi
import com.aviatechnik.android.data.auth.TokenStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true // server payloads evolve; the client reads what it knows
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun okHttp(tokenStore: TokenStore): OkHttpClient {
        val auth = Interceptor { chain ->
            val builder = chain.request().newBuilder().header("Accept", "application/json")
            tokenStore.token?.let { builder.header("Authorization", "Bearer $it") }
            // Debug against local OSPanel: vhost is picked by the Host header
            if (BuildConfig.DEBUG_HOST_HEADER.isNotEmpty()) {
                builder.header("Host", BuildConfig.DEBUG_HOST_HEADER)
            }
            chain.proceed(builder.build())
        }
        val builder = OkHttpClient.Builder().addInterceptor(auth)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun api(okHttp: OkHttpClient, json: Json): AviaApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttp)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AviaApi::class.java)
}
