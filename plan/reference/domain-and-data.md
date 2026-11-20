# 领域模型、数据层与状态契约

> 本文是 Phase 2 及之后所有阶段的**接口契约**。实现可以调整内部细节，但公开类型/职责变更必须同步更新本文与受影响的阶段文档。

---

## 1. 包结构

```text
domain/
├── connection/        ConnectionProfile, TlsConfig, SshConfig, ConnectionEnvironment, ConnectionValidation
├── key/               KeyType, KeySummary, KeyDetail, KeyMetadata, KeyValue, HashField, StreamEntry, KeyNode
├── command/           RedisReply, CommandStat, SlowLogEntry, ClientConnection
├── server/            ServerSnapshot, MetricsSample, ServerHealth
├── settings/          AppSettings, ThemeMode, Accent, UiDensity, KeyView
└── repository/        ConnectionRepository, SettingsRepository, PasswordCipher

data/
├── redis/             RedisGateway, RedisGatewayFactory, ValueCodec, InfoParser
├── jedis/             JedisRedisGateway, JedisGatewayFactory   (jvmMain)
└── storage/           JsonConnectionRepository, JsonSettingsRepository, AesGcmPasswordCipher, FilePaths (jvmMain)
```

命名规则：

- 接口/抽象类型不加 `I` 前缀。
- `RedisClient` 统一改叫 `RedisGateway`（避免和 Jedis Client 混淆）。
- 持久化实体用 `Profile`（`ConnectionProfile`），运行时会话用 `RedisSession`。
- UI 状态用 `XxxState`，不要叫 `ViewModel`。
- 所有时间戳字段以 `EpochMillis` 结尾；时长字段以 `Seconds` / `Millis` 结尾。

---

## 2. 连接模型

```kotlin
@Serializable
enum class ConnectionEnvironment { PRODUCTION, STAGING, LOCAL, ANALYTICS, OTHER }

@Serializable
enum class ConnectionColor { IRIS, MINT, AMBER, SKY, ROSE, LILAC }

@Serializable
enum class TlsVerifyMode { STRICT, ALLOW_SELF_SIGNED, NONE }

@Serializable
data class TlsConfig(
    val enabled: Boolean = false,
    val caPath: String = "",
    val clientCertPath: String = "",
    val clientKeyPath: String = "",
    val verifyMode: TlsVerifyMode = TlsVerifyMode.STRICT,
)

@Serializable
enum class SshAuthMethod { PRIVATE_KEY, PASSWORD }

@Serializable
data class SshConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 22,
    val username: String = "",
    val authMethod: SshAuthMethod = SshAuthMethod.PRIVATE_KEY,
    val password: String = "",
    val privateKeyPath: String = "",
)

@Serializable
data class ConnectionProfile(
    val id: String,
    val name: String,
    val environment: ConnectionEnvironment = ConnectionEnvironment.LOCAL,
    val host: String,
    val port: Int = 6379,
    val username: String = "",
    val password: String = "",
    val defaultDatabase: Int = 0,
    val keySeparator: String = ":",
    val color: ConnectionColor = ConnectionColor.IRIS,
    val group: String = "",
    val tls: TlsConfig = TlsConfig(),
    val ssh: SshConfig = SshConfig(),
    val readOnly: Boolean = false,
    val connectTimeoutMillis: Int = 5_000,
    val readTimeoutMillis: Int = 5_000,
    val lastOpenedAtEpochMillis: Long? = null,
    val createdAtEpochMillis: Long = 0,
    val updatedAtEpochMillis: Long = 0,
)
```

```kotlin
data class ConnectionTestResult(
    val success: Boolean,
    val latencyMillis: Long? = null,
    val version: String? = null,
    val errorMessage: String? = null,
)
```

`ConnectionTestResult` 只包含表单“测试连接”需要的结果；临时 gateway 在 `AppState` 内创建并始终关闭。

### 2.1 校验

