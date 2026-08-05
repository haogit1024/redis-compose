package com.czh.redis.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.czh.redis.data.RedisConnection
import com.czh.redis.data.loadConnections
import com.czh.redis.data.loadSettings
import com.czh.redis.data.saveConnections
import com.czh.redis.data.saveSettings
import com.czh.redis.redis.CommandResult
import com.czh.redis.redis.DbInfo
import com.czh.redis.redis.KeyTreeNode
import com.czh.redis.redis.RedisClient
import com.czh.redis.redis.RedisType
import com.czh.redis.redis.buildKeyTree
import com.czh.redis.redis.createRedisClient
import com.czh.redis.redis.parseInfoSections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TabType { KEYS, CONSOLE, SERVER_INFO, SETTINGS }

class WorkspaceTab(
    val id: String,
    val title: String,
    val connectionId: String?,
    val type: TabType,
)

enum class ConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

class Session(
    val connectionId: String,
    val connection: RedisConnection,
) {
    val client: RedisClient = createRedisClient(connection)
    var status by mutableStateOf(ConnectionStatus.DISCONNECTED)
    var statusError by mutableStateOf<String?>(null)
    var currentDb by mutableStateOf(connection.defaultDb)
    var dbs by mutableStateOf<List<DbInfo>>(emptyList())

    val totalKeys: Long
        get() = dbs.firstOrNull { it.index == currentDb }?.keyCount ?: 0

    val isReadonly: Boolean
        get() = connection.readonly
}

enum class ToastType { SUCCESS, ERROR, WARNING, INFO }

data class Toast(val id: Long, val message: String, val type: ToastType, val detail: String? = null)

data class ContextMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val danger: Boolean = false,
)

class ContextMenuState {
    var visible by mutableStateOf(false)
    var x by mutableStateOf(0f)
    var y by mutableStateOf(0f)
    var items by mutableStateOf<List<ContextMenuItem>>(emptyList())

    fun show(x: Float, y: Float, items: List<ContextMenuItem>) {
        this.x = x
        this.y = y
        this.items = items
        visible = true
    }

    fun dismiss() {
        visible = false
    }
}

data class ConfirmRequest(
    val message: String,
    val confirmText: String = "删除",
    val onConfirm: () -> Unit,
)

enum class ConsoleLineKind { PROMPT, INPUT, OUTPUT, RESULT, WARNING, ERROR }

data class ConsoleLine(val text: String, val kind: ConsoleLineKind)

