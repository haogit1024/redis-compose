package com.czh.rediscompose.feature.workspace

import com.czh.rediscompose.app.FakeRedisGateway
import com.czh.rediscompose.app.RedisSession
import com.czh.rediscompose.data.redis.RedisException
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.key.KeyType
import com.czh.rediscompose.domain.key.ScanPage
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.KeyView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkspaceStateTest {
    private fun gateway(vararg keys: String): FakeRedisGateway =
        FakeRedisGateway().apply {
            scanKeys = keys.toList()
            databaseSizeValue = keys.size.toLong()
        }

    private fun workspace(
        gateway: FakeRedisGateway,
        initialSettings: AppSettings = AppSettings(),
        onSettingsChange: (AppSettings) -> Unit = {},
    ): WorkspaceState {
        currentSettings = initialSettings
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val session =
            RedisSession(
                profile = ConnectionProfile(id = "c1", name = "Local", host = "127.0.0.1"),
                gateway = gateway,
                appScope = scope,
            )
        scope.launch { session.connect() }
        return WorkspaceState(
            session = session,
            settings = { currentSettings },
            onToast = { _, _ -> },
            scope = scope,
            onSettingsChange = {
                currentSettings = it
                onSettingsChange(it)
            },
        )
    }

    private var currentSettings: AppSettings = AppSettings()

    @Test
    fun `refresh loads the first page and reports total`() {
        val gateway = gateway("app:a", "app:b", "cache:x").apply { scanPageSize = 2 }
        val state = workspace(gateway)

        state.start()

        assertEquals(listOf("app:a", "app:b"), state.keys)
        assertEquals(3L, state.total)
        assertFalse(state.loadedAll)
        assertFalse(state.isLoading)
        assertEquals(1, gateway.scanCalls.size)
        assertEquals(null, gateway.scanCalls.single().pattern)

        state.loadMore()

        assertEquals(listOf("app:a", "app:b", "cache:x"), state.keys)
        assertTrue(state.loadedAll)
    }

    @Test
    fun `scan pages are de-duplicated because SCAN may repeat keys`() {
        val gateway = gateway("a", "b", "a", "c").apply { scanPageSize = 2 }
        val state = workspace(gateway)

        state.start()
        state.loadMore()
        state.loadMore()

        assertEquals(listOf("a", "b", "c"), state.keys)
    }

    @Test
    fun `search passes the pattern to SCAN and clearing restores the full list`() {
        val gateway = gateway("user:1", "user:2", "session:1").apply { scanPageSize = 100 }
        val state = workspace(gateway)
        state.start()

        state.searchInput = "user:*"
        state.applySearch("user:*")

        assertEquals("user:*", state.appliedPattern)
        assertEquals(listOf("user:1", "user:2"), state.keys)
        assertEquals("user:*", gateway.scanCalls.last().pattern)

        state.clearSearch()

        assertNull(state.appliedPattern)
        assertEquals("", state.searchInput)
        assertEquals(3, state.keys.size)
    }

    @Test
    fun `type filter keeps unknown types visible until they are resolved`() {
        val gateway = gateway("a", "b", "c").apply { scanPageSize = 100 }
        val state = workspace(gateway)
        state.start()
        state.keyTypes["a"] = KeyType.STRING
        state.keyTypes["b"] = KeyType.HASH

        state.updateTypeFilter(KeyType.STRING)

        assertEquals(listOf("a", "c"), state.filteredKeys)

        state.keyTypes["c"] = KeyType.HASH
        assertEquals(listOf("a"), state.filteredKeys)

        state.updateTypeFilter(null)
        assertEquals(listOf("a", "b", "c"), state.filteredKeys)
    }

    @Test
    fun `pinning toggles and keeps a stable order`() {
        val gateway = gateway("b", "a").apply { scanPageSize = 100 }
        val state = workspace(gateway)
        state.start()

        state.togglePinned("b")
        state.togglePinned("a")

        assertTrue(state.isPinned("a"))
        assertEquals(listOf("a", "b"), state.pinnedKeyList)

        state.togglePinned("b")

        assertFalse(state.isPinned("b"))
        assertEquals(listOf("a"), state.pinnedKeyList)
    }

    @Test
    fun `load failures keep loaded keys and can be retried`() {
        val gateway = gateway("a", "b", "c", "d").apply { scanPageSize = 2 }
        val state = workspace(gateway)
        state.start()
        assertEquals(listOf("a", "b"), state.keys)

        gateway.scanError = RedisException("连接已断开")
        state.loadMore()

        assertEquals(listOf("a", "b"), state.keys)
        assertNotNull(state.loadError)

        gateway.scanError = null
        state.retry()

        assertEquals(listOf("a", "b", "c", "d"), state.keys)
        assertNull(state.loadError)
        assertTrue(state.loadedAll)
    }

    @Test
    fun `load more retries after a failed page instead of doing nothing`() {
        val gateway = gateway("a", "b", "c", "d").apply { scanPageSize = 2 }
        val state = workspace(gateway)
        state.start()

        gateway.scanError = RedisException("连接已断开")
        state.loadMore()
        assertNotNull(state.loadError)

        gateway.scanError = null
        state.loadMore(notify = true)

        assertEquals(listOf("a", "b", "c", "d"), state.keys)
        assertNull(state.loadError)
        assertTrue(state.loadedAll)
    }

    @Test
    fun `load more keeps scanning past pages with no new keys`() {
        val gateway =
            gateway("unused").apply {
                scanPagesOverride =
                    listOf(
                        ScanPage(cursor = 1L, keys = listOf("a", "b")),
                        ScanPage(cursor = 2L, keys = listOf("a", "b")),
                        ScanPage(cursor = 0L, keys = listOf("c", "d")),
                    )
            }
        val state = workspace(gateway)
        state.start()
        assertEquals(listOf("a", "b"), state.keys)

        state.loadMore(notify = true)

        assertEquals(listOf("a", "b", "c", "d"), state.keys)
        assertTrue(state.loadedAll)
        assertEquals(3, gateway.scanCalls.size)
    }

    @Test
    fun `top level directories stay collapsed until the user expands one`() {
        val gateway =
            gateway("app:config:a", "app:session:b", "cache:x").apply { scanPageSize = 100 }
        val state = workspace(gateway)
        state.start()

        assertTrue(state.expandedDirs.isEmpty())

        state.toggleDirectory("app")
        assertEquals(setOf("app"), state.expandedDirs)

        state.refresh()

        assertEquals(setOf("app"), state.expandedDirs)
    }

    @Test
    fun `key view toggle persists through the settings callback`() {
        val gateway = gateway("a")
        var updated: AppSettings? = null
        val state = workspace(gateway, onSettingsChange = { updated = it })
        state.start()

        state.toggleKeyView()

        assertEquals(KeyView.FLAT, currentSettings.keyView)
        assertEquals(KeyView.FLAT, updated?.keyView)
        assertEquals(KeyView.FLAT, state.keyView)

        state.toggleKeyView()
        assertEquals(KeyView.TREE, currentSettings.keyView)
    }

    @Test
    fun `previews are requested once per key and cache type and ttl`() {
        val gateway = gateway("key").apply { scanPageSize = 100 }
        gateway.keyTypeByKey = mapOf("key" to KeyType.LIST)
        gateway.keyTtlByKey = mapOf("key" to 42L)
        val state = workspace(gateway)
        state.start()

        state.ensurePreview("key")
        state.ensurePreview("key")

        assertEquals(listOf("key"), gateway.typeRequests)
        assertEquals(listOf("key"), gateway.ttlRequests)
        assertEquals(KeyType.LIST, state.keyTypes["key"])
        assertEquals(42L, state.keyTtls["key"])
    }

    @Test
    fun `refresh clears preview caches`() {
        val gateway = gateway("key").apply { scanPageSize = 100 }
        val state = workspace(gateway)
        state.start()
        state.keyTypes["key"] = KeyType.SET
        state.keyTtls["key"] = 60L

        state.refresh()

        assertTrue(state.keyTypes.isEmpty())
        assertTrue(state.keyTtls.isEmpty())
    }

    @Test
    fun `search replaces previous keys instead of mixing results`() {
        val gateway = gateway("user:1", "order:1").apply { scanPageSize = 100 }
        val state = workspace(gateway)
        state.start()

        state.searchInput = "order:*"
        state.applySearch("order:*")

        assertEquals(listOf("order:1"), state.keys)
        assertFalse(state.keys.contains("user:1"))
    }

    @Test
    fun `selecting and clearing a key only changes selection`() {
        val gateway = gateway("a").apply { scanPageSize = 100 }
        val state = workspace(gateway)
        state.start()

        state.selectKey("a")
        assertEquals("a", state.selectedKey)

        state.clearSelection()
        assertNull(state.selectedKey)
        assertEquals(listOf("a"), state.keys)
    }

    @Test
    fun `flattened tree hides children of collapsed directories`() {
        val gateway =
            gateway("app:config:a", "app:session:b", "cache:x").apply { scanPageSize = 100 }
        val state = workspace(gateway)
        state.start()
        state.toggleDirectory("app")

        val expandedRows = flattenKeyTree(state.tree, state.expandedDirs)
        assertTrue(expandedRows.any { it is KeyTreeRow.Directory && it.node.path == "app" })
        assertTrue(expandedRows.any { it is KeyTreeRow.Directory && it.node.path == "app:config" })
        assertFalse(expandedRows.any { it is KeyTreeRow.Leaf && it.node.path == "app:config:a" })

        state.toggleDirectory("app:config")
        assertTrue(
            flattenKeyTree(state.tree, state.expandedDirs)
                .any { it is KeyTreeRow.Leaf && it.node.path == "app:config:a" },
        )

        state.toggleDirectory("app")
        val collapsedRows = flattenKeyTree(state.tree, state.expandedDirs)

        assertTrue(collapsedRows.any { it is KeyTreeRow.Directory && it.node.path == "app" })
        assertFalse(collapsedRows.any { it is KeyTreeRow.Directory && it.node.path == "app:config" })
        assertFalse(collapsedRows.any { it is KeyTreeRow.Leaf && it.node.path == "app:config:a" })
        assertTrue(collapsedRows.any { it is KeyTreeRow.Directory && it.node.path == "cache" })
    }
}