```kotlin
enum class ConnectionField {
    NAME, HOST, PORT, USERNAME, PASSWORD, DATABASE,
    TLS_CA, TLS_CERT, TLS_KEY,
    SSH_HOST, SSH_PORT, SSH_USER, SSH_KEY, SSH_PASSWORD,
    CONNECT_TIMEOUT, READ_TIMEOUT,
}

data class ConnectionValidation(val errors: Map<ConnectionField, String>) {
    val isValid: Boolean get() = errors.isEmpty()
}

fun validateConnection(profile: ConnectionProfile): ConnectionValidation
```

规则：

- `name` 非空；`host` 非空；端口 1..65535；默认 DB 0..15（不强制上限，Redis 可配置，但 UI 输入按 0..255 校验）。
- `tls.enabled` 时证书路径可为空（使用系统信任链），但填了就必须是绝对路径或 `~` 开头。
- `ssh.enabled` 时 host/port/username 必填；私钥方式需 privateKeyPath。
- 错误文案中文，展示在表单字段下方与顶部汇总。

---

## 3. Key 模型

```kotlin
enum class KeyType { STRING, HASH, LIST, SET, ZSET, STREAM, NONE }

data class ScanPage(val cursor: Long, val keys: List<String>)

data class DatabaseInfo(val index: Int, val keyCount: Long)

data class KeySummary(
    val name: String,
    val type: KeyType,
    val ttlSeconds: Long = -1,
)

data class KeyMetadata(
    val ttlSeconds: Long = -1,
    val memoryBytes: Long? = null,
    val encoding: String? = null,
    val elementCount: Long? = null,
)

data class HashField(val field: String, val value: String)
data class SortedSetEntry(val member: String, val score: Double)
data class StreamEntry(
    val id: String,
    val fields: List<Pair<String, String>>,
)

sealed interface KeyValue {
    data class StringValue(
        val text: String,
        val isJson: Boolean,
        val isBinary: Boolean = false,
        val byteLength: Long = text.encodeToByteArray().size.toLong(),
    ) : KeyValue
    data class HashValue(val fields: List<HashField>) : KeyValue
    data class ListValue(val items: List<String>) : KeyValue
    data class SetValue(val members: List<String>) : KeyValue
    data class SortedSetValue(val entries: List<SortedSetEntry>) : KeyValue
    data class StreamValue(val entries: List<StreamEntry>, val groupCount: Int?) : KeyValue
}

- `StringValue.isBinary = true` 表示原始字节不是合法 UTF-8，此时 `text` 是 HEX 展示，UI 必须禁止文本编辑/写回。
- `StringValue.byteLength` 是原始值字节数，用于大值截断与 VALUE SIZE 元信息。

data class KeyDetail(
    val summary: KeySummary,
    val metadata: KeyMetadata,
    val value: KeyValue,
)
```

### 3.1 树

```kotlin
data class KeyNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val count: Int,
    val key: KeySummary?,
    val children: List<KeyNode>,
)

object KeyTreeBuilder {
    fun build(keys: List<String>, separator: String): List<KeyNode>
}
```

- 空分隔符时全部按平铺处理。
- 目录按名称排序，Key 排在目录之后或统一按字典序（与设计一致：目录优先）。
- 目录 path 不含末尾分隔符。
- 构建保持稳定顺序，避免刷新时闪烁。

### 3.2 编辑器状态（Phase 5 细化）

编辑器不放进 `KeyDetail`，而是 feature 内的可变状态：

```kotlin
data class EditableHashField(var originalField: String?, var field: String, var value: String)
data class EditableListMember(val index: Long, var value: String)
data class EditableSetMember(var original: String?, var value: String)
data class EditableSortedSetEntry(var originalMember: String?, var member: String, var score: String)
```

---

## 4. 命令 / 服务端模型

```kotlin
sealed interface RedisReply {
    data class Simple(val value: String) : RedisReply
    data class Error(val message: String) : RedisReply
    data class Integer(val value: Long) : RedisReply
    data class Bulk(val value: ByteArray?) : RedisReply
    data class Array(val items: List<RedisReply>) : RedisReply
    data object Nil : RedisReply
}
```

