package com.czh.rediscompose.feature.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.czh.rediscompose.app.SessionStatus
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.domain.connection.ConnectionColor
import com.czh.rediscompose.domain.connection.ConnectionEnvironment
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.ConnectionTestResult
import com.czh.rediscompose.domain.connection.SshAuthMethod
import com.czh.rediscompose.domain.connection.SshConfig
import com.czh.rediscompose.domain.connection.TlsConfig
import com.czh.rediscompose.util.currentEpochMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private fun previewProfiles(): List<ConnectionProfile> {
    val now = currentEpochMillis()
    return listOf(
        ConnectionProfile(
            id = "prod-cache",
            name = "Prod Cache",
            environment = ConnectionEnvironment.PRODUCTION,
            host = "redis-01.acme.io",
            port = 6379,
            group = "Production",
            color = ConnectionColor.IRIS,
            tls = TlsConfig(enabled = true),
            lastOpenedAtEpochMillis = now - 120_000,
            createdAtEpochMillis = now - 90L * 86_400_000,
        ),
        ConnectionProfile(
            id = "session-store",
            name = "Session Store",
            environment = ConnectionEnvironment.PRODUCTION,
            host = "127.0.0.1",
            port = 6380,
            defaultDatabase = 2,
            group = "Production",
            color = ConnectionColor.MINT,
            ssh =
                SshConfig(
                    enabled = true,
                    host = "bastion.acme.io",
                    username = "deploy",
                    authMethod = SshAuthMethod.PRIVATE_KEY,
                    privateKeyPath = "~/.ssh/id_ed25519",
                ),
            lastOpenedAtEpochMillis = now - 4L * 60_000,
        ),
        ConnectionProfile(
            id = "analytics",
            name = "Analytics Replica",
            environment = ConnectionEnvironment.ANALYTICS,
            host = "redis-analytics.acme.io",
            port = 6379,
            group = "Analytics",
            color = ConnectionColor.SKY,
            readOnly = true,
            lastOpenedAtEpochMillis = now - 11L * 60_000,
        ),
        ConnectionProfile(
            id = "billing",
            name = "Billing Staging",
            environment = ConnectionEnvironment.STAGING,
            host = "staging-billing.acme.io",
            port = 6379,
            group = "Staging",
            color = ConnectionColor.AMBER,
            lastOpenedAtEpochMillis = now - 60L * 60_000,
        ),
        ConnectionProfile(
            id = "local",
            name = "Local Sandbox",
            environment = ConnectionEnvironment.LOCAL,
            host = "127.0.0.1",
            port = 6379,
            group = "Local",
            color = ConnectionColor.LILAC,
            lastOpenedAtEpochMillis = now - 26L * 3_600_000,
        ),
        ConnectionProfile(
            id = "legacy",
            name = "Legacy Cache",
            environment = ConnectionEnvironment.STAGING,
            host = "legacy-cache.internal",
            port = 6380,
            group = "Staging",
            color = ConnectionColor.ROSE,
            lastOpenedAtEpochMillis = now - 2L * 86_400_000,
        ),
    )
}

private fun previewRuntime(connectionId: String): ConnectionRuntime =
    when (connectionId) {
        "prod-cache" ->
            ConnectionRuntime(
                status = SessionStatus.CONNECTED,
                latencyMillis = 12,
                version = "7.4.2",
                totalKeys = 24_585,
                connectedClients = 24,
                databaseCount = 16,
            )
        "session-store" ->
            ConnectionRuntime(
                status = SessionStatus.CONNECTED,
                latencyMillis = 8,
                version = "7.4.2",
                totalKeys = 8_201,
                connectedClients = 12,
                databaseCount = 16,
            )
        "analytics" ->
            ConnectionRuntime(
                status = SessionStatus.CONNECTED,
                latencyMillis = 18,
                version = "7.4.2",
                totalKeys = 184_920,
                connectedClients = 7,
                databaseCount = 16,
            )
        "legacy" ->
            ConnectionRuntime(
                status = SessionStatus.ERROR,
                errorMessage = "connection refused",
            )
        else -> ConnectionRuntime()
    }

