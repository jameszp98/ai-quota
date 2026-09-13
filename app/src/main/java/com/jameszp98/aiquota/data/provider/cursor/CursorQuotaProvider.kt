package com.jameszp98.aiquota.data.provider.cursor

import com.jameszp98.aiquota.data.storage.TokenStorage
import com.jameszp98.aiquota.domain.FetchResult
import com.jameszp98.aiquota.domain.ProviderId
import com.jameszp98.aiquota.domain.ProviderUsage
import com.jameszp98.aiquota.domain.QuotaBucket
import com.jameszp98.aiquota.domain.QuotaProvider
import com.jameszp98.aiquota.domain.QuotaUnit
import com.jameszp98.aiquota.domain.QuotaWindow
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Cursor AI quota provider.
 *
 * Cursor has TWO monthly quota pools:
 * 1. Cursor Models - First-party models (Composer, Cursor Grok, etc.)
 * 2. Other Models - Third-party models with dollar-based allowance (Pro ~$20, Pro+ ~$70, Ultra ~$400)
 *
 * Plus optional on-demand (pay-as-you-go) spending beyond included quotas.
 *
 * IMPORTANT: Cursor does NOT have weekly quotas or rolling 5-hour windows.
 * The reset is tied to the subscription billing anniversary, not calendar month start.
 *
 * Uses UNOFFICIAL APIs that may change without notice:
 * - Primary: POST https://api2.cursor.sh/aiserver.v1.DashboardService/GetCurrentPeriodUsage
 * - Fallback: GET https://cursor.com/api/usage-summary
 */
class CursorQuotaProvider(
    private val primaryApi: CursorApi,
    private val fallbackApi: CursorWebApi,
    private val tokenStorage: TokenStorage
) : QuotaProvider {

    override val id = PROVIDER_ID
    override val displayName = DISPLAY_NAME
    override val requiresAuth = true

    override suspend fun fetchUsage(): FetchResult {
        val bearerToken = tokenStorage.getCursorBearerToken()
        val sessionToken = tokenStorage.getCursorSessionToken()

        return when {
            bearerToken != null -> fetchWithBearerToken(bearerToken)
            sessionToken != null -> fetchWithSessionToken(sessionToken)
            else -> FetchResult.Error(
                message = "No Cursor token configured. Add your token in Settings.",
                isAuthError = true
            )
        }
    }

    override suspend fun isConfigured(): Boolean {
        return tokenStorage.getCursorBearerToken() != null ||
                tokenStorage.getCursorSessionToken() != null
    }

    override fun getDemoUsage(): ProviderUsage {
        val now = Instant.now()
        val resetDate = now.plus(15, ChronoUnit.DAYS)

        return ProviderUsage(
            providerId = PROVIDER_ID,
            displayName = DISPLAY_NAME,
            buckets = listOf(
                QuotaBucket(
                    name = "Cursor Models",
                    used = 150.0,
                    limit = 500.0,
                    unit = QuotaUnit.REQUESTS,
                    window = QuotaWindow.MONTHLY,
                    resetsAt = resetDate
                ),
                QuotaBucket(
                    name = "Other Models",
                    used = 7.0,
                    limit = 20.0,
                    unit = QuotaUnit.DOLLARS,
                    window = QuotaWindow.MONTHLY,
                    resetsAt = resetDate
                ),
                QuotaBucket(
                    name = "On-demand",
                    used = 2.50,
                    limit = 100.0,
                    unit = QuotaUnit.DOLLARS,
                    window = QuotaWindow.NONE,
                    resetsAt = null
                )
            ),
            fetchedAt = now,
            isDemo = true,
            planName = "Pro"
        )
    }

    private suspend fun fetchWithBearerToken(token: String): FetchResult {
        return try {
            val response = primaryApi.getCurrentPeriodUsage(
                authorization = "Bearer $token"
            )

            when {
                response.isSuccessful && response.body() != null -> {
                    val usage = CursorResponseParser.parse(response.body()!!)
                    FetchResult.Success(usage)
                }
                response.code() == 401 || response.code() == 403 -> {
                    FetchResult.Error(
                        message = "Cursor authentication failed. Check your token.",
                        isAuthError = true
                    )
                }
                else -> {
                    FetchResult.Error(
                        message = "Cursor API error: ${response.code()} ${response.message()}"
                    )
                }
            }
        } catch (e: Exception) {
            FetchResult.Error(message = "Network error: ${e.message ?: "Unknown error"}")
        }
    }

    private suspend fun fetchWithSessionToken(token: String): FetchResult {
        return try {
            val response = fallbackApi.getUsageSummary(
                cookie = "WorkosCursorSessionToken=$token"
            )

            when {
                response.isSuccessful && response.body() != null -> {
                    val usage = CursorResponseParser.parseFromSummary(response.body()!!)
                    FetchResult.Success(usage)
                }
                response.code() == 401 || response.code() == 403 -> {
                    FetchResult.Error(
                        message = "Cursor session expired. Re-authenticate in Settings.",
                        isAuthError = true
                    )
                }
                else -> {
                    FetchResult.Error(
                        message = "Cursor API error: ${response.code()} ${response.message()}"
                    )
                }
            }
        } catch (e: Exception) {
            FetchResult.Error(message = "Network error: ${e.message ?: "Unknown error"}")
        }
    }

    companion object {
        val PROVIDER_ID = ProviderId("cursor")
        const val DISPLAY_NAME = "Cursor"
    }
}

// Future provider stubs for reference:

/**
 * Kimi AI quota provider (future implementation).
 *
 * Kimi has THREE quota windows:
 * 1. Monthly quota - Resets with billing cycle
 * 2. Weekly quota - Resets every 7 days
 * 3. 5-hour rolling window - Continuous rolling limit
 *
 * This demonstrates why the domain model supports multiple QuotaWindow types.
 */
// class KimiQuotaProvider : QuotaProvider { ... }