`Bulk(value = null)` 表示 nil bulk；也可以统一用 `Nil`，二选一，但全项目必须一致。建议用 `Nil` + `Bulk` 非空。

```kotlin
data class ServerSnapshot(
    val sections: Map<String, Map<String, String>>,
    val version: String?,
    val uptimeSeconds: Long?,
    val usedMemoryBytes: Long?,
    val maxMemoryBytes: Long?,
    val connectedClients: Int?,
    val totalKeys: Long?,
    val keyspaceHits: Long?,
    val keyspaceMisses: Long?,
    val totalCommandsProcessed: Long?,
    val instantaneousOpsPerSecond: Double?,
    val databases: List<DatabaseInfo>,
    val replicationRole: String?,
    val rdbLastSaveEpochSeconds: Long?,
    val aofEnabled: Boolean?,
)

data class CommandStat(
    val name: String,
    val calls: Long,
    val totalUsec: Long,
    val usecPerCall: Double,
)

data class SlowLogEntry(
    val id: Long,
    val timestampEpochSeconds: Long,
    val durationMicros: Long,
    val command: List<String>,
    val clientAddress: String?,
    val clientName: String?,
)

data class ClientConnection(
    val id: String,
    val address: String,
    val name: String,
    val database: Int,
    val idleSeconds: Long,
)

data class MetricsSample(
    val atEpochMillis: Long,
    val usedMemoryBytes: Long,
    val opsPerSecond: Double,
    val hitRate: Double,
    val connectedClients: Int,
    val totalCommands: Long,
    val commandCalls: Map<String, Long>,
)
```

`ServerSnapshot` 由 `InfoParser` 生成；原始 sections 保留，便于设置页/Console 展示。

---

## 5. 设置模型

```kotlin
@Serializable
enum class ThemeMode { SYSTEM, DARK, LIGHT }

@Serializable
enum class Accent { IRIS, MINT, SKY, AMBER, ROSE }

@Serializable
enum class UiDensity { COMFORTABLE, COMPACT }

@Serializable
enum class KeyView { TREE, FLAT }

@Serializable
data class AppSettings(
    val schemaVersion: Int = 1,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.IRIS,
    val density: UiDensity = UiDensity.COMFORTABLE,
    val uiFont: String = "Inter",
    val editorFont: String = "JetBrains Mono",
    val editorFontSize: Int = 13,
    val defaultValueFormat: ValueFormat = ValueFormat.AUTO,
    val wordWrap: Boolean = true,
    val showLineNumbers: Boolean = true,
    val scanBatchSize: Int = 1_000,
    val truncateValueKb: Int = 10,
    val keyView: KeyView = KeyView.TREE,
    val defaultKeySeparator: String = ":",
    val showKeyTtl: Boolean = true,
    val confirmDeleteKeys: Boolean = true,
    val confirmDangerousCommands: Boolean = true,
    val maskSensitiveValues: Boolean = false,
    val globalReadOnly: Boolean = false,
    val encryptPasswords: Boolean = true,
    val autoRefreshSeconds: Int = 30,
    val recentCommands: List<String> = emptyList(),
)

@Serializable
enum class ValueFormat { AUTO, JSON, TEXT, HEX }
```

- `theme = SYSTEM` 时由 Compose 的 `isSystemInDarkTheme()` 决定；主题选择本身仍保存为 `SYSTEM`。
- 强调色计算见 `design-tokens.md`。
- 写入节流：设置变更立即更新内存状态，文件写入 debounce 300ms（Phase 9 实现）。

---

## 6. RedisGateway

