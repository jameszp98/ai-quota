package com.jameszp98.aiquota.data

import com.jameszp98.aiquota.domain.ProviderId
import com.jameszp98.aiquota.domain.ProviderUsage
import com.jameszp98.aiquota.domain.QuotaBucket
import com.jameszp98.aiquota.domain.QuotaUnit
import com.jameszp98.aiquota.domain.QuotaWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DomainModelsTest {

    @Test
    fun `QuotaBucket calculates remaining correctly`() {
        val bucket = QuotaBucket(
            name = "Test",
            used = 30.0,
            limit = 100.0,
            unit = QuotaUnit.REQUESTS,
            window = QuotaWindow.MONTHLY,
            resetsAt = null
        )

        assertEquals(70.0, bucket.remaining!!, 0.01)
        assertEquals(30.0, bucket.percentUsed, 0.01)
    }

    @Test
    fun `QuotaBucket handles overuse (used exceeds limit)`() {
        val bucket = QuotaBucket(
            name = "Test",
            used = 120.0,
            limit = 100.0,
            unit = QuotaUnit.REQUESTS,
            window = QuotaWindow.MONTHLY,
            resetsAt = null
        )

        assertEquals(0.0, bucket.remaining!!, 0.01)
        assertEquals(100.0, bucket.percentUsed, 0.01) // Capped at 100%
    }

    @Test
    fun `QuotaBucket handles zero limit`() {
        val bucket = QuotaBucket(
            name = "Test",
            used = 10.0,
            limit = 0.0,
            unit = QuotaUnit.REQUESTS,
            window = QuotaWindow.MONTHLY,
            resetsAt = null
        )

        assertEquals(0.0, bucket.remaining!!, 0.01)
        assertEquals(0.0, bucket.percentUsed, 0.01) // Avoid division by zero
    }

    @Test
    fun `QuotaBucket handles null limit`() {
        val bucket = QuotaBucket(
            name = "Test",
            used = 10.0,
            limit = null,
            unit = QuotaUnit.REQUESTS,
            window = QuotaWindow.MONTHLY,
            resetsAt = null
        )

        assertNull(bucket.remaining)
        assertEquals(0.0, bucket.percentUsed, 0.01)
    }

    @Test
    fun `QuotaBucket unlimited has null remaining`() {
        val bucket = QuotaBucket(
            name = "Test",
            used = 1000.0,
            limit = null,
            unit = QuotaUnit.REQUESTS,
            window = QuotaWindow.MONTHLY,
            resetsAt = null,
            isUnlimited = true
        )

        assertNull(bucket.remaining)
        assertEquals(0.0, bucket.percentUsed, 0.01)
    }

    @Test
    fun `QuotaBucket formats dollar values correctly`() {
        val bucket = QuotaBucket(
            name = "Test",
            used = 7.5,
            limit = 20.0,
            unit = QuotaUnit.DOLLARS,
            window = QuotaWindow.MONTHLY,
            resetsAt = null
        )

        assertEquals("\$7.50", bucket.displayUsed)
        assertEquals("\$20.00", bucket.displayLimit)
        assertEquals("\$12.50", bucket.displayRemaining)
    }

    @Test
    fun `QuotaBucket formats request values correctly`() {
        val bucket = QuotaBucket(
            name = "Test",
            used = 150.0,
            limit = 500.0,
            unit = QuotaUnit.REQUESTS,
            window = QuotaWindow.MONTHLY,
            resetsAt = null
        )

        assertEquals("150", bucket.displayUsed)
        assertEquals("500", bucket.displayLimit)
        assertEquals("350", bucket.displayRemaining)
    }

    @Test
    fun `ProviderUsage hasError returns true when errorMessage set`() {
        val usage = ProviderUsage.error(
            ProviderId("test"),
            "Test Provider",
            "Auth failed"
        )

        assertTrue(usage.hasError)
        assertEquals("Auth failed", usage.errorMessage)
    }

    @Test
    fun `ProviderUsage hasError returns false for successful usage`() {
        val usage = ProviderUsage(
            providerId = ProviderId("test"),
            displayName = "Test Provider",
            buckets = emptyList(),
            fetchedAt = Instant.now()
        )

        assertFalse(usage.hasError)
        assertNull(usage.errorMessage)
    }

    @Test
    fun `ProviderId value class works correctly`() {
        val id1 = ProviderId("cursor")
        val id2 = ProviderId("cursor")
        val id3 = ProviderId("kimi")

        assertEquals(id1, id2)
        assertFalse(id1 == id3)
        assertEquals("cursor", id1.value)
    }

    @Test
    fun `QuotaWindow covers all expected values`() {
        val windows = QuotaWindow.entries.toTypedArray()

        assertTrue(windows.contains(QuotaWindow.MONTHLY))
        assertTrue(windows.contains(QuotaWindow.WEEKLY))
        assertTrue(windows.contains(QuotaWindow.ROLLING_HOURS))
        assertTrue(windows.contains(QuotaWindow.DAILY))
        assertTrue(windows.contains(QuotaWindow.NONE))
    }

    @Test
    fun `QuotaUnit covers all expected values`() {
        val units = QuotaUnit.entries.toTypedArray()

        assertTrue(units.contains(QuotaUnit.REQUESTS))
        assertTrue(units.contains(QuotaUnit.DOLLARS))
        assertTrue(units.contains(QuotaUnit.TOKENS))
        assertTrue(units.contains(QuotaUnit.CREDITS))
    }
}
