package com.jameszp98.aiquota.ui.widget

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.dataStoreFile
import androidx.glance.state.GlanceStateDefinition
import com.jameszp98.aiquota.domain.ProviderId
import com.jameszp98.aiquota.domain.ProviderUsage
import com.jameszp98.aiquota.domain.QuotaBucket
import com.jameszp98.aiquota.domain.QuotaUnit
import com.jameszp98.aiquota.domain.QuotaWindow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Widget state containing all provider usage data.
 */
data class WidgetState(
    val providers: List<ProviderUsage>,
    val lastUpdated: Instant,
    val isDemo: Boolean
) {
    companion object {
        fun demo(): WidgetState {
            val now = Instant.now()
            val resetDate = now.plus(15, ChronoUnit.DAYS)

            val cursorUsage = ProviderUsage(
                providerId = ProviderId("cursor"),
                displayName = "Cursor",
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

            return WidgetState(
                providers = listOf(cursorUsage),
                lastUpdated = now,
                isDemo = true
            )
        }

        fun empty(): WidgetState = WidgetState(
            providers = emptyList(),
            lastUpdated = Instant.now(),
            isDemo = false
        )
    }
}

object WidgetStateDefinition : GlanceStateDefinition<WidgetState> {

    private const val DATA_STORE_FILENAME = "widget_state"

    private val Context.dataStore by dataStore(DATA_STORE_FILENAME, WidgetStateSerializer)

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<WidgetState> {
        return context.dataStore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return context.dataStoreFile(DATA_STORE_FILENAME)
    }
}

private object WidgetStateSerializer : Serializer<WidgetState> {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override val defaultValue: WidgetState = WidgetState.demo()

    override suspend fun readFrom(input: InputStream): WidgetState {
        try {
            val serializable = json.decodeFromString(
                SerializableWidgetState.serializer(),
                input.bufferedReader().readText()
            )
            return serializable.toDomain()
        } catch (e: SerializationException) {
            throw CorruptionException("Could not read widget state", e)
        }
    }

    override suspend fun writeTo(t: WidgetState, output: OutputStream) {
        val serializable = SerializableWidgetState.fromDomain(t)
        output.bufferedWriter().use {
            it.write(json.encodeToString(SerializableWidgetState.serializer(), serializable))
        }
    }
}

@Serializable
private data class SerializableWidgetState(
    val providers: List<SerializableProviderUsage>,
    val lastUpdatedMs: Long,
    val isDemo: Boolean
) {
    fun toDomain(): WidgetState = WidgetState(
        providers = providers.map { it.toDomain() },
        lastUpdated = Instant.ofEpochMilli(lastUpdatedMs),
        isDemo = isDemo
    )

    companion object {
        fun fromDomain(state: WidgetState): SerializableWidgetState = SerializableWidgetState(
            providers = state.providers.map { SerializableProviderUsage.fromDomain(it) },
            lastUpdatedMs = state.lastUpdated.toEpochMilli(),
            isDemo = state.isDemo
        )
    }
}

@Serializable
private data class SerializableProviderUsage(
    val providerId: String,
    val displayName: String,
    val buckets: List<SerializableBucket>,
    val fetchedAtMs: Long,
    val isDemo: Boolean,
    val planName: String?,
    val errorMessage: String?
) {
    fun toDomain(): ProviderUsage = ProviderUsage(
        providerId = ProviderId(providerId),
        displayName = displayName,
        buckets = buckets.map { it.toDomain() },
        fetchedAt = Instant.ofEpochMilli(fetchedAtMs),
        isDemo = isDemo,
        planName = planName,
        errorMessage = errorMessage
    )

    companion object {
        fun fromDomain(usage: ProviderUsage): SerializableProviderUsage = SerializableProviderUsage(
            providerId = usage.providerId.value,
            displayName = usage.displayName,
            buckets = usage.buckets.map { SerializableBucket.fromDomain(it) },
            fetchedAtMs = usage.fetchedAt.toEpochMilli(),
            isDemo = usage.isDemo,
            planName = usage.planName,
            errorMessage = usage.errorMessage
        )
    }
}

@Serializable
private data class SerializableBucket(
    val name: String,
    val used: Double,
    val limit: Double?,
    val unit: String,
    val window: String,
    val resetsAtMs: Long?,
    val isUnlimited: Boolean,
    val rollingWindowHours: Int?
) {
    fun toDomain(): QuotaBucket = QuotaBucket(
        name = name,
        used = used,
        limit = limit,
        unit = QuotaUnit.valueOf(unit),
        window = QuotaWindow.valueOf(window),
        resetsAt = resetsAtMs?.let { Instant.ofEpochMilli(it) },
        isUnlimited = isUnlimited,
        rollingWindowHours = rollingWindowHours
    )

    companion object {
        fun fromDomain(bucket: QuotaBucket): SerializableBucket = SerializableBucket(
            name = bucket.name,
            used = bucket.used,
            limit = bucket.limit,
            unit = bucket.unit.name,
            window = bucket.window.name,
            resetsAtMs = bucket.resetsAt?.toEpochMilli(),
            isUnlimited = bucket.isUnlimited,
            rollingWindowHours = bucket.rollingWindowHours
        )
    }
}