```kotlin
interface RedisGateway {
    suspend fun ping(): String
    suspend fun info(section: String? = null): String
    suspend fun databaseCount(): Int?   // CONFIG GET databases; null when hidden
    suspend fun selectDatabase(index: Int)
    suspend fun databaseSize(): Long

    suspend fun scan(cursor: Long, pattern: String?, count: Int): ScanPage

    suspend fun keyType(key: String): KeyType
    suspend fun keyTtl(key: String): Long
    suspend fun keyMemoryUsage(key: String): Long?
    suspend fun keyEncoding(key: String): String?

    suspend fun getString(key: String): ByteArray?
    suspend fun setString(key: String, value: ByteArray)

    suspend fun getHash(key: String): List<HashFieldBytes>
    suspend fun hashSet(key: String, field: String, value: ByteArray)
    suspend fun hashDelete(key: String, field: String)

    suspend fun getList(key: String): List<ByteArray>
    suspend fun listPush(key: String, value: ByteArray, head: Boolean)
    suspend fun listSet(key: String, index: Long, value: ByteArray)
    suspend fun listRemove(key: String, value: ByteArray)

    suspend fun getSet(key: String): List<ByteArray>
    suspend fun setAdd(key: String, value: ByteArray)
    suspend fun setRemove(key: String, value: ByteArray)

    suspend fun getSortedSet(key: String): List<SortedSetEntryBytes>
    suspend fun sortedSetAdd(key: String, member: ByteArray, score: Double)
    suspend fun sortedSetRemove(key: String, member: ByteArray)

    suspend fun getStream(key: String, count: Int = 50): List<StreamEntryBytes>
    suspend fun getStreamGroupCount(key: String): Int?

    suspend fun deleteKeys(keys: List<String>)
    suspend fun renameKey(oldKey: String, newKey: String)
    suspend fun expire(key: String, seconds: Long)
    suspend fun persist(key: String)

    suspend fun slowLog(count: Int = 10): List<SlowLogEntry>
    suspend fun clients(): List<ClientConnection>
    suspend fun commandStats(): List<CommandStat>

    suspend fun execute(args: List<String>): RedisReply

    /** 快速释放连接；允许在主线程调用，内部自行处理阻塞。 */
    fun close()
}
```

字节级辅助类型放在 data 层，避免 domain 依赖 ByteArray 包装的命名混乱：

```kotlin
data class HashFieldBytes(val field: String, val value: ByteArray)
data class SortedSetEntryBytes(val member: ByteArray, val score: Double)
data class StreamEntryBytes(val id: String, val fields: List<Pair<String, ByteArray>>)
```

也可以直接在 gateway 返回 `ByteArray` 变体；关键是**编辑后的字符串写回时使用 UTF-8**，非 UTF-8 值显示 HEX 并禁止直接编辑（或编辑后按 HEX 解析）。

### 6.1 实现要求（Jedis）

- 所有操作在 `Dispatchers.IO` 上执行，`Mutex` 串行化同一连接（Jedis 非线程安全）。
- 连接懒创建；`close()` 清空；`ping()` 触发连接。
- SSL / SSH：
  - TLS 用 Jedis 的 SSL socket factory；证书路径支持 PEM。
  - SSH 隧道：本阶段不实现；若 `ssh.enabled`，抛出明确的 `RedisException("SSH 隧道尚未启用")` 并在 UI 侧显示。Phase 8 引入 JSch 并实现端口转发，不要静默忽略 SSH 配置直连 Redis。
- `execute(args)` 用 `Connection.executeCommand` 并解析为递归 `RedisReply`；数组必须保留嵌套，不要提前 `joinToString`。
- `info()` 原样返回字符串；`InfoParser` 单独解析。
- `slowLog()` / `clients()` 可以基于 `execute()` 解析，也可以直接调用 Jedis API；优先 `execute()`，保证与命令控制台一致。

### 6.2 命令映射

