# Phase 2 — 领域模型与数据层

> **目标**：在没有 UI 的情况下，把新的 domain / data / util 契约实现完，供 Phase 3 及后续页面直接使用。
>
> 依赖：Phase 0  
> 产出：模型、RedisGateway + Jedis、设置/连接持久化、密码加密、解析器、纯函数测试  
> 下一阶段：Phase 3（AppState 与外壳）

---

## 1. 开始前

- 阅读 `plan/reference/domain-and-data.md` 全文。
- 阅读 `plan/reference/coding-standards.md` 的 Kotlin/数据层部分。
- 本阶段不写页面/外壳；应用仍停留在 Phase 1 的画廊。
- 可以阅读旧 `JedisRedisClient` 作为“协议如何调用”的参考，但**不要复制旧包结构/命名/状态管理**。

---

## 2. 文件清单

```text
app/src/commonMain/kotlin/com/czh/rediscompose/domain/
├── connection/
│   ├── ConnectionProfile.kt        # 模型 + TlsConfig + SshConfig + 枚举
│   ├── ConnectionValidation.kt     # validateConnection
│   └── ConnectionRepository.kt
├── key/
│   ├── KeyModels.kt                # KeyType, KeySummary, KeyDetail, KeyMetadata, KeyValue, ...
│   ├── KeyNode.kt
│   └── KeyTreeBuilder.kt
├── command/
│   ├── RedisReply.kt
│   ├── CommandStat.kt
│   ├── SlowLogEntry.kt
│   └── ClientConnection.kt
├── server/
│   ├── ServerSnapshot.kt
│   └── MetricsSample.kt
├── settings/
│   ├── AppSettings.kt              # ThemeMode, Accent, UiDensity, KeyView
│   └── SettingsRepository.kt
└── repository/
    └── PasswordCipher.kt

app/src/commonMain/kotlin/com/czh/rediscompose/data/
├── redis/
│   ├── RedisGateway.kt
│   ├── RedisGatewayFactory.kt
│   ├── RedisException.kt
│   ├── ValueCodec.kt
│   └── InfoParser.kt
└── util/ 或 app/util/
    ├── Formatters.kt               # TTL / bytes / count / duration
    └── SlowLogParser.kt            # 如独立

app/src/jvmMain/kotlin/com/czh/rediscompose/
├── DesktopAppContainer.kt          # createDesktopAppContainer
└── data/
    ├── jedis/JedisRedisGateway.kt
    ├── jedis/JedisGatewayFactory.kt
    └── storage/
        ├── FilePaths.kt
        ├── JsonSettingsRepository.kt
        ├── JsonConnectionRepository.kt
        └── AesGcmPasswordCipher.kt

app/src/commonTest/kotlin/com/czh/rediscompose/
└── ... 测试文件
```

> `AppContainer` / `RedisGatewayFactory` 接口本身可放 `app/` 或 `data/`；建议放 `app/AppContainer.kt`，Phase 3 使用。

---

## 3. 任务

### 3.1 领域模型

按 `domain-and-data.md` §2–§5 实现：

- `ConnectionProfile` + `TlsConfig` + `SshConfig` + 枚举，全部 `@Serializable`。
- `validateConnection`：返回字段级错误；不要抛异常。
- `KeyType`、`ScanPage`、`DatabaseInfo`、`KeySummary`、`KeyMetadata`、`KeyValue`、`HashField`、`SortedSetEntry`、`StreamEntry`、`KeyDetail`。
- `KeyNode` + `KeyTreeBuilder`。
- `RedisReply`（递归数组）、`CommandStat`、`SlowLogEntry`、`ClientConnection`。
- `ServerSnapshot`、`MetricsSample`。
- `AppSettings` 及枚举，默认值与 `design` 设置页一致。

**不要**把 UI 状态放进模型（如 `isLoading`、`expandedDirs`）；这些属于 state holder。

### 3.2 解析工具

`InfoParser`：

```kotlin
object InfoParser {
    fun parse(raw: String): Map<String, Map<String, String>>
    fun snapshot(raw: String): ServerSnapshot
    fun parseKeyspace(section: Map<String, String>): List<DatabaseInfo>
}
```

- 支持 `# Section` 分组、`key:value`、值中含冒号。
- `ServerSnapshot` 缺字段时用 null，不填 0（0 是有效值）。
- 解析 `db0:keys=...,expires=...,avg_ttl=...`。
- 解析 `cmdstat_get:calls=...,usec=...,usec_per_call=...`（如果放在 CommandStat 解析）。

`ValueCodec`：

```kotlin
object ValueCodec {
    fun decode(bytes: ByteArray): String?   // 合法 UTF-8 返回文本，否则 null
    fun hex(bytes: ByteArray): String
    fun isJson(text: String): Boolean
    fun prettyJson(text: String): String?   // 失败返回 null
    fun hexToBytes(hex: String): ByteArray?
}
```

- UTF-8 合法性检查（不能简单 `String(bytes)` 后比较）。
- JSON 使用 kotlinx.serialization `Json.parseToJsonElement` 验证；pretty 输出保持稳定。
- HEX 支持空格分隔输入。

`Formatters`：

