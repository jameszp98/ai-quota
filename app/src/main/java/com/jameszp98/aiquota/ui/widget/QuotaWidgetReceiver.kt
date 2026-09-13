package com.jameszp98.aiquota.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import com.jameszp98.aiquota.data.ProviderRegistry
import com.jameszp98.aiquota.data.storage.SettingsStore
import com.jameszp98.aiquota.worker.UsageRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

class QuotaWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = QuotaWidget()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        scope.launch {
            val settingsStore = SettingsStore(context)
            val interval = settingsStore.refreshIntervalMinutes.first()
            UsageRefreshWorker.schedulePeriodicRefresh(context, interval)
            refreshAllWidgets(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_REFRESH) {
            scope.launch {
                refreshAllWidgets(context)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)

        scope.launch {
            val manager = GlanceAppWidgetManager(context)
            val remainingIds = manager.getGlanceIds(QuotaWidget::class.java)
            if (remainingIds.isEmpty()) {
                UsageRefreshWorker.cancelPeriodicRefresh(context)
            }
        }
    }

    private suspend fun refreshAllWidgets(context: Context) {
        val settingsStore = SettingsStore(context)
        val demoMode = settingsStore.demoMode.first()
        val registry = ProviderRegistry.getInstance(context)

        val providers = registry.fetchAllUsage(demoMode)

        val state = WidgetState(
            providers = providers,
            lastUpdated = Instant.now(),
            isDemo = demoMode
        )

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
            glanceAppWidget.update(context, glanceId)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.jameszp98.aiquota.ACTION_REFRESH"

        fun requestRefresh(context: Context) {
            val intent = Intent(context, QuotaWidgetReceiver::class.java).apply {
                action = ACTION_REFRESH
            }
            context.sendBroadcast(intent)
        }
    }
}