| 能力 | Redis 命令 |
| --- | --- |
| 加载 Key | `SCAN cursor MATCH pattern COUNT n` |
| Key 总数 | `DBSIZE` |
| 类型 | `TYPE key` |
| TTL | `TTL key` |
| 内存 | `MEMORY USAGE key [SAMPLES n]` |
| 编码 | `OBJECT ENCODING key` |
| String | `GET` / `SET` |
| Hash | `HGETALL` / `HSET` / `HDEL` |
| List | `LRANGE` / `LPUSH` / `RPUSH` / `LSET` / `LREM` |
| Set | `SMEMBERS` / `SADD` / `SREM` |
| ZSet | `ZRANGE key 0 -1 WITHSCORES` / `ZADD` / `ZREM` |
| Stream | `XRANGE key - + COUNT n` / `XINFO GROUPS key` |
| 删除 | `DEL` |
| 重命名 | `RENAME` |
| 过期 | `EXPIRE` / `PERSIST` |
| 服务器信息 | `INFO` / `INFO section` |
| 慢查询 | `SLOWLOG GET n` |
| 客户端 | `CLIENT LIST` |
| 命令统计 | `INFO commandstats` |
| 控制台 | 任意命令 |

---

## 7. 持久化

### 7.1 文件

```text
~/.redis-compose/
├── settings.json
├── connections.json
└── secret.key        # 32 字节，仅当前用户可读
```

保留旧路径以便已有用户数据可读。JSON 使用 `ignoreUnknownKeys = true`、`prettyPrint = true`。

### 7.2 Repository

```kotlin
interface SettingsRepository {
    suspend fun load(): AppSettings
    suspend fun save(settings: AppSettings)
}

interface ConnectionRepository {
    suspend fun load(): List<ConnectionProfile>
    suspend fun save(connections: List<ConnectionProfile>)
}
```

- 写入采用临时文件 + 原子 move。
- 父目录自动创建。
- POSIX 下 `secret.key` 建议 `rw-------`；Windows 忽略权限失败。
- 失败不崩溃：记录 `println` 或通过返回结果上报；App 启动使用默认值。

### 7.3 密码加密

```kotlin
interface PasswordCipher {
    fun encrypt(plain: String): String
    fun decrypt(stored: String): String
}
```

- `AesGcmPasswordCipher`：AES-256-GCM，12 字节随机 IV；输出 `enc:v1:<base64(iv + ciphertext)>`。
- 密钥文件不存在时生成 32 字节随机数；读取失败则本次会话使用明文并提示。
- Repository 在保存前加密所有密码字段（Redis password / SSH password），加载后解密。
- `AppSettings.encryptPasswords = false` 时明文存储；切换设置时不自动重写已有数据（Phase 9 可提供手动迁移）。

---

## 8. 组合根与 AppState

### 8.1 Container

```kotlin
interface AppContainer {
    val settingsRepository: SettingsRepository
    val connectionRepository: ConnectionRepository
    val gatewayFactory: RedisGatewayFactory
    val passwordCipher: PasswordCipher
    val initialSettings: AppSettings
    val initialConnections: List<ConnectionProfile>
}

fun interface RedisGatewayFactory {
    fun create(profile: ConnectionProfile): RedisGateway
}
```

`initialSettings` / `initialConnections` 在容器创建时同步读取（文件很小），让首帧直接使用正确主题与连接列表，避免闪烁。

JVM 实现：

```kotlin
fun createDesktopAppContainer(): AppContainer
```

- 读取初始 settings / connections（同步，文件很小）或提供 `initial*`；在 `DesktopAppContainer` 构造时完成。
- 供测试使用 `FakeAppContainer`（Phase 2 测试可选）。

### 8.2 AppState

```kotlin
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
    val toasts = mutableStateListOf<ToastMessage>()

    var activePage by mutableStateOf(AppPage.WORKSPACE)
    var activeTabId by mutableStateOf<String?>(null)
    var activeConnectionId by mutableStateOf<String?>(null)
    var commandPaletteVisible by mutableStateOf(false)

    fun setPage(page: AppPage)
    fun openConnection(connectionId: String)
    fun openTab(connectionId: String, kind: WorkspaceTabKind)
    fun closeTab(tabId: String)
    fun selectDatabase(connectionId: String, index: Int)
    fun updateSettings(settings: AppSettings)
    fun saveConnection(profile: ConnectionProfile)
    fun deleteConnection(connectionId: String)
    fun duplicateConnection(connectionId: String): ConnectionProfile?
    suspend fun testConnection(profile: ConnectionProfile): ConnectionTestResult
    val connectionLibrary: ConnectionsState
    fun showToast(message: String, kind: ToastKind, detail: String? = null)
    fun dismissToast(id: Long)
}
```

