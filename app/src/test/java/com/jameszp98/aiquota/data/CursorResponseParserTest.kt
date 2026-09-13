package com.jameszp98.aiquota.data

import com.jameszp98.aiquota.data.provider.cursor.CursorModelsData
import com.jameszp98.aiquota.data.provider.cursor.CursorResponseParser
import com.jameszp98.aiquota.data.provider.cursor.CursorUsageResponse
import com.jameszp98.aiquota.data.provider.cursor.CursorUsageSummaryResponse
import com.jameszp98.aiquota.data.provider.cursor.OnDemandData
import com.jameszp98.aiquota.data.provider.cursor.OtherModelsData
import com.jameszp98.aiquota.data.provider.cursor.UsageWrapper
import com.jameszp98.aiquota.domain.QuotaUnit
import com.jameszp98.aiquota.domain.QuotaWindow
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CursorResponseParserTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `parse primary API response with all fields`() {
        val jsonString = loadResource("cursor_usage_response.json")
        val response = json.decodeFromString<CursorUsageResponse>(jsonString)

        val usage = CursorResponseParser.parse(response)

        assertEquals("cursor", usage.providerId.value)
        assertEquals("Cursor", usage.displayName)
        assertEquals("Pro", usage.planName)
        assertFalse(usage.isDemo)

        // Should have 3 buckets: Cursor Models, Other Models, On-demand
        assertEquals(3, usage.buckets.size)

        // Cursor Models bucket
        val cursorModelsBucket = usage.buckets.find { it.name == "Cursor Models" }
        assertNotNull(cursorModelsBucket)
        assertEquals(150.0, cursorModelsBucket!!.used, 0.01)
        assertEquals(500.0, cursorModelsBucket.limit!!, 0.01)
        assertEquals(QuotaUnit.REQUESTS, cursorModelsBucket.unit)
        assertEquals(QuotaWindow.MONTHLY, cursorModelsBucket.window)
        assertFalse(cursorModelsBucket.isUnlimited)
        assertEquals(30.0, cursorModelsBucket.percentUsed, 0.01)

        // Other Models bucket (dollar-based)
        val otherModelsBucket = usage.buckets.find { it.name == "Other Models" }
        assertNotNull(otherModelsBucket)
        assertEquals(7.50, otherModelsBucket!!.used, 0.01)
        assertEquals(20.0, otherModelsBucket.limit!!, 0.01)
        assertEquals(QuotaUnit.DOLLARS, otherModelsBucket.unit)
        assertEquals(QuotaWindow.MONTHLY, otherModelsBucket.window)
        assertEquals(37.5, otherModelsBucket.percentUsed, 0.01)

        // On-demand bucket
        val onDemandBucket = usage.buckets.find { it.name == "On-demand" }
        assertNotNull(onDemandBucket)
        assertEquals(2.50, onDemandBucket!!.used, 0.01)
        assertEquals(100.0, onDemandBucket.limit!!, 0.01)
        assertEquals(QuotaUnit.DOLLARS, onDemandBucket.unit)
        assertEquals(QuotaWindow.NONE, onDemandBucket.window)
    }

    @Test
    fun `parse primary API response with unlimited Cursor Models`() {
        val jsonString = loadResource("cursor_usage_response_unlimited.json")
        val response = json.decodeFromString<CursorUsageResponse>(jsonString)

        val usage = CursorResponseParser.parse(response)

        assertEquals("Pro+", usage.planName)

        // Should have 2 buckets (no on-demand since disabled)
        assertEquals(2, usage.buckets.size)

        // Cursor Models bucket - unlimited
        val cursorModelsBucket = usage.buckets.find { it.name == "Cursor Models" }
        assertNotNull(cursorModelsBucket)
        assertEquals(1500.0, cursorModelsBucket!!.used, 0.01)
        assertNull(cursorModelsBucket.limit)
        assertTrue(cursorModelsBucket.isUnlimited)
        assertEquals(0.0, cursorModelsBucket.percentUsed, 0.01) // Unlimited = 0% used

        // Other Models bucket
        val otherModelsBucket = usage.buckets.find { it.name == "Other Models" }
        assertNotNull(otherModelsBucket)
        assertEquals(35.0, otherModelsBucket!!.used, 0.01)
        assertEquals(70.0, otherModelsBucket.limit!!, 0.01)
        assertEquals(50.0, otherModelsBucket.percentUsed, 0.01)
    }

    @Test
    fun `parse fallback API (usage-summary) response`() {
        val jsonString = loadResource("cursor_usage_summary_response.json")
        val response = json.decodeFromString<CursorUsageSummaryResponse>(jsonString)

        val usage = CursorResponseParser.parseFromSummary(response)

        assertEquals("cursor", usage.providerId.value)
        assertEquals(3, usage.buckets.size)

        // Cursor Models bucket
        val cursorModelsBucket = usage.buckets.find { it.name == "Cursor Models" }
        assertNotNull(cursorModelsBucket)
        assertEquals(250.0, cursorModelsBucket!!.used, 0.01)
        assertEquals(500.0, cursorModelsBucket.limit!!, 0.01)

        // Other Models bucket
        val otherModelsBucket = usage.buckets.find { it.name == "Other Models" }
        assertNotNull(otherModelsBucket)
        assertEquals(12.0, otherModelsBucket!!.used, 0.01)
        assertEquals(20.0, otherModelsBucket.limit!!, 0.01)

        // On-demand bucket
        val onDemandBucket = usage.buckets.find { it.name == "On-demand" }
        assertNotNull(onDemandBucket)
        assertEquals(5.0, onDemandBucket!!.used, 0.01)
    }

    @Test
    fun `parse empty response gracefully`() {
        val response = CursorUsageResponse()

        val usage = CursorResponseParser.parse(response)

        assertEquals("cursor", usage.providerId.value)
        assertTrue(usage.buckets.isEmpty())
        assertNull(usage.planName)
    }

    @Test
    fun `parse response with request-based Other Models`() {
        val response = CursorUsageResponse(
            cursorModels = CursorModelsData(used = 100, limit = 500),
            otherModels = OtherModelsData(used = 50, limit = 100)
        )

        val usage = CursorResponseParser.parse(response)

        val otherModelsBucket = usage.buckets.find { it.name == "Other Models" }
        assertNotNull(otherModelsBucket)
        assertEquals(50.0, otherModelsBucket!!.used, 0.01)
        assertEquals(100.0, otherModelsBucket.limit!!, 0.01)
        assertEquals(QuotaUnit.REQUESTS, otherModelsBucket.unit)
    }

    @Test
    fun `QuotaBucket display formatting`() {
        val response = CursorUsageResponse(
            cursorModels = CursorModelsData(used = 150, limit = 500),
            otherModels = OtherModelsData(usedDollars = 7.5, limitDollars = 20.0)
        )

        val usage = CursorResponseParser.parse(response)

        val cursorBucket = usage.buckets.find { it.name == "Cursor Models" }!!
        assertEquals("150", cursorBucket.displayUsed)
        assertEquals("500", cursorBucket.displayLimit)
        assertEquals("350", cursorBucket.displayRemaining)

        val otherBucket = usage.buckets.find { it.name == "Other Models" }!!
        assertEquals("\$7.50", otherBucket.displayUsed)
        assertEquals("\$20.00", otherBucket.displayLimit)
        assertEquals("\$12.50", otherBucket.displayRemaining)
    }

    @Test
    fun `unlimited bucket has no remaining and zero percent`() {
        val response = CursorUsageResponse(
            cursorModels = CursorModelsData(used = 1000, unlimited = true)
        )

        val usage = CursorResponseParser.parse(response)

        val bucket = usage.buckets.first()
        assertTrue(bucket.isUnlimited)
        assertNull(bucket.remaining)
        assertNull(bucket.limit)
        assertEquals(0.0, bucket.percentUsed, 0.01)
        assertEquals("Unlimited", bucket.displayLimit)
    }

    private fun loadResource(filename: String): String {
        return javaClass.classLoader?.getResourceAsStream(filename)
            ?.bufferedReader()
            ?.readText()
            ?: throw IllegalArgumentException("Resource not found: $filename")
    }
}