class ConsoleState(
    val vm: AppViewModel,
    val session: Session,
    private val scope: CoroutineScope,
) {
    val lines = mutableStateListOf<ConsoleLine>()
    var input by mutableStateOf("")
    val history = mutableStateListOf<String>()
    var historyIndex by mutableStateOf(-1)

    fun execute() {
        val cmd = input.trim()
        if (cmd.isEmpty()) return
        history.add(cmd)
        historyIndex = -1
        val prefix = "${session.connection.host}:${session.connection.port}>"
        lines.add(ConsoleLine("$prefix $cmd", ConsoleLineKind.PROMPT))
        input = ""
        scope.launch {
            try {
                val args = parseArgs(cmd)
                if (vm.settings.readonly && isWriteCommand(args.first())) {
                    lines.add(ConsoleLine("⚠ 只读模式已开启，写命令被拦截：${args.first()}", ConsoleLineKind.WARNING))
                    return@launch
                }
                val result = session.client.command(args)
                lines.addAll(renderResult(result))
            } catch (e: Exception) {
                lines.add(ConsoleLine("(error) ${e.message}", ConsoleLineKind.ERROR))
            }
        }
    }

    fun clear() {
        lines.clear()
    }

    fun historyUp(): Boolean {
        if (history.isEmpty()) return false
        historyIndex = if (historyIndex >= 0) historyIndex - 1 else history.lastIndex
        input = history[historyIndex]
        return true
    }

    fun historyDown(): Boolean {
        if (historyIndex < 0) return false
        historyIndex++
        if (historyIndex >= history.size) {
            historyIndex = -1
            input = ""
        } else {
            input = history[historyIndex]
        }
        return true
    }

    private fun renderResult(result: CommandResult): List<ConsoleLine> = when (result) {
        is CommandResult.Status -> listOf(ConsoleLine(result.value, ConsoleLineKind.RESULT))
        is CommandResult.Integer -> listOf(ConsoleLine("(integer) ${result.value}", ConsoleLineKind.RESULT))
        is CommandResult.Bulk -> {
            val text = com.czh.redis.redis.ValueCodec.decode(result.value)
                ?: com.czh.redis.redis.ValueCodec.hex(result.value)
            listOf(ConsoleLine("\"$text\"", ConsoleLineKind.RESULT))
        }
        is CommandResult.Array -> result.values.mapIndexed { index, value ->
            ConsoleLine("${index + 1}) $value", ConsoleLineKind.RESULT)
        }
        is CommandResult.Error -> listOf(ConsoleLine("(error) ${result.message}", ConsoleLineKind.ERROR))
        CommandResult.Nil -> listOf(ConsoleLine("(nil)", ConsoleLineKind.RESULT))
        CommandResult.Ok -> listOf(ConsoleLine("OK", ConsoleLineKind.RESULT))
    }

    private fun isWriteCommand(command: String): Boolean {
        val writeCommands = setOf(
            "SET", "SETEX", "PSETEX", "SETNX", "GETSET", "APPEND", "SETRANGE", "MSET", "MSETNX", "INCR",
            "INCRBY", "INCRBYFLOAT", "DECR", "DECRBY", "DEL", "UNLINK", "EXPIRE", "EXPIREAT", "PEXPIRE",
            "PEXPIREAT", "PERSIST", "RENAME", "RENAMENX", "HSET", "HSETNX", "HDEL", "HINCRBY", "HMSET",
            "LPUSH", "LPUSHX", "RPUSH", "RPUSHX", "LPOP", "RPOP", "LSET", "LREM", "LTRIM", "SADD",
            "SREM", "SPOP", "SMOVE", "ZADD", "ZREM", "ZINCRBY", "FLUSHDB", "FLUSHALL", "SELECT",
            "GETDEL", "GETEX", "COPY", "MOVE", "SWAPDB", "RESTORE", "PING", "MULTI", "EXEC",
        )
        return writeCommands.contains(command.uppercase())
    }
}

class ServerInfoState(
    val vm: AppViewModel,
    val session: Session,
    private val scope: CoroutineScope,
) {
    var sections by mutableStateOf<List<Pair<String, List<Pair<String, String>>>>>(emptyList())
    var loading by mutableStateOf(false)

    fun refresh() {
        scope.launch {
            loading = true
            try {
                val info = session.client.info()
                sections = parseInfoSections(info)
            } catch (e: Exception) {
                vm.showToast("刷新服务器信息失败：${e.message}", ToastType.ERROR)
            }
            loading = false
        }
    }
}