- `settings` / `connections` 的 setter 私有；所有变更通过方法。
- `updateSettings` 立即更新内存 + 启动 debounce 持久化。
- `saveConnection` / `deleteConnection` 同步更新 connections 并持久化；删除时关闭 session/tabs。
- 连接失败只影响对应 session，不阻塞应用。
- `activeConnectionId` 用于侧栏和顶栏面包屑；可以为 null。

### 8.3 Tabs / 页面

```kotlin
enum class AppPage { WORKSPACE, CONNECTIONS, SETTINGS }
enum class WorkspaceTabKind { KEYS, CONSOLE, INSIGHTS }

data class WorkspaceTab(
    val id: String,
    val connectionId: String,
    val kind: WorkspaceTabKind,
    val title: String,
)
```

- `id = "$connectionId:${kind.name.lowercase()}"`，由 helper 生成。
- 打开同一个连接同一类 tab 时复用。
- 关闭当前 tab 时激活右侧 tab，否则左侧，否则空。
- 关闭连接时关闭其全部 tab。

### 8.4 RedisSession

```kotlin
enum class SessionStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

class RedisSession(
    val profile: ConnectionProfile,
    val gateway: RedisGateway,
    private val scope: CoroutineScope,
) {
    var status by mutableStateOf(SessionStatus.DISCONNECTED)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var latencyMillis by mutableStateOf<Long?>(null)
        private set
    var databases by mutableStateOf<List<DatabaseInfo>>(emptyList())
        private set
    var databaseCount by mutableStateOf(DEFAULT_DATABASE_COUNT)
        private set
    var currentDatabase by mutableStateOf(profile.defaultDatabase)
        private set
    var summary by mutableStateOf<ServerSnapshot?>(null)
        private set

    suspend fun connect()
    suspend fun refreshSummary()
    suspend fun refreshDatabases(count: Int? = null)
    suspend fun switchDatabase(index: Int)
    fun disconnect()
}
```

- `connect()`：计时 `ping()` → `latencyMillis`；`selectDatabase(profile.defaultDatabase)`；`refreshDatabases()`；`refreshSummary()`；状态 CONNECTED。
- 失败：`status = ERROR`，`errorMessage` 取异常首行；toast 由 AppState 处理。
- `refreshSummary()` 解析 `INFO` 为 `ServerSnapshot`；供侧栏健康卡、状态栏、Insights 复用。
- `disconnect()`：`gateway.close()`；状态 DISCONNECTED；取消会话作用域。

### 8.5 Feature states

**WorkspaceState（Phase 4/5）**

