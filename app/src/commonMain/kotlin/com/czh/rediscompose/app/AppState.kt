package com.czh.rediscompose.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.czh.rediscompose.data.redis.InfoParser
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.ConnectionTestResult
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.ThemeMode
import com.czh.rediscompose.feature.connections.ConnectionRuntime
import com.czh.rediscompose.feature.connections.ConnectionsState
import com.czh.rediscompose.feature.console.ConsoleState
import com.czh.rediscompose.feature.insights.InsightsState
import com.czh.rediscompose.feature.workspace.WorkspaceState
import com.czh.rediscompose.util.currentEpochMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.TimeSource

/**
 * Global app state: settings, connections, sessions, workspace tabs and toasts.
 *
 * Feature screens read the pieces they need; all mutations go through the methods below
 * so persistence and session cleanup stay in one place.
 */
class AppState(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    initialSettings: AppSettings,
    initialConnections: List<ConnectionProfile>,
) {
    var settings by mutableStateOf(initialSettings)
        private set

    var connections by mutableStateOf(initialConnections)
        private set

    val sessions = mutableStateMapOf<String, RedisSession>()
    val tabs = mutableStateListOf<WorkspaceTab>()

    private val workspaces = mutableMapOf<String, WorkspaceState>()
    private val consoles = mutableMapOf<String, ConsoleState>()
    private val insights = mutableMapOf<String, InsightsState>()

    private val connectionLibraryDelegate =
        lazy {
            ConnectionsState(
                connections = { connections },
                runtimeFor = ::connectionRuntime,
                onOpen = ::openConnection,
                onSave = ::saveConnection,
                onDelete = ::deleteConnection,
                onDuplicate = ::duplicateConnection,
                onTest = ::testConnection,
                onToast = { message, kind -> showToast(message, kind) },
                scope = scope,
            )
        }

    val connectionLibrary: ConnectionsState by connectionLibraryDelegate

    var activePage by mutableStateOf(AppPage.WORKSPACE)
        private set

    var activeTabId by mutableStateOf<String?>(null)
        private set

    var activeConnectionId by mutableStateOf<String?>(null)
        private set

    var commandPaletteVisible by mutableStateOf(false)
        private set

    private val toastController = ToastController(scope)
    private val connectJobs = mutableMapOf<String, Job>()

    val toasts: List<ToastMessage> get() = toastController.toasts

    val activeConnection: ConnectionProfile?
        get() = connections.firstOrNull { it.id == activeConnectionId }

    fun activeSession(): RedisSession? = activeConnectionId?.let { sessions[it] }

    fun activeTab(): WorkspaceTab? = tabs.firstOrNull { it.id == activeTabId }

    fun sessionFor(connectionId: String): RedisSession? = sessions[connectionId]

    /** Key browser state for a connection, created when its Keys tab opens. */
    fun workspaceFor(connectionId: String): WorkspaceState? = workspaces[connectionId]

    /** Command console state for a connection, created when its Console tab opens. */
    fun consoleFor(connectionId: String): ConsoleState? = consoles[connectionId]

    /** Insights state for a connection, created when its Insights tab opens. */
    fun insightsFor(connectionId: String): InsightsState? = insights[connectionId]

    fun setPage(page: AppPage) {
        activePage = page
        commandPaletteVisible = false
    }

    fun setPaletteVisible(visible: Boolean) {
        commandPaletteVisible = visible
    }

    fun toggleCommandPalette() {
        commandPaletteVisible = !commandPaletteVisible
    }

    fun openConnection(connectionId: String) {
        openTab(connectionId, WorkspaceTabKind.KEYS)
    }

    fun openTab(
        connectionId: String,
        kind: WorkspaceTabKind,
    ) {
        val profile = connections.firstOrNull { it.id == connectionId } ?: return
        val openedAt = currentEpochMillis()
        if (profile.lastOpenedAtEpochMillis != openedAt) {
            connections =
                connections.map { connection ->
                    if (connection.id == connectionId) {
                        connection.copy(lastOpenedAtEpochMillis = openedAt)
                    } else {
                        connection
                    }
                }
            persistConnections()
        }
        val session = ensureSession(profile)
        ensureConnected(session)

        activePage = AppPage.WORKSPACE
        activeConnectionId = connectionId
        val tabId = WorkspaceTab.idFor(connectionId, kind)
        if (tabs.none { it.id == tabId }) {
            tabs.add(
                WorkspaceTab(
                    id = tabId,
                    connectionId = connectionId,
                    kind = kind,
                    title = tabTitle(profile, kind),
                ),
            )
        }
        if (kind == WorkspaceTabKind.KEYS) ensureWorkspace(connectionId, session)
        if (kind == WorkspaceTabKind.CONSOLE) ensureConsole(connectionId, session)
        if (kind == WorkspaceTabKind.INSIGHTS) ensureInsights(connectionId, session)
        activeTabId = tabId
        commandPaletteVisible = false
    }

    fun selectTab(tabId: String) {
        val tab = tabs.firstOrNull { it.id == tabId } ?: return
        activeTabId = tabId
        activeConnectionId = tab.connectionId
        activePage = AppPage.WORKSPACE
    }

    fun closeTab(tabId: String) {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index < 0) return
        val closing = tabs.removeAt(index)
        when (closing.kind) {
            WorkspaceTabKind.KEYS -> workspaces.remove(closing.connectionId)?.dispose()
            WorkspaceTabKind.CONSOLE -> consoles.remove(closing.connectionId)?.dispose()
            WorkspaceTabKind.INSIGHTS -> insights.remove(closing.connectionId)?.dispose()
        }
        if (activeTabId == tabId) {
            val next = tabs.getOrNull(index) ?: tabs.getOrNull(index - 1)
            activeTabId = next?.id
            if (next != null) activeConnectionId = next.connectionId
        }
    }

    fun selectDatabase(
        connectionId: String,
        index: Int,
    ) {
        val session = sessions[connectionId] ?: return
        if (!session.isConnected) {
            showToast("请先连接后再切换数据库", ToastKind.Warning)
            return
        }
        if (index !in 0 until RedisSession.DATABASE_COUNT) return
        scope.launch {
            runCatching { session.switchDatabase(index) }
                .onSuccess { workspaces[connectionId]?.refresh() }
                .onFailure { error ->
                    showToast("切换数据库失败：${error.firstLine()}", ToastKind.Danger)
                }
        }
    }

    fun updateSettings(settings: AppSettings) {
        this.settings = settings
        scope.launch {
            container.settingsRepository.save(settings)
        }
    }

    fun updateTheme(theme: ThemeMode) {
        updateSettings(settings.copy(theme = theme))
    }

    fun saveConnection(profile: ConnectionProfile) {
        val index = connections.indexOfFirst { it.id == profile.id }
        connections =
            if (index >= 0) {
                connections.toMutableList().also { it[index] = profile }
            } else {
                connections + profile
            }
        // Connection-critical fields may have changed, so the old session and its tabs
        // must not keep using the previous gateway.
        disposeConnectionRuntime(profile.id)
        persistConnections()
    }

    fun deleteConnection(connectionId: String) {
        disposeConnectionRuntime(connectionId)
        connections = connections.filterNot { it.id == connectionId }
        if (activeConnectionId == connectionId) {
            activeConnectionId = tabs.firstOrNull { it.id == activeTabId }?.connectionId
                ?: connections.firstOrNull()?.id
        }
        if (activeTabId != null && tabs.none { it.id == activeTabId }) {
            activeTabId = tabs.lastOrNull()?.id
            tabs.lastOrNull()?.let { activeConnectionId = it.connectionId }
        }
        persistConnections()
    }

    /** Profile for the "复制连接" action; the caller opens the sheet with this draft. */
    fun duplicateConnection(connectionId: String): ConnectionProfile? {
        val source = connections.firstOrNull { it.id == connectionId } ?: return null
        val now = currentEpochMillis()
        return source.copy(
            id = "conn-$now-copy-${Random.nextInt(1_000, 10_000)}",
            name = "${source.name} 副本",
            lastOpenedAtEpochMillis = null,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
    }

    /** Opens a throwaway gateway for the sheet's "测试连接" action; always closes it. */
    suspend fun testConnection(profile: ConnectionProfile): ConnectionTestResult {
        val gateway = container.gatewayFactory.create(profile)
        val started = TimeSource.Monotonic.markNow()
        return try {
            gateway.ping()
            val latency = started.elapsedNow().inWholeMilliseconds
            val version =
                try {
                    InfoParser.snapshot(gateway.info()).version
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            ConnectionTestResult(
                success = true,
                latencyMillis = latency,
                version = version,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ConnectionTestResult(
                success = false,
                errorMessage = error.firstLine(),
            )
        } finally {
            runCatching { gateway.close() }
        }
    }

    fun showToast(
        message: String,
        kind: ToastKind = ToastKind.Info,
        detail: String? = null,
    ) {
        toastController.show(message, kind, detail)
    }

    fun dismissToast(id: Long) {
        toastController.dismiss(id)
    }

    fun close() {
        connectJobs.values.forEach { it.cancel() }
        connectJobs.clear()
        workspaces.values.forEach { it.dispose() }
        workspaces.clear()
        consoles.values.forEach { it.dispose() }
        consoles.clear()
        insights.values.forEach { it.dispose() }
        insights.clear()
        if (connectionLibraryDelegate.isInitialized()) connectionLibrary.dispose()
        sessions.values.forEach { it.disconnect() }
        sessions.clear()
        toastController.clear()
    }

    private fun disposeConnectionRuntime(connectionId: String) {
        connectJobs.remove(connectionId)?.cancel()
        sessions.remove(connectionId)?.disconnect()
        workspaces.remove(connectionId)?.dispose()
        consoles.remove(connectionId)?.dispose()
        insights.remove(connectionId)?.dispose()
        tabs.removeAll { it.connectionId == connectionId }
        if (activeTabId != null && tabs.none { it.id == activeTabId }) {
            activeTabId = tabs.lastOrNull()?.id
            activeConnectionId = tabs.lastOrNull()?.connectionId
        }
        if (activeConnectionId == connectionId) {
            activeConnectionId = tabs.firstOrNull { it.id == activeTabId }?.connectionId
        }
    }

    private fun connectionRuntime(connectionId: String): ConnectionRuntime {
        val session = sessions[connectionId] ?: return ConnectionRuntime()
        return ConnectionRuntime(
            status = session.status,
            latencyMillis = session.latencyMillis,
            errorMessage = session.errorMessage,
            version = session.summary?.version,
            totalKeys = session.summary?.totalKeys,
            connectedClients = session.summary?.connectedClients,
            databaseCount = session.databases.size.takeIf { it > 0 },
        )
    }

    private fun persistConnections() {
        val snapshot = connections
        scope.launch {
            container.connectionRepository.save(snapshot)
        }
    }

    private fun ensureSession(profile: ConnectionProfile): RedisSession =
        sessions[profile.id] ?: RedisSession(
            profile = profile,
            gateway = container.gatewayFactory.create(profile),
            appScope = scope,
        ).also { sessions[profile.id] = it }

    private fun ensureWorkspace(
        connectionId: String,
        session: RedisSession,
    ): WorkspaceState =
        workspaces.getOrPut(connectionId) {
            WorkspaceState(
                session = session,
                settings = { settings },
                onToast = { message, kind -> showToast(message, kind) },
                scope = session.scope,
                onSettingsChange = ::updateSettings,
                onOpenConsole = { openTab(connectionId, WorkspaceTabKind.CONSOLE) },
            )
        }

    private fun ensureConsole(
        connectionId: String,
        session: RedisSession,
    ): ConsoleState =
        consoles.getOrPut(connectionId) {
            ConsoleState(
                session = session,
                settings = { settings },
                onToast = { message, kind -> showToast(message, kind) },
                onRecordCommand = ::recordCommand,
                scope = session.scope,
                onDatabaseSelected = { workspaces[connectionId]?.refresh() },
            )
        }

    private fun ensureInsights(
        connectionId: String,
        session: RedisSession,
    ): InsightsState =
        insights.getOrPut(connectionId) {
            InsightsState(
                session = session,
                scope = session.scope,
                onToast = { message, kind -> showToast(message, kind) },
            )
        }

    private fun recordCommand(command: String) {
        val current = settings.recentCommands
        val updated =
            if (current.lastOrNull() == command) {
                current
            } else {
                (current + command).takeLast(RECENT_COMMAND_LIMIT)
            }
        if (updated != current) {
            updateSettings(settings.copy(recentCommands = updated))
        }
    }

    private fun ensureConnected(session: RedisSession) {
        if (session.status == SessionStatus.CONNECTING || session.status == SessionStatus.CONNECTED) {
            return
        }
        connectJobs.remove(session.profile.id)?.cancel()
        connectJobs[session.profile.id] = scope.launch { session.connect() }
    }

    private fun tabTitle(
        profile: ConnectionProfile,
        kind: WorkspaceTabKind,
    ): String =
        when (kind) {
            WorkspaceTabKind.KEYS -> profile.name
            WorkspaceTabKind.CONSOLE -> "Console"
            WorkspaceTabKind.INSIGHTS -> "Insights"
        }

    private fun Throwable.firstLine(): String =
        message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: this::class.simpleName
            ?: "未知错误"

    private companion object {
        const val RECENT_COMMAND_LIMIT = 20
    }
}