class KeyBrowserState(
    val vm: AppViewModel,
    val session: Session,
    private val scope: CoroutineScope,
) {
    var viewMode by mutableStateOf("tree")
    var searchPattern by mutableStateOf("")
    val keys = mutableStateListOf<String>()
    var cursor by mutableStateOf(0L)
    var loadedAll by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var total by mutableStateOf(0L)
    var selectedKey by mutableStateOf<String?>(null)
    var selectedType by mutableStateOf<RedisType?>(null)
    var tree by mutableStateOf<List<KeyTreeNode>>(emptyList())
    val keyTypes = mutableStateMapOf<String, RedisType>()
    var collapsedDirs by mutableStateOf(setOf<String>())

    fun toggleDir(path: String) {
        collapsedDirs = if (path in collapsedDirs) collapsedDirs - path else collapsedDirs + path
    }

    fun refresh() {
        keys.clear()
        cursor = 0
        loadedAll = false
        total = 0
        loadMore()
    }

    fun loadMore() {
        if (loading || loadedAll) return
        loading = true
        scope.launch {
            try {
                val pattern = searchPattern.trim().ifEmpty { null }
                if (pattern == null && cursor == 0L) {
                    total = session.client.dbsize()
                }
                val res = session.client.scan(cursor, pattern, vm.settings.batchSize)
                keys.addAll(res.keys)
                cursor = res.cursor
                loadedAll = res.cursor == 0L
                rebuildTree()
            } catch (e: Exception) {
                vm.showToast("加载 Key 失败：${e.message}", ToastType.ERROR)
            } finally {
                loading = false
            }
        }
    }

    fun applySearch() {
        keys.clear()
        cursor = 0
        loadedAll = false
        total = 0
        selectedKey = null
        selectedType = null
        loadMore()
    }

    fun selectKey(key: String) {
        selectedKey = key
        selectedType = null
        scope.launch {
            try {
                selectedType = session.client.type(key)
            } catch (e: Exception) {
                vm.showToast("获取 Key 类型失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    fun removeKey(key: String) {
        keys.removeAll { it == key }
        keyTypes.remove(key)
        if (selectedKey == key) {
            selectedKey = null
            selectedType = null
        }
        rebuildTree()
    }

    fun renameKey(oldKey: String, newKey: String) {
        val idx = keys.indexOfFirst { it == oldKey }
        if (idx >= 0) keys[idx] = newKey
        val type = keyTypes.remove(oldKey)
        if (type != null) keyTypes[newKey] = type
        if (selectedKey == oldKey) selectedKey = newKey
        rebuildTree()
    }

    suspend fun ensureType(key: String): RedisType {
        keyTypes[key]?.let { return it }
        val type = session.client.type(key)
        keyTypes[key] = type
        return type
    }

    fun flatRows(): List<String> = keys.toList()

    private fun rebuildTree() {
        tree = buildKeyTree(keys, session.connection.separator)
    }
}

class AppViewModel(private val scope: CoroutineScope) {
    var connections by mutableStateOf(loadConnections())
    var settings by mutableStateOf(loadSettings())
    val sessions = mutableStateMapOf<String, Session>()
    val tabs = mutableStateListOf<WorkspaceTab>()
    var activeTabId by mutableStateOf<String?>(null)
    var newConnectionDialog by mutableStateOf<NewConnectionDialogData?>(null)
    var confirmRequest by mutableStateOf<ConfirmRequest?>(null)
    var renameRequest by mutableStateOf<String?>(null)
    var ttlRequest by mutableStateOf<String?>(null)
    var ttlRequestInitial by mutableStateOf(0L)
    var newKeyRequest by mutableStateOf<String?>(null)
    val toasts = mutableStateListOf<Toast>()
    val contextMenu = ContextMenuState()
    val keyBrowsers = mutableStateMapOf<String, KeyBrowserState>()
    val consoles = mutableStateMapOf<String, ConsoleState>()
    val serverInfos = mutableStateMapOf<String, ServerInfoState>()
    private var toastCounter = 0L

    fun connectionById(id: String?): RedisConnection? = connections.firstOrNull { it.id == id }

    fun openTab(connectionId: String, type: TabType) {
        val tabId = tabId(connectionId, type)
        if (tabs.none { it.id == tabId }) {
            val connection = connectionById(connectionId)
            val title = when (type) {
                TabType.KEYS -> connection?.name ?: "Keys"
                TabType.CONSOLE -> "${connection?.name ?: ""} · 控制台"
                TabType.SERVER_INFO -> "${connection?.name ?: ""} · 服务器信息"
                TabType.SETTINGS -> "设置"
            }
            tabs.add(WorkspaceTab(tabId, title, connectionId, type))
            when (type) {
                TabType.KEYS -> {
                    connectSession(connectionId)
                    keyBrowsers.getOrPut(tabId) { KeyBrowserState(this, sessions.getValue(connectionId), scope) }
                }
                TabType.CONSOLE -> {
                    connectSession(connectionId)
                    consoles.getOrPut(tabId) { ConsoleState(this, sessions.getValue(connectionId), scope) }
                }
                TabType.SERVER_INFO -> {
                    connectSession(connectionId)
                    serverInfos.getOrPut(tabId) { ServerInfoState(this, sessions.getValue(connectionId), scope) }.refresh()
                }
                TabType.SETTINGS -> Unit
            }
        }
        activeTabId = tabId
    }

    fun openSettings() {
        val tabId = "settings"
        if (tabs.none { it.id == tabId }) {
            tabs.add(WorkspaceTab(tabId, "设置", null, TabType.SETTINGS))
        }
        activeTabId = tabId
    }

    fun closeTab(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        tabs.removeAll { it.id == id }
        keyBrowsers.remove(id)
        consoles.remove(id)
        serverInfos.remove(id)
        if (activeTabId == id) {
            activeTabId = tabs.getOrNull(index.coerceAtMost(tabs.lastIndex))?.id ?: tabs.lastOrNull()?.id
        }
    }

    fun connectSession(connectionId: String) {
        val connection = connectionById(connectionId) ?: return
        val session = sessions.getOrPut(connectionId) { Session(connectionId, connection) }
        if (session.status == ConnectionStatus.CONNECTED || session.status == ConnectionStatus.CONNECTING) return
        session.status = ConnectionStatus.CONNECTING
        scope.launch {
            try {
                session.client.ping()
                session.client.select(session.currentDb)
                session.dbs = loadDbInfos(session)
                session.status = ConnectionStatus.CONNECTED
                showToast("已连接 ${connection.name}", ToastType.SUCCESS)
            } catch (e: Exception) {
                session.status = ConnectionStatus.ERROR
                session.statusError = e.message
                showToast("连接 ${connection.name} 失败：${e.message}", ToastType.ERROR, e.stackTraceToString())
            }
        }
    }

    fun disconnectSession(connectionId: String) {
        val session = sessions[connectionId] ?: return
        scope.launch {
            runCatching { session.client.close() }
        }
        session.status = ConnectionStatus.DISCONNECTED
        sessions.remove(connectionId)
        keyBrowsers.values.filter { it.session.connectionId == connectionId }.forEach { it.refresh() }
    }

    fun switchDb(connectionId: String, db: Int) {
        val session = sessions[connectionId] ?: return
        session.currentDb = db
        scope.launch {
            try {
                session.client.select(db)
                showToast("已切换到 db$db", ToastType.SUCCESS)
            } catch (e: Exception) {
                showToast("切换 db 失败：${e.message}", ToastType.ERROR)
            }
        }
        keyBrowsers.values.filter { it.session.connectionId == connectionId }.forEach { it.refresh() }
    }

    fun openNewConnection(editing: RedisConnection? = null) {
        newConnectionDialog = NewConnectionDialogData(editing)
    }

    fun addConnection(connection: RedisConnection) {
        val fresh = connection.withFreshId()
        connections = connections + fresh
        saveConnections(connections)
        showToast("已保存连接 ${fresh.name}", ToastType.SUCCESS)
    }

    fun updateConnection(connection: RedisConnection) {
        connections = connections.map { if (it.id == connection.id) connection else it }
        saveConnections(connections)
        val session = sessions[connection.id]
        if (session != null) {
            session.status = ConnectionStatus.DISCONNECTED
            sessions.remove(connection.id)
        }
        showToast("已更新连接 ${connection.name}", ToastType.SUCCESS)
    }

    fun deleteConnection(id: String) {
        val connection = connectionById(id) ?: return
        sessions.remove(id)?.let { scope.launch { runCatching { it.client.close() } } }
        tabs.removeAll { it.connectionId == id }
        connections = connections.filterNot { it.id == id }
        saveConnections(connections)
        showToast("已删除连接 ${connection.name}", ToastType.SUCCESS)
    }

    fun duplicateConnection(id: String) {
        val connection = connectionById(id) ?: return
        val copy = connection.copy(name = "${connection.name} 副本", id = "")
        addConnection(copy)
    }

    fun updateSettings(newSettings: com.czh.redis.data.AppSettings) {
        settings = newSettings
        saveSettings(newSettings)
    }

    fun showToast(message: String, type: ToastType, detail: String? = null) {
        val toast = Toast(++toastCounter, message, type, detail)
        toasts.add(toast)
        scope.launch {
            delay(3500)
            toasts.remove(toast)
        }
    }

    fun confirm(message: String, confirmText: String = "删除", onConfirm: () -> Unit) {
        confirmRequest = ConfirmRequest(message, confirmText, onConfirm)
    }

    fun requestRefresh(connectionId: String) {
        keyBrowsers.values.filter { it.session.connectionId == connectionId }.forEach { it.refresh() }
    }

    fun deleteKey(key: String) {
        val browser = activeBrowser() ?: return
        val session = sessions[browser.session.connectionId] ?: return
        scope.launch {
            try {
                session.client.del(key)
                browser.removeKey(key)
                requestRefresh(browser.session.connectionId)
                showToast("已删除 $key", ToastType.SUCCESS)
            } catch (e: Exception) {
                showToast("删除失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    fun renameKeyTo(oldKey: String, newKey: String) {
        val browser = activeBrowser() ?: return
        val session = sessions[browser.session.connectionId] ?: return
        scope.launch {
            try {
                session.client.rename(oldKey, newKey)
                browser.renameKey(oldKey, newKey)
                showToast("已重命名 $oldKey → $newKey", ToastType.SUCCESS)
            } catch (e: Exception) {
                showToast("重命名失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    fun setTtl(key: String, ttl: Long) {
        val browser = activeBrowser() ?: return
        val session = sessions[browser.session.connectionId] ?: return
        scope.launch {
            try {
                if (ttl < 0) {
                    session.client.persist(key)
                    showToast("已清除 $key 的过期时间", ToastType.SUCCESS)
                } else {
                    session.client.expire(key, ttl)
                    showToast("已设置 $key TTL 为 $ttl 秒", ToastType.SUCCESS)
                }
            } catch (e: Exception) {
                showToast("设置 TTL 失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    fun activeBrowser(): KeyBrowserState? = keyBrowsers[activeTabId]

    private suspend fun loadDbInfos(session: Session): List<DbInfo> {
        val info = session.client.info("keyspace")
        val counts = mutableMapOf<Int, Long>()
        for (line in info.lines()) {
            val match = Regex("db(\\d+):keys=(\\d+)").find(line)
            if (match != null) {
                counts[match.groupValues[1].toInt()] = match.groupValues[2].toLong()
            }
        }
        return (0 until 16).map { DbInfo(it, counts[it] ?: 0) }
    }

    private fun tabId(connectionId: String, type: TabType): String = "$connectionId-${type.name}"
}

data class NewConnectionDialogData(val editing: RedisConnection?)

fun parseArgs(command: String): List<String> {
    val args = mutableListOf<String>()
    val current = StringBuilder()
    var inQuote = false
    for (ch in command) {
        when {
            ch == '"' -> inQuote = !inQuote
            ch == ' ' && !inQuote -> {
                if (current.isNotEmpty()) {
                    args.add(current.toString())
                    current.clear()
                }
            }
            else -> current.append(ch)
        }
    }
    if (current.isNotEmpty()) args.add(current.toString())
    return args
}
