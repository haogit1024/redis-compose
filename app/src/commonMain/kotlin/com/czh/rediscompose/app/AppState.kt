package com.czh.rediscompose.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
        tabs.removeAt(index)
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
        connectJobs.remove(profile.id)?.cancel()
        sessions.remove(profile.id)?.disconnect()
        for (tabIndex in tabs.indices) {
            val tab = tabs[tabIndex]
            if (tab.connectionId == profile.id && tab.kind == WorkspaceTabKind.KEYS) {
                tabs[tabIndex] = tab.copy(title = profile.name)
            }
        }
        persistConnections()
    }

    fun deleteConnection(connectionId: String) {
        connectJobs.remove(connectionId)?.cancel()
        sessions.remove(connectionId)?.disconnect()
        tabs.removeAll { it.connectionId == connectionId }
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
        sessions.values.forEach { it.disconnect() }
        sessions.clear()
        toastController.clear()
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
}
