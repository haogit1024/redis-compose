package com.czh.rediscompose.feature.workspace

import com.czh.rediscompose.app.FakeRedisGateway
import com.czh.rediscompose.app.RedisSession
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.key.KeyType
import com.czh.rediscompose.domain.key.KeyValue
import com.czh.rediscompose.domain.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkspaceEditingTest {
    private val toasts = mutableListOf<Pair<String, ToastKind>>()
    private var settings = AppSettings()

    private fun gateway(): FakeRedisGateway = FakeRedisGateway()

    private fun workspace(gateway: FakeRedisGateway): WorkspaceState {
        toasts.clear()
        settings = AppSettings()
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
            settings = { settings },
            onToast = { message, kind -> toasts += message to kind },
            scope = scope,
        )
    }

    private fun seedString(
        gateway: FakeRedisGateway,
        key: String,
        text: String,
        ttl: Long = -1,
    ) {
        gateway.stringValues[key] = text.encodeToByteArray()
        gateway.storeKeys.add(key)
        gateway.keyTtlByKey = gateway.keyTtlByKey + (key to ttl)
        gateway.databaseSizeValue = gateway.storeKeys.size.toLong()
    }

    @Test
    fun `selecting a string key loads detail metadata and json detection`() {
        val gateway = gateway()
        val text = """{"name":"ada","active":true}"""
        seedString(gateway, "user:1", text, ttl = 120)
        gateway.memoryByKey = mapOf("user:1" to 1_433L)
        gateway.encodingByKey = mapOf("user:1" to "embstr")
        val state = workspace(gateway)
        state.start()

        state.selectKey("user:1")

        val detail = assertNotNull(state.keyDetail)
        assertFalse(state.detailLoading)
        assertEquals(KeyType.STRING, detail.summary.type)
        assertEquals(120L, detail.metadata.ttlSeconds)
        assertEquals(1_433L, detail.metadata.memoryBytes)
        assertEquals("embstr", detail.metadata.encoding)
        assertEquals(text.encodeToByteArray().size.toLong(), detail.metadata.elementCount)
        val value = detail.value as KeyValue.StringValue
        assertEquals(text, value.text)
        assertTrue(value.isJson)
        assertFalse(value.isBinary)
    }

    @Test
    fun `non utf8 string is marked binary and cannot be saved`() {
        val gateway = gateway()
        gateway.stringValues["raw"] = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        gateway.storeKeys.add("raw")
        gateway.databaseSizeValue = 1
        val state = workspace(gateway)
        state.start()
        state.selectKey("raw")

        val value = (assertNotNull(state.keyDetail).value as KeyValue.StringValue)
        assertTrue(value.isBinary)

        state.saveString("replacement")

        assertTrue(gateway.stringValues.getValue("raw").contentEquals(byteArrayOf(0xFF.toByte(), 0xFE.toByte())))
        assertTrue(toasts.any { it.second == ToastKind.Warning && it.first.contains("二进制") })
    }

    @Test
    fun `saving a string updates redis and detail`() {
        val gateway = gateway()
        seedString(gateway, "app:name", "old")
        val state = workspace(gateway)
        state.start()
        state.selectKey("app:name")

        state.saveString("new")

        assertEquals("new", gateway.stringValues.getValue("app:name").decodeToString())
        assertEquals("new", (assertNotNull(state.keyDetail).value as KeyValue.StringValue).text)
        assertTrue(toasts.any { it.first == "已保存" })
        assertFalse(state.hasUnsavedEdits)
    }

    @Test
    fun `read only mode blocks writes at state level`() {
        val gateway = gateway()
        seedString(gateway, "app:name", "old")
        val state = workspace(gateway)
        settings = AppSettings(globalReadOnly = true)
        state.start()
        state.selectKey("app:name")

        state.saveString("new")
        state.updateTtl(10)

        assertEquals("old", gateway.stringValues.getValue("app:name").decodeToString())
        assertTrue(toasts.any { it.second == ToastKind.Warning && it.first.contains("只读") })
        assertFalse(state.canWrite)
    }

    @Test
    fun `hash fields can be saved renamed and deleted`() {
        val gateway = gateway()
        gateway.hashValues["h"] = linkedMapOf("a" to "1".encodeToByteArray(), "b" to "2".encodeToByteArray())
        gateway.storeKeys.add("h")
        gateway.databaseSizeValue = 1
        val state = workspace(gateway)
        state.start()
        state.selectKey("h")

        val renamed = EditableHashField(originalField = "a", field = "a2", value = "10", originalValue = "1")
        state.saveHashField(renamed)

        assertEquals("10", gateway.hashValues.getValue("h").getValue("a2").decodeToString())
        assertFalse(gateway.hashValues.getValue("h").containsKey("a"))
        assertEquals(2, (assertNotNull(state.keyDetail).value as KeyValue.HashValue).fields.size)

        val existing = EditableHashField(originalField = "b", field = "b", value = "2")
        state.deleteHashField(existing)

        assertFalse(gateway.hashValues.getValue("h").containsKey("b"))
        assertEquals(1, (assertNotNull(state.keyDetail).value as KeyValue.HashValue).fields.size)
    }

    @Test
    fun `list members can be edited added at head and deleted`() {
        val gateway = gateway()
        gateway.listValues["l"] = mutableListOf("a".encodeToByteArray(), "b".encodeToByteArray())
        gateway.storeKeys.add("l")
        gateway.databaseSizeValue = 1
        val state = workspace(gateway)
        state.start()
        state.selectKey("l")

        state.saveListMember(0, "A")
        assertEquals(listOf("A", "b"), gateway.listValues.getValue("l").map { it.decodeToString() })

        state.addListMember("head", head = true)
        assertEquals(listOf("head", "A", "b"), gateway.listValues.getValue("l").map { it.decodeToString() })

        state.deleteListMember(0, "head")
        assertEquals(listOf("A", "b"), gateway.listValues.getValue("l").map { it.decodeToString() })
        assertEquals(2, (assertNotNull(state.keyDetail).value as KeyValue.ListValue).items.size)
    }

    @Test
    fun `set members can be renamed and deleted`() {
        val gateway = gateway()
        gateway.setValues["s"] = linkedSetOf("a", "b")
        gateway.storeKeys.add("s")
        gateway.databaseSizeValue = 1
        val state = workspace(gateway)
        state.start()
        state.selectKey("s")

        state.saveSetMember(EditableSetMember(original = "a", value = "c"))

        assertEquals(setOf("c", "b"), gateway.setValues.getValue("s"))

        state.deleteSetMember(EditableSetMember(original = "c", value = "c"))
        assertEquals(setOf("b"), gateway.setValues.getValue("s"))
        assertEquals(listOf("b"), (assertNotNull(state.keyDetail).value as KeyValue.SetValue).members)
    }

    @Test
    fun `sorted set rejects invalid scores and saves valid changes`() {
        val gateway = gateway()
        gateway.zsetValues["z"] = linkedMapOf("a" to 1.0)
        gateway.storeKeys.add("z")
        gateway.databaseSizeValue = 1
        val state = workspace(gateway)
        state.start()
        state.selectKey("z")

        state.saveSortedSetEntry(
            EditableSortedSetEntry(
                originalMember = "a",
                member = "a",
                score = "abc",
                originalScore = "1.0",
            ),
        )
        assertEquals(1.0, gateway.zsetValues.getValue("z").getValue("a"))
        assertTrue(toasts.any { it.first.contains("Score") })

        state.saveSortedSetEntry(
            EditableSortedSetEntry(
                originalMember = "a",
                member = "b",
                score = "2.5",
                originalScore = "1.0",
            ),
        )
        assertEquals(2.5, gateway.zsetValues.getValue("z").getValue("b"))
        assertFalse(gateway.zsetValues.getValue("z").containsKey("a"))
    }

    @Test
    fun `ttl dialog actions persist and expire`() {
        val gateway = gateway()
        seedString(gateway, "temp", "x", ttl = 60)
        val state = workspace(gateway)
        state.start()
        state.selectKey("temp")
        state.openTtlDialog()
        assertEquals("temp", state.ttlDialogKey)

        state.updateTtl(-1)
        assertNull(state.ttlDialogKey)
        assertEquals(-1L, state.keyDetail?.summary?.ttlSeconds)

        state.updateTtl(120)
        assertEquals(120L, gateway.ttlValues.getValue("temp"))
    }

    @Test
    fun `rename moves list selection pinned state and redis value`() {
        val gateway = gateway()
        seedString(gateway, "old", "value")
        gateway.stringValues["existing"] = "x".encodeToByteArray()
        gateway.storeKeys.add("existing")
        gateway.databaseSizeValue = 2
        val state = workspace(gateway)
        state.start()
        state.selectKey("old")
        state.togglePinned("old")

        state.renameKey("new")

        assertTrue(gateway.stringValues.containsKey("new"))
        assertFalse(gateway.stringValues.containsKey("old"))
        assertFalse(state.keys.contains("old"))
        assertTrue(state.keys.contains("new"))
        assertTrue(state.pinnedKeys.contains("new"))
        assertFalse(state.pinnedKeys.contains("old"))
        assertEquals("new", state.selectedKey)
        assertEquals("new", state.keyDetail?.summary?.name)
    }

    @Test
    fun `rename to an existing key is rejected`() {
        val gateway = gateway()
        seedString(gateway, "old", "value")
        gateway.stringValues["taken"] = "x".encodeToByteArray()
        gateway.storeKeys.add("taken")
        gateway.databaseSizeValue = 2
        val state = workspace(gateway)
        state.start()
        state.selectKey("old")

        state.renameKey("taken")

        assertTrue(gateway.stringValues.containsKey("old"))
        assertEquals("value", gateway.stringValues.getValue("old").decodeToString())
        assertTrue(toasts.any { it.first.contains("目标 Key 已存在") })
    }

    @Test
    fun `deleting removes the key tree pin selection and total`() {
        val gateway = gateway()
        seedString(gateway, "a:1", "1")
        seedString(gateway, "a:2", "2")
        val state = workspace(gateway)
        state.start()
        assertEquals(2L, state.total)
        state.selectKey("a:1")
        state.togglePinned("a:1")

        state.deleteSelectedKey()

        assertFalse(gateway.stringValues.containsKey("a:1"))
        assertEquals(listOf("a:2"), state.keys)
        assertFalse(state.pinnedKeys.contains("a:1"))
        assertNull(state.selectedKey)
        assertNull(state.keyDetail)
        assertEquals(1L, state.total)
    }

    @Test
    fun `create key supports five types and validates collection content`() {
        val gateway = gateway()
        val state = workspace(gateway)
        state.start()

        state.createKey(NewKeyDraft(name = "string", type = KeyType.STRING, stringValue = "hello"))
        assertEquals("hello", gateway.stringValues.getValue("string").decodeToString())

        state.createKey(
            NewKeyDraft(
                name = "hash",
                type = KeyType.HASH,
                hashField = "field",
                hashValue = "value",
            ),
        )
        assertEquals("value", gateway.hashValues.getValue("hash").getValue("field").decodeToString())

        state.createKey(
            NewKeyDraft(
                name = "list",
                type = KeyType.LIST,
                listValue = "item",
                listHead = true,
            ),
        )
        assertEquals("item", gateway.listValues.getValue("list").single().decodeToString())

        state.createKey(NewKeyDraft(name = "set", type = KeyType.SET, setMember = "member"))
        assertEquals(setOf("member"), gateway.setValues.getValue("set"))

        state.createKey(
            NewKeyDraft(
                name = "zset",
                type = KeyType.ZSET,
                zsetMember = "member",
                zsetScore = "1.5",
            ),
        )
        assertEquals(1.5, gateway.zsetValues.getValue("zset").getValue("member"))

        state.createKey(NewKeyDraft(name = "bad-hash", type = KeyType.HASH))
        assertFalse(gateway.storeKeys.contains("bad-hash"))
        assertTrue(toasts.any { it.first.contains("Hash field") })
    }

    @Test
    fun `set rejects duplicate members before writing`() {
        val gateway = gateway()
        gateway.setValues["s"] = linkedSetOf("a")
        gateway.storeKeys.add("s")
        gateway.databaseSizeValue = 1
        val state = workspace(gateway)
        state.start()
        state.selectKey("s")

        state.saveSetMember(EditableSetMember(original = null, value = "a"))

        assertEquals(setOf("a"), gateway.setValues.getValue("s"))
        assertTrue(toasts.any { it.first.contains("成员已存在") })
    }

    @Test
    fun `sorted set rejects non finite scores`() {
        val gateway = gateway()
        gateway.zsetValues["z"] = linkedMapOf("a" to 1.0)
        gateway.storeKeys.add("z")
        gateway.databaseSizeValue = 1
        val state = workspace(gateway)
        state.start()
        state.selectKey("z")

        state.saveSortedSetEntry(
            EditableSortedSetEntry(
                originalMember = "a",
                member = "a",
                score = "NaN",
                originalScore = "1",
            ),
        )

        assertEquals(1.0, gateway.zsetValues.getValue("z").getValue("a"))
        assertTrue(toasts.any { it.first.contains("有限数字") })
    }

    @Test
    fun `save shortcut requests are consumed once`() {
        val state = workspace(gateway())

        assertFalse(state.consumeSaveRequest())
        state.requestSave()
        assertTrue(state.consumeSaveRequest())
        assertFalse(state.consumeSaveRequest())
    }
}
