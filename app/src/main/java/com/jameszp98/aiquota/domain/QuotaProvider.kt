package com.jameszp98.aiquota.domain

/**
 * Interface for AI quota providers.
 *
 * Each implementation fetches usage data from a specific AI service.
 * The architecture supports multiple providers with different quota structures:
 *
 * - Cursor: Monthly Cursor Models + Monthly Other Models + On-demand
 * - Kimi: Monthly + Weekly + 5-hour rolling window (future)
 * - OpenAI: Monthly token/dollar limits (future)
 */
interface QuotaProvider {
    /** Unique identifier for this provider */
    val id: ProviderId

    /** Human-readable name for display */
    val displayName: String

    /** Whether this provider requires authentication */
    val requiresAuth: Boolean

    /** Fetch current usage data from the provider's API */
    suspend fun fetchUsage(): FetchResult

    /** Check if authentication is configured */
    suspend fun isConfigured(): Boolean

    /** Get demo/mock data for testing without auth */
    fun getDemoUsage(): ProviderUsage
}
