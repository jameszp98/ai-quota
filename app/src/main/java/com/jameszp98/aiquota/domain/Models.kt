package com.jameszp98.aiquota.domain

import java.time.Instant

/**
 * Unique identifier for an AI provider (e.g., "cursor", "kimi", "openai").
 */
@JvmInline
value class ProviderId(val value: String)

/**
 * Time window for quota resets.
 *
 * Different AI providers use different reset windows:
 * - MONTHLY: Resets with billing cycle (e.g., Cursor)
 * - WEEKLY: Resets every 7 days (e.g., Kimi weekly quota)
 * - ROLLING_HOURS: Rolling window in hours (e.g., Kimi 5-hour window)
 * - DAILY: Resets every 24 hours
 * - NONE: No reset / lifetime quota
 */
enum class QuotaWindow {
    MONTHLY,
    WEEKLY,
    ROLLING_HOURS,
    DAILY,
    NONE
}

/**
 * Unit of measurement for quota values.
 */
enum class QuotaUnit {
    REQUESTS,   // Number of API requests/messages
    DOLLARS,    // Dollar amount (e.g., $20.00)
    TOKENS,     // Token count
    CREDITS     // Generic credits
}

/**
 * A single quota bucket representing one type of usage limit.
 *
 * Examples:
 * - Cursor Models: 150/500 requests, monthly reset
 * - Other Models: $7.00/$20.00, monthly reset
 * - Kimi weekly: 100/150 messages, weekly reset
 * - Kimi 5-hour: 10/50 messages, 5-hour rolling window
 */
data class QuotaBucket(
    val name: String,
    val used: Double,
    val limit: Double?,
    val unit: QuotaUnit,
    val window: QuotaWindow,
    val resetsAt: Instant?,
    val isUnlimited: Boolean = false,
    val rollingWindowHours: Int? = null
) {
    val remaining: Double?
        get() = if (isUnlimited || limit == null) null else (limit - used).coerceAtLeast(0.0)

    val percentUsed: Double
        get() = when {
            isUnlimited -> 0.0
            limit == null || limit == 0.0 -> 0.0
            else -> (used / limit * 100).coerceIn(0.0, 100.0)
        }

    val displayUsed: String
        get() = formatValue(used, unit)

    val displayLimit: String?
        get() = if (isUnlimited) "Unlimited" else limit?.let { formatValue(it, unit) }

    val displayRemaining: String?
        get() = remaining?.let { formatValue(it, unit) }

    private fun formatValue(value: Double, unit: QuotaUnit): String = when (unit) {
        QuotaUnit.DOLLARS -> "$${String.format("%.2f", value)}"
        QuotaUnit.REQUESTS, QuotaUnit.TOKENS, QuotaUnit.CREDITS -> value.toLong().toString()
    }
}

/**
 * Complete usage data for a single AI provider.
 */
data class ProviderUsage(
    val providerId: ProviderId,
    val displayName: String,
    val buckets: List<QuotaBucket>,
    val fetchedAt: Instant,
    val isDemo: Boolean = false,
    val planName: String? = null,
    val errorMessage: String? = null
) {
    val hasError: Boolean get() = errorMessage != null

    companion object {
        fun empty(providerId: ProviderId, displayName: String) = ProviderUsage(
            providerId = providerId,
            displayName = displayName,
            buckets = emptyList(),
            fetchedAt = Instant.now(),
            isDemo = false
        )

        fun error(providerId: ProviderId, displayName: String, message: String) = ProviderUsage(
            providerId = providerId,
            displayName = displayName,
            buckets = emptyList(),
            fetchedAt = Instant.now(),
            isDemo = false,
            errorMessage = message
        )
    }
}

/**
 * Result wrapper for provider fetch operations.
 */
sealed class FetchResult {
    data class Success(val usage: ProviderUsage) : FetchResult()
    data class Error(val message: String, val isAuthError: Boolean = false) : FetchResult()
}