```kotlin
class WorkspaceState(
    val session: RedisSession,
    val settings: () -> AppSettings,
    val onToast: (String, ToastKind) -> Unit,
    private val scope: CoroutineScope,
    onSettingsChange: (AppSettings) -> Unit = {},
    onOpenConsole: () -> Unit = {},
) {
    val keys = mutableStateListOf<String>()
    var isLoading by mutableStateOf(false)
    var cursor by mutableStateOf(0L)
    var loadedAll by mutableStateOf(false)
    var total by mutableStateOf(0L)
    var searchInput by mutableStateOf("")
    var appliedPattern by mutableStateOf<String?>(null)
    var typeFilter by mutableStateOf<KeyType?>(null)
    var pinnedKeys by mutableStateOf<Set<String>>(emptySet())
    var selectedKey by mutableStateOf<String?>(null)
    var keyDetail by mutableStateOf<KeyDetail?>(null)
    var detailLoading by mutableStateOf(false)
    var detailError by mutableStateOf<String?>(null)
    var isSaving by mutableStateOf(false)
    var hasUnsavedEdits by mutableStateOf(false)
    var ttlDialogKey: String?          // Phase 5 dialogs
    var renameDialogKey: String?
    var deleteDialogKey: String?
    var newKeyDialogVisible: Boolean
    val keyTypes = mutableStateMapOf<String, KeyType>()
    val keyTtls = mutableStateMapOf<String, Long>()

    fun refresh()
    fun loadMore()
    fun applySearch(pattern: String?)
    fun updateTypeFilter(type: KeyType?)
    fun selectKey(key: String)
    fun refreshSelected()
    fun togglePinned(key: String)

    // Phase 5 writes (read-only checked again in state)
    fun saveString(text: String)
    fun saveHashField(row: EditableHashField)
    fun saveHashFields(rows: List<EditableHashField>)
    fun deleteHashField(row: EditableHashField)
    fun saveListMember(index: Long, value: String)
    fun saveListMembers(rows: List<EditableListMember>)
    fun addListMember(value: String, head: Boolean)
    fun deleteListMember(index: Long, value: String)
    fun saveSetMember(row: EditableSetMember)
    fun saveSetMembers(rows: List<EditableSetMember>)
    fun deleteSetMember(row: EditableSetMember)
    fun saveSortedSetEntry(row: EditableSortedSetEntry)
    fun saveSortedSetEntries(rows: List<EditableSortedSetEntry>)
    fun deleteSortedSetEntry(row: EditableSortedSetEntry)
    fun updateTtl(seconds: Long)
    fun renameKey(newName: String)
    fun deleteSelectedKey()
    fun createKey(draft: NewKeyDraft)
    fun requestSave()
    fun consumeSaveRequest(): Boolean
}
```

- 搜索/刷新时取消上一轮 job，避免旧结果覆盖新结果。
- `typeFilter` 在已加载 Key 上过滤；搜索使用 SCAN pattern。两者可叠加。
- `keyTypes` 懒加载：只对可见行请求 `TYPE`，并发上限 6。

**ConsoleState（Phase 6）**

```kotlin
class ConsoleState(
    val session: RedisSession,
    val settings: () -> AppSettings,
    val onToast: (String, ToastKind) -> Unit,
    val onRecordCommand: (String) -> Unit,
    private val scope: CoroutineScope,
    private val onDatabaseSelected: (Int) -> Unit = {},
) {
    val entries = mutableStateListOf<ConsoleEntry>()
    val history = mutableStateListOf<String>()
    val historyItems = mutableStateListOf<ConsoleHistoryItem>()
    var input by mutableStateOf("")
    var isRunning by mutableStateOf(false)
    var suggestionVisible by mutableStateOf(false)
    var historyCursor by mutableStateOf(-1)

    fun execute(raw: String)
    fun clear()
    fun fillFromHistory(direction: Int)
    fun fillCommand(command: String)
    fun updateInput(value: String)
    fun acceptSuggestion(command: String)
    fun plainTextLog(): String
    fun dispose()
}
```

**InsightsState（Phase 7）**

```kotlin
class InsightsState(
    val session: RedisSession,
    private val scope: CoroutineScope,
    private val onToast: (String, ToastKind) -> Unit = { _, _ -> },
) {
    var snapshot by mutableStateOf<ServerSnapshot?>(null)
    var slowLog by mutableStateOf<List<SlowLogEntry>>(emptyList())
    var clients by mutableStateOf<List<ClientConnection>>(emptyList())
    var commandStats by mutableStateOf<List<CommandStat>>(emptyList())
    val samples = mutableStateListOf<MetricsSample>()
    var range by mutableStateOf(InsightsRange.LAST_5_MINUTES)
    var autoRefresh by mutableStateOf(true)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var lastUpdatedEpochMillis by mutableStateOf<Long?>(null)
    var isPolling by mutableStateOf(false)

    fun start()
    fun stop()
    fun refreshNow()
    fun refresh() = refreshNow()
    fun selectRange(value: InsightsRange)
    fun updateAutoRefresh(enabled: Boolean)
    fun dashboardData(): InsightsDashboardData
    fun dispose()
}
```

