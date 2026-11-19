package com.czh.rediscompose.app

import com.czh.rediscompose.data.redis.RedisException
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.settings.Accent
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppStateTest {
    private fun profile(
        id: String = "c1",
        name: String = "Local",
    ) = ConnectionProfile(
        id = id,
        name = name,
        host = "127.0.0.1",
    )

    private fun state(
        container: FakeAppContainer,
        connections: List<ConnectionProfile> = container.initialConnections,
    ): AppState =
        AppState(
            container = container,
            scope = CoroutineScope(Dispatchers.Unconfined),
            initialSettings = container.initialSettings,
            initialConnections = connections,
            settingsSaveDebounceMillis = 0,
        )

    @Test
    fun `opening a connection connects and opens the keys tab`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        val state = state(container)

        state.openConnection("c1")

        assertEquals(AppPage.WORKSPACE, state.activePage)
        assertEquals("c1", state.activeConnectionId)
        assertEquals("c1:keys", state.activeTabId)
        assertEquals(1, state.tabs.size)
        assertEquals("Local", state.tabs.single().title)

        val session = assertNotNull(state.sessionFor("c1"))
        assertEquals(SessionStatus.CONNECTED, session.status)
        assertEquals(16, session.databases.size)
        assertEquals(12_480, session.databases[0].keyCount)
        assertEquals(3_900, session.databases[2].keyCount)
        assertEquals("7.4.2", session.summary?.version)
        assertEquals(24, session.summary?.connectedClients)
        assertNotNull(session.latencyMillis)
        state.close()
    }

    @Test
    fun `opening the same tab twice reuses it`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        val state = state(container)

        state.openTab("c1", WorkspaceTabKind.KEYS)
        state.openTab("c1", WorkspaceTabKind.KEYS)
        assertEquals(1, state.tabs.size)

        state.openTab("c1", WorkspaceTabKind.CONSOLE)
        assertEquals(2, state.tabs.size)
        assertEquals("c1:console", state.activeTabId)
        state.openConnection("c1")
        assertEquals(2, state.tabs.size)
        assertEquals("c1:keys", state.activeTabId)
        assertEquals(1, container.gateway.pingCount)
        state.close()
    }

    @Test
    fun `closing a tab selects the right then left neighbour`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        val state = state(container)
        state.openTab("c1", WorkspaceTabKind.KEYS)
        state.openTab("c1", WorkspaceTabKind.CONSOLE)
        state.openTab("c1", WorkspaceTabKind.INSIGHTS)

        state.selectTab("c1:console")
        state.closeTab("c1:console")
        assertEquals("c1:insights", state.activeTabId)

        state.closeTab("c1:insights")
        assertEquals("c1:keys", state.activeTabId)

        state.closeTab("c1:keys")
        assertNull(state.activeTabId)
        state.close()
    }

    @Test
    fun `selecting a database updates session and gateway`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        val state = state(container)
        state.openConnection("c1")

        state.selectDatabase("c1", 2)

        val session = assertNotNull(state.sessionFor("c1"))
        assertEquals(2, session.currentDatabase)
        assertEquals(2, container.gateway.selectedDatabase)
        state.close()
    }

    @Test
    fun `settings updates are persisted immediately`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        val state = state(container)
        val updated = AppSettings(theme = ThemeMode.LIGHT, accent = Accent.MINT)

        state.updateSettings(updated)

        assertEquals(updated, state.settings)
        assertEquals(updated, container.settingsRepository.saved)
        state.close()
    }

    @Test
    fun `clearing local cache drops recent commands and console history`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        container.gateway.executeReply = com.czh.rediscompose.domain.command.RedisReply.Simple("PONG")
        val state = state(container)
        state.updateSettings(AppSettings(recentCommands = listOf("PING")))
        state.openTab("c1", WorkspaceTabKind.CONSOLE)
        val console = assertNotNull(state.consoleFor("c1"))
        console.execute("PING")
        assertTrue(console.history.isNotEmpty())

        state.clearLocalCache()

        assertEquals(emptyList(), state.settings.recentCommands)
        assertTrue(console.history.isEmpty())
        assertTrue(state.toasts.any { it.message.contains("本地缓存") })
        state.close()
    }

    @Test
    fun `toasts keep only the last three`() {
        val state = state(FakeAppContainer())
        repeat(4) { index -> state.showToast("message-$index") }

        assertEquals(3, state.toasts.size)
        assertEquals(listOf("message-1", "message-2", "message-3"), state.toasts.map { it.message })
        state.close()
    }

    @Test
    fun `connection errors are exposed without crashing`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        container.gateway.pingError = RedisException("连接失败：Connection refused")
        val state = state(container)

        state.openConnection("c1")

        val session = assertNotNull(state.sessionFor("c1"))
        assertEquals(SessionStatus.ERROR, session.status)
        assertTrue(session.errorMessage.orEmpty().contains("Connection refused"))
        assertEquals(1, state.tabs.size)
        state.close()
    }

    @Test
    fun `a failed connection can be retried`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        container.gateway.pingError = RedisException("连接失败：Connection refused")
        val state = state(container)
        state.openConnection("c1")
        assertEquals(SessionStatus.ERROR, state.sessionFor("c1")?.status)

        container.gateway.pingError = null
        state.openConnection("c1")

        assertEquals(SessionStatus.CONNECTED, state.sessionFor("c1")?.status)
        assertEquals(2, container.gateway.pingCount)
        assertEquals(1, state.tabs.size)
        state.close()
    }

    @Test
    fun `deleting a connection closes its session and tabs`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        val state = state(container)
        state.openConnection("c1")

        state.deleteConnection("c1")

        assertTrue(state.connections.isEmpty())
        assertTrue(state.tabs.isEmpty())
        assertNull(state.activeTabId)
        assertNull(state.activeConnectionId)
        assertNull(state.sessionFor("c1"))
        assertEquals(false, container.gateway.connected)
        assertEquals(emptyList(), container.connectionRepository.saved)
        state.close()
    }

    @Test
    fun `keys tab owns a workspace that closes with the tab`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        val state = state(container)
        state.openConnection("c1")

        assertNotNull(state.workspaceFor("c1"))

        state.closeTab("c1:keys")
        assertNull(state.workspaceFor("c1"))
        state.close()
    }

    @Test
    fun `console tab records recent commands and closes with the tab`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        val state = state(container)
        state.openTab("c1", WorkspaceTabKind.CONSOLE)
        val console = assertNotNull(state.consoleFor("c1"))
        container.gateway.executeReply = com.czh.rediscompose.domain.command.RedisReply.Simple("PONG")

        console.execute("PING")

        assertEquals(listOf("PING"), state.settings.recentCommands)
        assertEquals(listOf("PING"), container.settingsRepository.saved?.recentCommands)

        state.closeTab("c1:console")
        assertNull(state.consoleFor("c1"))
        state.close()
    }

    @Test
    fun `insights tab owns a state that closes with the tab`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        val state = state(container)
        state.openTab("c1", WorkspaceTabKind.INSIGHTS)

        assertNotNull(state.insightsFor("c1"))

        state.closeTab("c1:insights")
        assertNull(state.insightsFor("c1"))
        state.close()
    }

    @Test
    fun `saving a connection persists and closes its runtime tabs`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        val state = state(container)
        state.openConnection("c1")
        val updated = state.connections.single().copy(name = "Renamed", host = "redis.local")

        state.saveConnection(updated)

        assertEquals("Renamed", state.connections.single().name)
        assertEquals(updated, container.connectionRepository.saved?.single())
        assertNull(state.sessionFor("c1"))
        assertTrue(state.tabs.isEmpty())
        state.close()
    }

    @Test
    fun `duplicate connection gets a new id and copy name`() {
        val source = profile()
        val container = FakeAppContainer(initialConnections = listOf(source))
        val state = state(container)

        val duplicate = assertNotNull(state.duplicateConnection("c1"))

        assertTrue(duplicate.id != source.id)
        assertEquals("Local 副本", duplicate.name)
        assertNull(duplicate.lastOpenedAtEpochMillis)
        assertTrue(duplicate.createdAtEpochMillis > 0)
        assertNull(state.duplicateConnection("missing"))
        state.close()
    }

    @Test
    fun `testing a connection reports latency and version then closes the gateway`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        val state = state(container)
        container.gateway.connected = true

        val result =
            runBlocking {
                state.testConnection(container.initialConnections.single())
            }

        assertTrue(result.success)
        assertNotNull(result.latencyMillis)
        assertEquals("7.4.2", result.version)
        assertFalse(container.gateway.connected)
        state.close()
    }

    @Test
    fun `testing a connection surfaces the first error line`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        container.gateway.pingError = RedisException("Connection refused\nstack")
        val state = state(container)

        val result =
            runBlocking {
                state.testConnection(container.initialConnections.single())
            }

        assertFalse(result.success)
        assertTrue(result.errorMessage.orEmpty().contains("Connection refused"))
        state.close()
    }

    @Test
    fun `opening a connection updates the recent timestamp and persists it`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        val state = state(container)

        state.openConnection("c1")

        val opened = assertNotNull(state.connections.single().lastOpenedAtEpochMillis)
        assertTrue(opened > 0)
        assertEquals(state.connections, container.connectionRepository.saved)
        state.close()
    }

    @Test
    fun `switching database refreshes the key browser`() {
        val container = FakeAppContainer(initialConnections = listOf(profile()))
        val state = state(container)
        state.openConnection("c1")
        val scansBefore = container.gateway.scanCalls.size

        state.selectDatabase("c1", 2)

        assertEquals(scansBefore + 1, container.gateway.scanCalls.size)
        state.close()
    }

    @Test
    fun `opening an unknown tab is a no-op`() {
        val state = state(FakeAppContainer())
        state.openTab("missing", WorkspaceTabKind.CONSOLE)
        assertTrue(state.tabs.isEmpty())
        assertNull(state.activeTabId)
        state.close()
    }

    @Test
    fun `command palette toggles`() {
        val state = state(FakeAppContainer())
        state.toggleCommandPalette()
        assertTrue(state.commandPaletteVisible)
        state.setPaletteVisible(false)
        assertEquals(false, state.commandPaletteVisible)
        state.close()
    }
}
