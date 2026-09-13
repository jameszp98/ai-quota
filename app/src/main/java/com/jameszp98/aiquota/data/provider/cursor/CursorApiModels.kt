package com.jameszp98.aiquota.data.provider.cursor

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from POST https://api2.cursor.sh/aiserver.v1.DashboardService/GetCurrentPeriodUsage
 *
 * UNOFFICIAL API - Community-documented endpoint that may change without notice.
 */
@Serializable
data class CursorUsageResponse(
    @SerialName("cursorModels")
    val cursorModels: CursorModelsData? = null,
    @SerialName("otherModels")
    val otherModels: OtherModelsData? = null,
    @SerialName("onDemand")
    val onDemand: OnDemandData? = null,
    @SerialName("periodStart")
    val periodStart: String? = null,
    @SerialName("periodEnd")
    val periodEnd: String? = null,
    @SerialName("planType")
    val planType: String? = null
)

@Serializable
data class CursorModelsData(
    @SerialName("used")
    val used: Long? = null,
    @SerialName("limit")
    val limit: Long? = null,
    @SerialName("unlimited")
    val unlimited: Boolean? = null
)

@Serializable
data class OtherModelsData(
    @SerialName("usedDollars")
    val usedDollars: Double? = null,
    @SerialName("limitDollars")
    val limitDollars: Double? = null,
    @SerialName("used")
    val used: Long? = null,
    @SerialName("limit")
    val limit: Long? = null
)

@Serializable
data class OnDemandData(
    @SerialName("enabled")
    val enabled: Boolean? = null,
    @SerialName("spentDollars")
    val spentDollars: Double? = null,
    @SerialName("softLimitDollars")
    val softLimitDollars: Double? = null,
    @SerialName("hardLimitDollars")
    val hardLimitDollars: Double? = null
)

/**
 * Response from GET https://cursor.com/api/usage-summary
 * Fallback endpoint using session cookie.
 *
 * UNOFFICIAL API - May change without notice.
 */
@Serializable
data class CursorUsageSummaryResponse(
    @SerialName("startOfPeriod")
    val startOfPeriod: String? = null,
    @SerialName("endOfPeriod")
    val endOfPeriod: String? = null,
    @SerialName("usage")
    val usage: UsageWrapper? = null
)

@Serializable
data class UsageWrapper(
    @SerialName("cursorModels")
    val cursorModels: CursorModelsData? = null,
    @SerialName("otherModels")
    val otherModels: OtherModelsData? = null,
    @SerialName("onDemand")
    val onDemand: OnDemandData? = null
)