private fun previewConnectionsState(
    profiles: List<ConnectionProfile> = previewProfiles(),
    testOutcome: ConnectionTestResult = ConnectionTestResult(success = true, latencyMillis = 12, version = "7.4.2"),
    open: ((ConnectionsState) -> Unit)? = null,
): ConnectionsState {
    val state =
        ConnectionsState(
            connections = { profiles },
            runtimeFor = ::previewRuntime,
            onOpen = {},
            onSave = {},
            onDelete = {},
            onDuplicate = { id ->
                profiles.firstOrNull { it.id == id }?.let { source ->
                    source.copy(id = "$id-copy", name = "${source.name} 副本")
                }
            },
            onTest = { testOutcome },
            onToast = { _, _ -> },
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
    open?.invoke(state)
    return state
}

@Preview(widthDp = 1440, heightDp = 900)
@Composable
private fun ConnectionsScreenDarkPreview() {
    val state = remember { previewConnectionsState() }
    AppTheme(dark = true) {
        ConnectionsScreen(state = state)
    }
}

@Preview(widthDp = 1120, heightDp = 720)
@Composable
private fun ConnectionsScreenLightPreview() {
    val state = remember { previewConnectionsState() }
    AppTheme(dark = false) {
        ConnectionsScreen(state = state)
    }
}

@Preview(widthDp = 1440, heightDp = 900)
@Composable
private fun ConnectionsScreenListPreview() {
    val state = remember { previewConnectionsState { it.chooseView(ConnectionView.LIST) } }
    AppTheme(dark = true) {
        ConnectionsScreen(state = state)
    }
}

@Preview(widthDp = 1120, heightDp = 720)
@Composable
private fun ConnectionsScreenEmptyPreview() {
    val state = remember { previewConnectionsState(emptyList()) }
    AppTheme(dark = false) {
        ConnectionsScreen(state = state)
    }
}

@Preview(widthDp = 1120, heightDp = 720)
@Composable
private fun ConnectionsScreenDeletePreview() {
    val state = remember { previewConnectionsState { it.requestDelete("legacy") } }
    AppTheme(dark = true) {
        ConnectionsScreen(state = state)
    }
}

@Preview(widthDp = 420, heightDp = 220)
@Composable
private fun ConnectionCardPreview() {
    AppTheme(dark = true) {
        Box(
            Modifier
                .background(LocalAppColors.current.background)
                .fillMaxSize(),
        ) {
            ConnectionCard(
                profile = previewProfiles()[0],
                runtime = previewRuntime("prod-cache"),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(widthDp = 420, heightDp = 220)
@Composable
private fun ConnectionCardErrorPreview() {
    AppTheme(dark = false) {
        Box(
            Modifier
                .background(LocalAppColors.current.background)
                .fillMaxSize(),
        ) {
            ConnectionCard(
                profile = previewProfiles()[5],
                runtime = previewRuntime("legacy"),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(widthDp = 740, heightDp = 110)
@Composable
private fun AddConnectionRowPreview() {
    AppTheme(dark = true) {
        Box(Modifier.background(LocalAppColors.current.background).fillMaxSize()) {
            AddConnectionRow(onClick = {})
        }
    }
}

@Preview(widthDp = 1074, heightDp = 420)
@Composable
private fun ConnectionsHeaderPreview() {
    AppTheme(dark = false) {
        Box(Modifier.background(LocalAppColors.current.background).fillMaxSize()) {
            ConnectionsHeader(onlineCount = 3, totalCount = 6, onNew = {})
        }
    }
}

@Preview(widthDp = 1074, heightDp = 360)
@Composable
private fun RecentConnectionsTablePreview() {
    AppTheme(dark = true) {
        Box(Modifier.background(LocalAppColors.current.background).fillMaxSize()) {
            RecentConnectionsTable(
                connections = previewProfiles(),
                runtimeFor = ::previewRuntime,
                onOpen = {},
            )
        }
    }
}

@Preview(widthDp = 470, heightDp = 900)
@Composable
private fun ConnectionSheetBasicPreview() {
    val state = remember { previewConnectionsState { it.openNew() } }
    AppTheme(dark = true) {
        Box(Modifier.background(LocalAppColors.current.background).fillMaxSize()) {
            ConnectionSheet(state = state)
        }
    }
}

@Preview(widthDp = 470, heightDp = 900)
@Composable
private fun ConnectionSheetTlsPreview() {
    val state =
        remember {
            previewConnectionsState {
                it.openNew()
                it.selectSheetTab(ConnectionSheetTab.TLS)
            }
        }
    AppTheme(dark = false) {
        Box(Modifier.background(LocalAppColors.current.background).fillMaxSize()) {
            ConnectionSheet(state = state)
        }
    }
}

@Preview(widthDp = 470, heightDp = 900)
@Composable
private fun ConnectionSheetSshPreview() {
    val state =
        remember {
            previewConnectionsState {
                it.openNew()
                it.selectSheetTab(ConnectionSheetTab.SSH)
            }
        }
    AppTheme(dark = true) {
        Box(Modifier.background(LocalAppColors.current.background).fillMaxSize()) {
            ConnectionSheet(state = state)
        }
    }
}

@Preview(widthDp = 470, heightDp = 900)
@Composable
private fun ConnectionSheetAdvancedPreview() {
    val state =
        remember {
            previewConnectionsState {
                it.openNew()
                it.selectSheetTab(ConnectionSheetTab.ADVANCED)
            }
        }
    AppTheme(dark = false) {
        Box(Modifier.background(LocalAppColors.current.background).fillMaxSize()) {
            ConnectionSheet(state = state)
        }
    }
}

@Preview(widthDp = 470, heightDp = 900)
@Composable
private fun ConnectionSheetTestSuccessPreview() {
    val state =
        remember {
            previewConnectionsState {
                it.openNew()
                it.updateDraft { draft -> draft.copy(name = "Preview") }
                it.testDraft()
            }
        }
    AppTheme(dark = true) {
        Box(Modifier.background(LocalAppColors.current.background).fillMaxSize()) {
            ConnectionSheet(state = state)
        }
    }
}

@Preview(widthDp = 470, heightDp = 900)
@Composable
private fun ConnectionSheetTestFailurePreview() {
    val state =
        remember {
            previewConnectionsState(
                testOutcome = ConnectionTestResult(success = false, errorMessage = "connection refused"),
            ) {
                it.openNew()
                it.updateDraft { draft -> draft.copy(name = "Preview") }
                it.testDraft()
            }
        }
    AppTheme(dark = false) {
        Box(Modifier.background(LocalAppColors.current.background).fillMaxSize()) {
            ConnectionSheet(state = state)
        }
    }
}

@Preview(widthDp = 240, heightDp = 240)
@Composable
private fun ConnectionCardMenuPreview() {
    AppTheme(dark = true) {
        Box(Modifier.background(LocalAppColors.current.background).fillMaxSize()) {
            ConnectionCardMenu(
                expanded = true,
                testing = false,
                onDismiss = {},
                onOpen = {},
                onEdit = {},
                onDuplicate = {},
                onTest = {},
                onDelete = {},
            )
        }
    }
}
