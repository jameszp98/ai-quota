package com.jameszp98.aiquota.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jameszp98.aiquota.data.storage.CursorTokenState
import com.jameszp98.aiquota.domain.ProviderUsage
import com.jameszp98.aiquota.domain.QuotaBucket
import com.jameszp98.aiquota.domain.QuotaWindow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val cursorTokenState by viewModel.cursorTokenState.collectAsState()
    val demoMode by viewModel.demoMode.collectAsState()
    val refreshInterval by viewModel.refreshInterval.collectAsState()
    val usageState by viewModel.usageState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Quota / 额度看板") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Demo mode toggle
            DemoModeCard(
                demoMode = demoMode,
                onDemoModeChange = viewModel::setDemoMode
            )

            // Cursor provider settings
            CursorProviderCard(
                tokenState = cursorTokenState,
                saveState = saveState,
                onSaveBearerToken = viewModel::saveCursorBearerToken,
                onSaveSessionToken = viewModel::saveCursorSessionToken,
                onClearTokens = viewModel::clearCursorTokens,
                onResetSaveState = viewModel::resetSaveState
            )

            // Refresh settings
            RefreshSettingsCard(
                refreshInterval = refreshInterval,
                onIntervalChange = viewModel::setRefreshInterval,
                onRefreshNow = viewModel::refreshUsage
            )

            // Current usage display
            UsageDisplayCard(usageState = usageState)

            // Info section
            InfoCard()
        }
    }
}

@Composable
private fun DemoModeCard(
    demoMode: Boolean,
    onDemoModeChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (demoMode) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Demo Mode / 演示模式",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Show mock data without authentication",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = demoMode,
                onCheckedChange = onDemoModeChange
            )
        }
    }
}

@Composable
private fun CursorProviderCard(
    tokenState: CursorTokenState,
    saveState: SaveState,
    onSaveBearerToken: (String) -> Unit,
    onSaveSessionToken: (String) -> Unit,
    onClearTokens: () -> Unit,
    onResetSaveState: () -> Unit
) {
    var bearerToken by remember { mutableStateOf("") }
    var sessionToken by remember { mutableStateOf("") }
    var showBearerToken by remember { mutableStateOf(false) }
    var showSessionToken by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Cursor",
                style = MaterialTheme.typography.titleLarge
            )

            // Token status
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = when (tokenState) {
                        CursorTokenState.NoToken -> "Not configured"
                        CursorTokenState.HasBearerToken -> "Bearer token set ✓"
                        CursorTokenState.HasSessionToken -> "Session token set ✓"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (tokenState) {
                        CursorTokenState.NoToken -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }

            // Save state feedback
            when (saveState) {
                is SaveState.Saving -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is SaveState.Success -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Token saved!", color = MaterialTheme.colorScheme.primary)
                    }
                }
                is SaveState.Error -> {
                    Text(
                        text = "Error: ${saveState.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                SaveState.Idle -> {}
            }

            // Bearer token input
            Text(
                text = "Bearer Token (preferred)",
                style = MaterialTheme.typography.labelMedium
            )
            OutlinedTextField(
                value = bearerToken,
                onValueChange = { bearerToken = it; onResetSaveState() },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste your Bearer token here") },
                visualTransformation = if (showBearerToken) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showBearerToken = !showBearerToken }) {
                        Icon(
                            if (showBearerToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle visibility"
                        )
                    }
                },
                singleLine = true
            )
            Button(
                onClick = { onSaveBearerToken(bearerToken); bearerToken = "" },
                enabled = bearerToken.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Bearer Token")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Session token input
            Text(
                text = "Session Token (alternative)",
                style = MaterialTheme.typography.labelMedium
            )
            OutlinedTextField(
                value = sessionToken,
                onValueChange = { sessionToken = it; onResetSaveState() },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste WorkosCursorSessionToken here") },
                visualTransformation = if (showSessionToken) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showSessionToken = !showSessionToken }) {
                        Icon(
                            if (showSessionToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle visibility"
                        )
                    }
                },
                singleLine = true
            )
            Button(
                onClick = { onSaveSessionToken(sessionToken); sessionToken = "" },
                enabled = sessionToken.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Session Token")
            }

            // Clear tokens
            if (tokenState != CursorTokenState.NoToken) {
                OutlinedButton(
                    onClick = onClearTokens,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Cursor Tokens")
                }
            }

            // Instructions
            Text(
                text = """
                    How to get your token:
                    1. Open cursor.com in your browser
                    2. Log in to your account
                    3. Open DevTools (F12) → Application → Cookies
                    4. Copy the value of 'WorkosCursorSessionToken'
                    
                    Or for Bearer token, check Network tab for API requests.
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RefreshSettingsCard(
    refreshInterval: Int,
    onIntervalChange: (Int) -> Unit,
    onRefreshNow: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Refresh Settings / 刷新设置",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Auto-refresh every $refreshInterval minutes",
                style = MaterialTheme.typography.bodyMedium
            )

            Slider(
                value = refreshInterval.toFloat(),
                onValueChange = { onIntervalChange(it.toInt()) },
                valueRange = 15f..120f,
                steps = 6
            )

            Button(
                onClick = onRefreshNow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Refresh Now")
            }
        }
    }
}

@Composable
private fun UsageDisplayCard(usageState: UsageUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Current Usage / 当前用量",
                style = MaterialTheme.typography.titleMedium
            )

            when (usageState) {
                UsageUiState.Idle -> {
                    Text(
                        text = "Press Refresh to fetch usage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                UsageUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is UsageUiState.Error -> {
                    Text(
                        text = usageState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is UsageUiState.Success -> {
                    usageState.providers.forEach { provider ->
                        ProviderUsageDisplay(provider)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderUsageDisplay(provider: ProviderUsage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = provider.displayName,
                style = MaterialTheme.typography.titleSmall
            )
            provider.planName?.let { plan ->
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "($plan)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (provider.isDemo) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "[Demo]",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        if (provider.hasError) {
            Text(
                text = provider.errorMessage ?: "Error fetching data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            provider.buckets.forEach { bucket ->
                BucketDisplay(bucket)
            }
        }
    }
}

@Composable
private fun BucketDisplay(bucket: QuotaBucket) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Text(
                    text = bucket.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (bucket.window != QuotaWindow.MONTHLY && bucket.window != QuotaWindow.NONE) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (bucket.window) {
                            QuotaWindow.WEEKLY -> "(weekly)"
                            QuotaWindow.ROLLING_HOURS -> "(${bucket.rollingWindowHours}h)"
                            QuotaWindow.DAILY -> "(daily)"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = if (bucket.isUnlimited) "Unlimited"
                else "${bucket.displayUsed} / ${bucket.displayLimit ?: "∞"}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (!bucket.isUnlimited && bucket.limit != null) {
            LinearProgressIndicator(
                progress = { (bucket.percentUsed / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "About / 关于",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = """
                    This app displays AI usage quotas from various providers.
                    
                    Cursor quotas:
                    • Cursor Models: Monthly request limit (first-party models)
                    • Other Models: Monthly dollar allowance (third-party models)
                    • On-demand: Pay-as-you-go spending (optional)
                    • Resets with billing cycle (NOT calendar month)
                    • NO weekly or hourly quotas for Cursor
                    
                    ⚠️ UNOFFICIAL API
                    This app uses community-documented APIs that may change without notice.
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
