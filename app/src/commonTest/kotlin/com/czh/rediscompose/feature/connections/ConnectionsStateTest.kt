package com.czh.rediscompose.feature.connections

import com.czh.rediscompose.app.SessionStatus
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.domain.connection.ConnectionEnvironment
import com.czh.rediscompose.domain.connection.ConnectionField
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.ConnectionTestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectionsStateTest {
    private val saved = mutableListOf<ConnectionProfile>()
    private val deleted = mutableListOf<String>()
    private val opened = mutableListOf<String>()
    private val toasts = mutableListOf<Pair<String, ToastKind>>()
    private var testResult = ConnectionTestResult(success = true, latencyMillis = 12, version = "7.4.2")

    private fun profile(
        id: String,
        name: String,
        environment: ConnectionEnvironment = ConnectionEnvironment.LOCAL,
        host: String = "127.0.0.1",
        group: String = "",
        lastOpened: Long? = null,
    ) = ConnectionProfile(
        id = id,
        name = name,
        environment = environment,
        host = host,
        group = group,
        lastOpenedAtEpochMillis = lastOpened,
        createdAtEpochMillis = 1_000L,
    )

    private fun state(profiles: List<ConnectionProfile>): ConnectionsState =
        ConnectionsState(
            connections = { profiles },
            runtimeFor = { id ->
                when (id) {
                    "a" -> ConnectionRuntime(status = SessionStatus.CONNECTED, latencyMillis = 12, version = "7.4.2")
                    "bad" -> ConnectionRuntime(status = SessionStatus.ERROR, errorMessage = "refused")
                    else -> ConnectionRuntime()
                }
            },
            onOpen = { opened += it },
            onSave = { saved += it },
            onDelete = { deleted += it },
            onDuplicate = { id ->
                profiles.firstOrNull { it.id == id }?.let { source ->
                    source.copy(
                        id = "$id-copy",
                        name = "${source.name} 副本",
                        lastOpenedAtEpochMillis = null,
                    )
                }
            },
            onTest = { testResult },
            onToast = { message, kind -> toasts += message to kind },
            scope = CoroutineScope(Dispatchers.Unconfined),
        ).also { state ->
            state.openNew()
            state.updateDraft {
                it.copy(
                    name = "Local",
                    host = "127.0.0.1",
                    port = "6379",
                    connectTimeout = "5000",
                    readTimeout = "5000",
                )
            }
        }

    @Test
    fun `search environment and sort filter the visible connections`() {
        val connections =
            listOf(
                profile("a", "Prod Cache", ConnectionEnvironment.PRODUCTION, "prod.acme.io", "Prod", lastOpened = 30),
                profile("b", "Local Sandbox", ConnectionEnvironment.LOCAL, "127.0.0.1", "Local", lastOpened = 10),
                profile("c", "Billing", ConnectionEnvironment.STAGING, "billing.acme.io", "Staging", lastOpened = 20),
            )
        val state = state(connections)

        // Default is name order, so opening a connection never moves its card.
        assertEquals(ConnectionSort.NAME, state.sort)
        assertEquals(listOf("c", "b", "a"), state.filteredConnections.map { it.id })

        state.updateSearch("acme")
        assertEquals(listOf("c", "a"), state.filteredConnections.map { it.id })

        state.updateSearch("")
        state.selectEnvironment(ConnectionEnvironment.LOCAL)
        assertEquals(listOf("b"), state.filteredConnections.map { it.id })

        state.clearFilters()
        state.chooseSort(ConnectionSort.RECENT)
        assertEquals(listOf("a", "c", "b"), state.filteredConnections.map { it.id })

        state.chooseSort(ConnectionSort.NAME)
        assertEquals(listOf("c", "b", "a"), state.filteredConnections.map { it.id })
        assertFalse(state.hasActiveFilters)
        state.dispose()
    }

    @Test
    fun `recent connections are limited to six and exclude never opened`() {
        val connections =
            (1..8).map { index ->
                profile(
                    id = "c$index",
                    name = "C$index",
                    lastOpened = if (index == 8) null else index.toLong(),
                )
            }
        val state = state(connections)

        assertEquals(6, state.recentConnections.size)
        assertEquals("c7", state.recentConnections.first().id)
        assertTrue(state.recentConnections.none { it.id == "c8" })
        state.dispose()
    }

    @Test
    fun `opening and editing drafts keep the right mode`() {
        val connections = listOf(profile("a", "Prod", lastOpened = 10))
        val state = state(connections)

        state.openEdit("a")
        assertEquals(ConnectionFormMode.EDIT, state.draft?.mode)
        assertEquals("a", state.draft?.id)

        state.openDuplicate("a")
        assertEquals(ConnectionFormMode.DUPLICATE, state.draft?.mode)
        assertEquals("a-copy", state.draft?.id)
        assertTrue(state.draft?.name.orEmpty().contains("副本"))

        state.closeSheet()
        assertNull(state.draft)
        state.dispose()
    }

    @Test
    fun `save blocks invalid drafts and reports field errors`() {
        val state = state(emptyList())
        state.updateDraft { it.copy(name = "", host = "") }

        state.save()

        assertTrue(saved.isEmpty())
        assertTrue(state.validationErrors.containsKey(ConnectionField.NAME))
        assertTrue(state.validationErrors.containsKey(ConnectionField.HOST))
        assertTrue(state.isSheetVisible)
        state.dispose()
    }

    @Test
    fun `saving a new draft creates a connection and closes the sheet`() {
        val state = state(emptyList())
        state.updateDraft { it.copy(name = "New Connection", host = "redis.local") }

        state.save()

        assertEquals(1, saved.size)
        assertTrue(saved.single().id.startsWith("conn-"))
        assertEquals("New Connection", saved.single().name)
        assertTrue(toasts.any { it.first == "连接已创建" && it.second == ToastKind.Success })
        assertFalse(state.isSheetVisible)
        state.dispose()
    }

    @Test
    fun `saving an edit preserves id and creation time`() {
        val connections = listOf(profile("a", "Prod", lastOpened = 10))
        val state = state(connections)
        state.openEdit("a")
        state.updateDraft { it.copy(name = "Prod 2", port = "6380") }

        state.save()

        assertEquals("a", saved.single().id)
        assertEquals(1_000L, saved.single().createdAtEpochMillis)
        assertEquals(10L, saved.single().lastOpenedAtEpochMillis)
        assertEquals("Prod 2", saved.single().name)
        assertEquals(6380, saved.single().port)
        state.dispose()
    }

    @Test
    fun `saving a duplicate keeps the copied id and name`() {
        val connections = listOf(profile("a", "Prod", lastOpened = 10))
        val state = state(connections)
        state.openDuplicate("a")

        state.save()

        assertEquals("a-copy", saved.single().id)
        assertTrue(saved.single().name.contains("副本"))
        assertNull(saved.single().lastOpenedAtEpochMillis)
        state.dispose()
    }

    @Test
    fun `test connection exposes success and failure results`() {
        val state = state(emptyList())
        state.testDraft()

        assertFalse(state.isTesting)
        assertTrue(state.testResult?.success == true)
        assertEquals("7.4.2", state.testResult?.version)

        testResult = ConnectionTestResult(success = false, errorMessage = "connection refused")
        state.testDraft()

        assertEquals("connection refused", state.testResult?.errorMessage)
        assertTrue(state.isSheetVisible)
        state.dispose()
    }

    @Test
    fun `testing an existing connection reports a toast`() {
        val state = state(listOf(profile("a", "Prod")))
        state.testExisting("a")

        assertTrue(toasts.any { it.first.contains("连接成功") && it.second == ToastKind.Success })
        assertNull(state.testingConnectionId)

        testResult = ConnectionTestResult(success = false, errorMessage = "refused")
        state.testExisting("a")
        assertTrue(toasts.any { it.first.contains("连接失败") && it.second == ToastKind.Danger })
        state.dispose()
    }

    @Test
    fun `delete asks for confirmation then removes the profile`() {
        val state = state(listOf(profile("a", "Prod")))
        state.requestDelete("a")

        assertNotNull(state.deleteTarget)
        state.confirmDelete()

        assertEquals(listOf("a"), deleted)
        assertNull(state.deleteTarget)
        state.dispose()
    }

    @Test
    fun `opening a card delegates to the app`() {
        val state = state(listOf(profile("a", "Prod")))
        state.open("a")
        assertEquals(listOf("a"), opened)
        state.dispose()
    }
}