- `start()` 立即采样一次，之后每 5 秒；`stop()` 取消轮询但保留 `samples`。
- 每 3 轮刷新 `SLOWLOG GET 10` 与 `CLIENT LIST`；`INFO` 与 `COMMANDSTATS` 每轮读取。
- `samples` 仅保留本次会话最多 1 小时（720 条）；关闭 tab 时 `dispose()` 丢弃。
- `setRange`/`setAutoRefresh` 因 JVM setter 签名冲突改名为 `selectRange`/`updateAutoRefresh`。

**ConnectionsState（Phase 8）**

```kotlin
class ConnectionsState(
    private val connections: () -> List<ConnectionProfile>,
    private val runtimeFor: (String) -> ConnectionRuntime,
    private val onOpen: (String) -> Unit,
    private val onSave: (ConnectionProfile) -> Unit,
    private val onDelete: (String) -> Unit,
    private val onDuplicate: (String) -> ConnectionProfile?,
    private val onTest: suspend (ConnectionProfile) -> ConnectionTestResult,
    private val onToast: (String, ToastKind) -> Unit,
    scope: CoroutineScope,
) {
    var search: String
    var environmentFilter: ConnectionEnvironment?
    var sort: ConnectionSort          // RECENT / NAME
    var view: ConnectionView          // GRID / LIST
    var draft: ConnectionDraft?
    var sheetTab: ConnectionSheetTab
    var validationErrors: Map<ConnectionField, String>
    var testResult: ConnectionTestResult?
    var isTesting: Boolean
    var deleteTarget: ConnectionProfile?

    fun openNew()
    fun openEdit(connectionId: String)
    fun openDuplicate(connectionId: String)
    fun save()
    fun testDraft()
    fun testExisting(connectionId: String)
    fun requestDelete(connectionId: String)
    fun confirmDelete()
    fun dispose()
}
```

- `ConnectionDraft` 是表单的可编辑副本，数字字段在编辑期间保持字符串；`toProfile()` 与 `validate()` 负责转换和字段错误。
- 保存/删除/复制/测试都通过 `AppState` 的回调执行；feature 不直接创建 gateway 或写文件。
- `ConnectionRuntime` 只包含卡片可见的运行时事实（状态、延迟、错误、版本、Key 数、客户端数、DB 数），不暴露密码。
- Settings 页仍按 Phase 9 实现，不在本阶段使用 draft。

---

## 9. 线程、错误与资源

- 所有 Redis 调用是 `suspend`，内部切到 `Dispatchers.IO`。
- State 的公开方法从 Compose 主线程调用；`scope.launch` 内 await gateway，恢复后更新状态。
- 同一会话的 Jedis 操作串行化；不同会话可以并行。
- 关闭 tab 取消其 feature scope；关闭窗口取消 app scope 并关闭所有 gateway。
- 异常统一映射：`RedisConnectionException` → “连接失败：…”；命令错误 → 控制台展示 `(error) ...`，不弹 toast（控制台里的错误是正常结果）。
- 大值：`truncateValueKb` 只限制展示，不截断原始数据；编辑保存仍写完整值。

---

## 10. 测试要求（Phase 2 起）

最低单元测试：

- `InfoParser`：解析 Server / Memory / Clients / Stats / Keyspace / Replication / Persistence。
- `KeyTreeBuilder`：分隔符、空列表、深层路径、目录计数、排序。
- `Formatters`：TTL、字节、key 数量、耗时。
- `RedisReply` 渲染/嵌套数组。
- `SlowLogParser`（如果独立）。
- `AppSettings` / `ConnectionProfile` JSON round-trip；旧字段兼容。
- `AesGcmPasswordCipher` encrypt → decrypt；错误密钥失败。
- `JsonHighlighter`（Phase 5）：JSON 合法/非法、字符串转义、换行。

测试不依赖真实 Redis；Gateway 用 fake 实现。
