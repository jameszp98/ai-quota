package com.jameszp98.aiquota.data.provider.cursor

import com.jameszp98.aiquota.domain.ProviderId
import com.jameszp98.aiquota.domain.ProviderUsage
import com.jameszp98.aiquota.domain.QuotaBucket
import com.jameszp98.aiquota.domain.QuotaUnit
import com.jameszp98.aiquota.domain.QuotaWindow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Parses Cursor API responses into domain models.
 */
object CursorResponseParser {

    private val dateTimeFormatters = listOf(
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ISO_INSTANT,
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    fun parse(response: CursorUsageResponse): ProviderUsage {
        val buckets = mutableListOf<QuotaBucket>()
        val periodEnd = parseDateTime(response.periodEnd)

        response.cursorModels?.let { cm ->
            buckets.add(parseCursorModelsBucket(cm, periodEnd))
        }

        response.otherModels?.let { om ->
            buckets.add(parseOtherModelsBucket(om, periodEnd))
        }

        response.onDemand?.let { od ->
            if (od.enabled == true) {
                buckets.add(parseOnDemandBucket(od))
            }
        }

        return ProviderUsage(
            providerId = CursorQuotaProvider.PROVIDER_ID,
            displayName = CursorQuotaProvider.DISPLAY_NAME,
            buckets = buckets,
            fetchedAt = Instant.now(),
            planName = response.planType
        )
    }

    fun parseFromSummary(response: CursorUsageSummaryResponse): ProviderUsage {
        val buckets = mutableListOf<QuotaBucket>()
        val periodEnd = parseDateTime(response.endOfPeriod)
        val usage = response.usage

        usage?.cursorModels?.let { cm ->
            buckets.add(parseCursorModelsBucket(cm, periodEnd))
        }

        usage?.otherModels?.let { om ->
            buckets.add(parseOtherModelsBucket(om, periodEnd))
        }

        usage?.onDemand?.let { od ->
            if (od.enabled == true) {
                buckets.add(parseOnDemandBucket(od))
            }
        }

        return ProviderUsage(
            providerId = CursorQuotaProvider.PROVIDER_ID,
            displayName = CursorQuotaProvider.DISPLAY_NAME,
            buckets = buckets,
            fetchedAt = Instant.now()
        )
    }

    private fun parseCursorModelsBucket(data: CursorModelsData, periodEnd: Instant?): QuotaBucket {
        val isUnlimited = data.unlimited == true
        return QuotaBucket(
            name = "Cursor Models",
            used = (data.used ?: 0L).toDouble(),
            limit = if (isUnlimited) null else data.limit?.toDouble(),
            unit = QuotaUnit.REQUESTS,
            window = QuotaWindow.MONTHLY,
            resetsAt = periodEnd,
            isUnlimited = isUnlimited
        )
    }

    private fun parseOtherModelsBucket(data: OtherModelsData, periodEnd: Instant?): QuotaBucket {
        val isDollarBased = data.usedDollars != null || data.limitDollars != null

        return if (isDollarBased) {
            QuotaBucket(
                name = "Other Models",
                used = data.usedDollars ?: 0.0,
                limit = data.limitDollars,
                unit = QuotaUnit.DOLLARS,
                window = QuotaWindow.MONTHLY,
                resetsAt = periodEnd
            )
        } else {
            QuotaBucket(
                name = "Other Models",
                used = (data.used ?: 0L).toDouble(),
                limit = data.limit?.toDouble(),
                unit = QuotaUnit.REQUESTS,
                window = QuotaWindow.MONTHLY,
                resetsAt = periodEnd
            )
        }
    }

    private fun parseOnDemandBucket(data: OnDemandData): QuotaBucket {
        return QuotaBucket(
            name = "On-demand",
            used = data.spentDollars ?: 0.0,
            limit = data.hardLimitDollars,
            unit = QuotaUnit.DOLLARS,
            window = QuotaWindow.NONE,
            resetsAt = null
        )
    }

    private fun parseDateTime(dateString: String?): Instant? {
        if (dateString.isNullOrBlank()) return null

        for (formatter in dateTimeFormatters) {
            try {
                val ldt = LocalDateTime.parse(dateString, formatter)
                return ldt.toInstant(ZoneOffset.UTC)
            } catch (_: DateTimeParseException) {
                // Try next formatter
            }
        }

        try {
            return Instant.parse(dateString)
        } catch (_: DateTimeParseException) {
            // Ignore
        }

        return null
    }
}
