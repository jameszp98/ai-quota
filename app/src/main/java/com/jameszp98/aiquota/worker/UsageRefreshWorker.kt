package com.jameszp98.aiquota.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jameszp98.aiquota.data.ProviderRegistry
import com.jameszp98.aiquota.data.storage.SettingsStore
import com.jameszp98.aiquota.ui.widget.QuotaWidget
import com.jameszp98.aiquota.ui.widget.WidgetState
import com.jameszp98.aiquota.ui.widget.WidgetStateDefinition
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.concurrent.TimeUnit

class UsageRefreshWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val settingsStore = SettingsStore(context)
            val demoMode = settingsStore.demoMode.first()
            val registry = ProviderRegistry.getInstance(context)

            val providers = registry.fetchAllUsage(demoMode)

            val state = WidgetState(
                providers = providers,
                lastUpdated = Instant.now(),
                isDemo = demoMode
            )

            updateAllWidgets(state)

            if (providers.any { it.hasError }) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun updateAllWidgets(state: WidgetState) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(QuotaWidget::class.java)

        glanceIds.forEach { glanceId ->
            updateAppWidgetState(
                context = context,
                definition = WidgetStateDefinition,
                glanceId = glanceId
            ) {
                state
            }
            QuotaWidget().update(context, glanceId)
        }
    }

    companion object {
        private const val WORK_NAME = "ai_quota_refresh"

        fun schedulePeriodicRefresh(context: Context, intervalMinutes: Int) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<UsageRefreshWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancelPeriodicRefresh(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