```kotlin
fun formatTtl(seconds: Long): String        // 永久 / 42s / 28m 14s / 1h 0m / 2d 3h
fun formatBytes(bytes: Long?): String       // 1.4 KB
fun formatCount(count: Long): String        // 12,480
fun formatDurationMicros(micros: Long): String
fun formatEpochSeconds(seconds: Long): String  // 本地时间 HH:mm:ss
```

### 3.3 RedisGateway 接口

按 `domain-and-data.md` §6 定义；本阶段实现所有方法：

- 字符串/哈希/列表/集合/ZSet/Stream 的读取与写入。
- TTL / 内存 / 编码 / 删除 / 重命名 / 过期 / 持久化。
- INFO / SLOWLOG / CLIENT LIST / commandstats。
- `execute` 返回递归 `RedisReply`。
- `close()`。

`RedisGatewayFactory` 接口 + JVM 实现。

### 3.4 Jedis 实现

- 使用 `Dispatchers.IO` + `Mutex` 串行化同一实例。
- 懒连接；`ping()` 建连。
- 超时、SSL、ACL 用户/密码。
- 数组/嵌套数组解析不得丢失结构；`RedisReply.Array` 要保留嵌套。
- `slowLog` 用 `execute(listOf("SLOWLOG","GET", count))` 解析；`clients` 解析 `CLIENT LIST` 文本行。
- `commandStats` 可以直接解析 `INFO commandstats`。
- Stream：`XRANGE`、`XINFO GROUPS`；返回字节字段，由 UI 转字符串/HEX。
- **SSH**：
  - 本阶段不实现隧道；若 `profile.ssh.enabled`，抛出明确的 `RedisException("SSH 隧道将在连接阶段启用")` 并在 UI 侧显示。
  - 不得静默忽略 SSH 配置直连 Redis。
  - Phase 8 再决定引入 JSch 并实现端口转发。
- **TLS**：
  - 至少支持 `tls.enabled = true` 的系统信任链。
  - 自定义 CA/客户端证书如果实现成本过高，可留 `TODO(phase-8)`，但必须抛错而非降级成明文。
  - 不要把证书路径打印到日志。

### 3.5 持久化

- `FilePaths`：`~/.redis-compose/{settings.json,connections.json,secret.key}`。
- `JsonSettingsRepository` / `JsonConnectionRepository`：
  - `suspend` 方法，内部 `Dispatchers.IO`。
  - 原子写入：临时文件 → `move(REPLACE_EXISTING)`。
  - 读取失败 → 默认值，不崩溃。
  - `ignoreUnknownKeys = true`，兼容旧字段。
- `AesGcmPasswordCipher` 按 `domain-and-data.md` §7.3。
- 连接保存时加密 `password` 与 `ssh.password`；加载时解密。
- 旧 `connections.json` 是裸数组；新仓库也写裸数组，避免迁移。如果新模型有额外字段，旧数据应能读入（默认值）。

### 3.6 Container

`app/src/jvmMain/.../DesktopAppContainer.kt`：

```kotlin
fun createDesktopAppContainer(): AppContainer
```

- 构造 repository / cipher / gateway factory。
- 读取初始 settings 与 connections（文件很小，允许同步；失败用默认值）。
- 为 Phase 3 提供 initial 数据，避免 UI 初始化闪烁。
- 不要在此处创建 UI scope 或 AppState。

### 3.7 测试

至少覆盖：

- `InfoParserTest`：完整/缺字段/keyspace。
- `KeyTreeBuilderTest`：多层、空、单 Key、分隔符为空。
- `FormattersTest`：TTL 边界（-1、0、59、60、3600、86400+）、bytes、count。
- `ValueCodecTest`：UTF-8、非法 UTF-8、JSON pretty、HEX。
- `RedisReplyTest`：嵌套数组渲染/解析（如放在 gateway 层则测解析函数）。
- `SlowLogParserTest`。
- `AesGcmPasswordCipherTest`：round-trip、错误密文不抛到 UI（返回空/异常由 repository 处理）。
- `ConnectionValidationTest`：必填、端口、TLS/SSH 条件。
- `SettingsSerializationTest`：默认值、未知字段、枚举序列化。

FakeGateway 可放在 `commonTest`，为后续页面测试复用。

---

## 4. 非目标

- 不实现 AppState、会话管理、UI。
- 不实现 SSH 隧道（Phase 8）。
- 不实现导入/导出连接。
- 不实现指标采样循环（Phase 7）。
- 不实现命令补全数据（Phase 6）。

---

## 5. 验收

```bash
./gradlew :app:jvmTest
./gradlew ktlintCheck
./gradlew build
```

- [ ] 所有领域模型与接口按 reference 实现，命名一致。
- [ ] 用一个本地 Redis（如果可用）手动跑通：ping → scan → 读 6 种类型 → 写回 → TTL → 删除。
- [ ] 无 SSH 配置时连接正常；启用 SSH 时给出明确错误而非静默直连。
- [ ] `settings.json` / `connections.json` 可保存并重新加载；密码在文件中是 `enc:v1:` 前缀（默认设置）。
- [ ] `InfoParser` 对真实 `INFO` 输出返回正确的 keyspace 与内存字段。
- [ ] 单元测试全部通过。
- [ ] 应用仍可启动到 Phase 1 画廊，无 UI 回归。
- [ ] `plan/STATUS.md` 记录 SSH/TLS 的实现边界。

---

## 6. STOP

停止。Phase 3 将创建 `AppState`、会话管理、外壳与导航；不要提前实现页面内容。
