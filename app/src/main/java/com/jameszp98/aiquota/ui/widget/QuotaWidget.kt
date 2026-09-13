package com.jameszp98.aiquota.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jameszp98.aiquota.domain.ProviderUsage
import com.jameszp98.aiquota.domain.QuotaBucket
import com.jameszp98.aiquota.domain.QuotaUnit
import com.jameszp98.aiquota.domain.QuotaWindow
import com.jameszp98.aiquota.ui.settings.SettingsActivity
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class QuotaWidget : GlanceAppWidget() {

    override val stateDefinition = WidgetStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        val state = currentState<WidgetState>()

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF1E1E2E))
                .padding(12.dp)
                .clickable(actionStartActivity<SettingsActivity>())
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.Start
            ) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Quota",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFCDD6F4)),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    if (state.isDemo) {
                        Text(
                            text = "(Demo)",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFF9E2AF)),
                                fontSize = 10.sp
                            )
                        )
                    }
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Box(
                        modifier = GlanceModifier
                            .size(24.dp)
                            .clickable(actionRunCallback<RefreshAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "↻",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF89B4FA)),
                                fontSize = 16.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                if (state.providers.isEmpty()) {
                    Text(
                        text = "No providers configured",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFA6ADC8)),
                            fontSize = 11.sp
                        )
                    )
                } else {
                    state.providers.forEach { provider ->
                        ProviderSection(provider)
                        Spacer(modifier = GlanceModifier.height(8.dp))
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Last updated
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(ZoneId.systemDefault())
                Text(
                    text = "Updated ${formatter.format(state.lastUpdated)}",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF45475A)),
                        fontSize = 8.sp
                    )
                )
            }
        }
    }

    @Composable
    private fun ProviderSection(provider: ProviderUsage) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            // Provider header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = provider.displayName,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFCDD6F4)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                provider.planName?.let { plan ->
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "($plan)",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFA6ADC8)),
                            fontSize = 9.sp
                        )
                    )
                }
            }

            if (provider.hasError) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = provider.errorMessage ?: "Error",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFF38BA8)),
                        fontSize = 9.sp
                    )
                )
            } else {
                Spacer(modifier = GlanceModifier.height(4.dp))

                // Quota buckets
                val colors = listOf(
                    Color(0xFF89B4FA), // Blue
                    Color(0xFFA6E3A1), // Green
                    Color(0xFFF9E2AF)  // Yellow
                )

                provider.buckets.forEachIndexed { index, bucket ->
                    QuotaRow(
                        bucket = bucket,
                        accentColor = colors[index % colors.size]
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }

                // Reset date (from first bucket with resetsAt)
                provider.buckets.firstOrNull { it.resetsAt != null }?.resetsAt?.let { resetDate ->
                    val formatter = DateTimeFormatter.ofPattern("MMM d")
                        .withZone(ZoneId.systemDefault())
                    Text(
                        text = "Resets ${formatter.format(resetDate)}",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF6C7086)),
                            fontSize = 9.sp
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun QuotaRow(bucket: QuotaBucket, accentColor: Color) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bucket name with window indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bucket.name,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFBAC2DE)),
                            fontSize = 10.sp
                        )
                    )
                    if (bucket.window != QuotaWindow.MONTHLY && bucket.window != QuotaWindow.NONE) {
                        Spacer(modifier = GlanceModifier.width(2.dp))
                        Text(
                            text = getWindowLabel(bucket),
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF6C7086)),
                                fontSize = 8.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Usage display
                when {
                    bucket.isUnlimited -> {
                        Text(
                            text = "Unlimited",
                            style = TextStyle(
                                color = ColorProvider(accentColor),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    else -> {
                        Text(
                            text = bucket.displayUsed,
                            style = TextStyle(
                                color = ColorProvider(accentColor),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        bucket.displayLimit?.let { limit ->
                            Text(
                                text = " / $limit",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF6C7086)),
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }
            }

            // Progress bar
            if (!bucket.isUnlimited && bucket.limit != null) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                ProgressBar(
                    progress = (bucket.percentUsed / 100.0).toFloat().coerceIn(0f, 1f),
                    color = accentColor
                )
            }
        }
    }

    private fun getWindowLabel(bucket: QuotaBucket): String = when (bucket.window) {
        QuotaWindow.WEEKLY -> "(wk)"
        QuotaWindow.ROLLING_HOURS -> "(${bucket.rollingWindowHours}h)"
        QuotaWindow.DAILY -> "(day)"
        else -> ""
    }

    @Composable
    private fun ProgressBar(progress: Float, color: Color) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color(0xFF313244))
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth(progress)
                    .height(3.dp)
                    .background(color)
            ) {}
        }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        QuotaWidgetReceiver.requestRefresh(context)
    }
}
