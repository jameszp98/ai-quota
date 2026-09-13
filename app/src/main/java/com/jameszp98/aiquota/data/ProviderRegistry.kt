package com.jameszp98.aiquota.data

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.jameszp98.aiquota.data.provider.cursor.CursorApi
import com.jameszp98.aiquota.data.provider.cursor.CursorQuotaProvider
import com.jameszp98.aiquota.data.provider.cursor.CursorWebApi
import com.jameszp98.aiquota.data.storage.SettingsStore
import com.jameszp98.aiquota.data.storage.TokenStorage
import com.jameszp98.aiquota.domain.FetchResult
import com.jameszp98.aiquota.domain.ProviderId
import com.jameszp98.aiquota.domain.ProviderUsage
import com.jameszp98.aiquota.domain.QuotaProvider
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Central registry for all quota providers.
 * Manages provider instances and coordinates fetching usage data.
 */
class ProviderRegistry private constructor(
    private val providers: Map<ProviderId, QuotaProvider>,
    private val settingsStore: SettingsStore
) {
    fun getProvider(id: ProviderId): QuotaProvider? = providers[id]

    fun getAllProviders(): List<QuotaProvider> = providers.values.toList()

    suspend fun getEnabledProviders(): List<QuotaProvider> {
        val enabledIds = settingsStore.enabledProviderIds.first()
        return enabledIds.mapNotNull { providers[it] }
    }

    suspend fun fetchAllUsage(demoMode: Boolean): List<ProviderUsage> {
        val enabledProviders = getEnabledProviders()

        return enabledProviders.map { provider ->
            if (demoMode) {
                provider.getDemoUsage()
            } else {
                when (val result = provider.fetchUsage()) {
                    is FetchResult.Success -> result.usage
                    is FetchResult.Error -> ProviderUsage.error(
                        provider.id,
                        provider.displayName,
                        result.message
                    )
                }
            }
        }
    }

    companion object {
        @Volatile
        private var instance: ProviderRegistry? = null

        fun getInstance(context: Context): ProviderRegistry {
            return instance ?: synchronized(this) {
                instance ?: create(context.applicationContext).also { instance = it }
            }
        }

        private fun create(context: Context): ProviderRegistry {
            val tokenStorage = TokenStorage(context)
            val settingsStore = SettingsStore(context)

            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Content-Type", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val cursorPrimaryApi = Retrofit.Builder()
                .baseUrl("https://api2.cursor.sh/")
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(CursorApi::class.java)

            val cursorFallbackApi = Retrofit.Builder()
                .baseUrl("https://cursor.com/")
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(CursorWebApi::class.java)

            val cursorProvider = CursorQuotaProvider(
                primaryApi = cursorPrimaryApi,
                fallbackApi = cursorFallbackApi,
                tokenStorage = tokenStorage
            )

            val providers = mapOf(
                cursorProvider.id to cursorProvider
            )

            return ProviderRegistry(providers, settingsStore)
        }
    }
}
