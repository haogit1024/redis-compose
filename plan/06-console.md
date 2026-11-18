# Phase 6 — 命令控制台

> **目标**：实现真实可用的命令控制台：命令执行、递归结果展示、历史、补全、收藏命令、只读拦截。
>
> 依赖：Phase 3、Phase 5（值面板可用后可跳转控制台）  
> 下一阶段：Phase 7（洞察）

---

## 1. 开始前

- 阅读 `design/console.html` 与 `design/assets/pages.css` 中 `.console-*`、`.terminal`、`.term-*`、`.suggestions`、`.console-side`。
- 终端始终使用深色 token（`terminalBackground`），即使在浅色主题下。
- 阅读 `plan/reference/domain-and-data.md` 的 `RedisReply` 与 `ConsoleState`。
- 不实现指标图表、连接表单、设置页。

---

## 2. 文件清单

```text
feature/console/
├── ConsoleScreen.kt
├── ConsoleState.kt
├── ConsoleEntry.kt            # sealed：Command / Output / Error / Note
├── ConsoleLog.kt              # LazyColumn + 自动滚动
├── ConsoleBlock.kt            # 命令块 / 结果块渲染
├── CommandComposer.kt         # 输入框 + 运行按钮 + 补全弹层
├── ConsoleHistoryPane.kt      # 历史 + 收藏命令 + 提示
└── ConsoleToolbar.kt

util/
└── RedisCommandCatalog.kt     # 命令名 + 语法提示（本地静态表）
└── WriteCommandGuard.kt       # 只读模式命令判定
```

同时修改 `AppShell` 的 CONSOLE 分支与 `AppState` 的 console state 生命周期。

---

## 3. ConsoleState

```kotlin
class ConsoleState(
    val session: RedisSession,
    val settings: () -> AppSettings,
    val onToast: (String, ToastKind) -> Unit,
    val onRecordCommand: (String) -> Unit,
    private val scope: CoroutineScope,
) {
    val entries = mutableStateListOf<ConsoleEntry>()
    val history = mutableStateListOf<String>()
    var input by mutableStateOf("")
    var isRunning by mutableStateOf(false)
    var suggestionVisible by mutableStateOf(false)
    var historyCursor by mutableStateOf(-1)

    fun execute(raw: String)
    fun clear()
    fun fillFromHistory(direction: Int)
    fun fillCommand(command: String)
    fun copyLogToClipboard()
}
```

行为：

- 初始化时插入一条 `Note`：“Redis Compose shell · connected to host:port (version)”。
- `execute`：
  - trim；空输入忽略；
  - 记录历史（去重相邻重复）；
  - 写入 `Command` entry（时间、命令）；
  - 只读校验（global 或连接级）→ 写命令则写 `Error` entry：“只读模式已开启，写命令被拦截：SET”，不执行；
  - `isRunning` 防重入；
  - 调 `gateway.execute(args)`；
  - 将 `RedisReply` 转为 `Output`/`Error` entry；
  - 成功后调用 `onRecordCommand(command)`（AppState 写入 settings.recentCommands，最多 20 条）；
  - 更新 isRunning；自动滚动。
- 命令解析：优先复用一个 `splitCommand` 工具，支持双引号/单引号与空格；解析失败给出错误。不要用简单 `split(" ")` 丢失引号。
- 历史：
  - `fillFromHistory(-1/1)` 在 history 中移动；
  - 到达末尾清空输入；
  - 点击历史行填充输入。
- 清空：只清 entries，不清 history。
- 关闭 tab：取消正在进行命令的 job（如果 gateway 不可取消，至少不更新已关闭页面的状态）。

---

## 4. 结果渲染

`ConsoleEntry` 建议：

```kotlin
sealed interface ConsoleEntry {
    data class Command(val at: String, val text: String) : ConsoleEntry
    data class Note(val text: String) : ConsoleEntry
    data class Output(val reply: RedisReply, val durationMillis: Long?) : ConsoleEntry
    data class Error(val message: String) : ConsoleEntry
}
```

`ConsoleBlock` 渲染：

- Command：时间（8.5sp dim）+ 提示符 `redis@host:port[db]>`（accent）+ 命令（text）。
- Output：
  - Simple：绿色文本；
  - Integer：`(integer) 123` 绿色；
  - Bulk：可打印文本用绿色引号包裹；非 UTF-8 用 HEX，dim；
  - Nil：`(nil)` dim；
  - Error：红色 `(error) ...`；
  - Array：编号列表；嵌套数组用缩进；深度超过 3 或项数超过 200 时截断并提示。
- 命令耗时显示在命令块右侧或结果头部（如果 gateway 能提供；否则只显示总耗时）。
- 结果区使用 Mono 11sp/1.78，宽度较大时保持可读；长行可横向滚动或换行（控制台建议不换行，横向滚动）。
- 不要做 HTML 式卡片包裹每一条数组项；只有 HGETALL 这类 key-value 结果可以用两列表格（可选，Phase 10）。

---

## 5. 界面

### 5.1 布局

```text
Row(fillMaxSize)
├── Column(weight 1, padding 17–18dp)
│   ├── ConsoleToolbar
│   ├── TerminalSurface(weight 1)
│   ├── QuickCommands
│   └── CommandComposer
└── ConsoleHistoryPane(width 272dp)
```

- 页头：`Command console` + `host:port / dbN` + 右侧 RTT 徽标、导出（可暂不实现）、帮助。
- Terminal 头部：三个小圆点（纯装饰，可用颜色 token；不要模拟窗口按钮行为）、`redis-cli — connectionName`、db 徽标、复制/清空按钮。
- Terminal 背景 `terminalBackground`，边框 border，圆角 13dp，深阴影；浅色主题下仍是深色。
- 输入 composer：独立卡片，圆角 11dp，accent 聚焦环；提示符 + 输入 + `⌘↵ 执行` 提示 + 运行按钮。
- QuickCommands：常用命令 chips（INFO memory / DBSIZE / SLOWLOG GET 10 / SCAN 0 COUNT 100 / MEMORY DOCTOR）；点击只**填充并聚焦**输入框，不直接执行；运行按钮或 Enter 执行。
- 右侧 272dp：
  - 历史记录（时间、命令、耗时/结果摘要）；
  - 收藏的命令（静态常量列表 + 可选新增）；
  - 提示卡（↑↓、⌘K、自动补全）。
- 空/初始状态：提示“输入命令开始”与快捷 chips；不要伪造历史。

### 5.2 补全

- 输入 >= 1 字符时，根据 `RedisCommandCatalog` 前缀匹配显示建议（最多 6 条）。
- 每项：命令名 + 语法提示。
- 点击建议或按 Tab 将命令名/前缀填入输入框并聚焦；Enter 始终执行输入框中的完整命令。
- 键盘：
  - Enter：执行；
  - ⌘↵ / Ctrl+↵：执行（同一行为，兼容设计）；
  - ↑↓：历史（补全弹层未打开时）；
  - Tab：补全；
  - Esc：关闭补全。
- 补全数据只包含常用命令，不假装完整 Redis 文档。

### 5.3 只读拦截

`WriteCommandGuard.isWrite(command: String): Boolean` 必须准确，至少包含：

`SET, SETEX, PSETEX, SETNX, GETSET, GETDEL, GETEX, APPEND, SETRANGE, MSET, MSETNX, INCR, INCRBY, INCRBYFLOAT, DECR, DECRBY, DEL, UNLINK, EXPIRE, EXPIREAT, PEXPIRE, PEXPIREAT, PERSIST, RENAME, RENAMENX, MOVE, COPY, RESTORE, HSET, HSETNX, HDEL, HINCRBY, HINCRBYFLOAT, HMSET, LPUSH, LPUSHX, RPUSH, RPUSHX, LPOP, RPOP, LSET, LREM, LTRIM, LINSERT, SADD, SREM, SPOP, SMOVE, SDIFFSTORE, SINTERSTORE, SUNIONSTORE, ZADD, ZREM, ZINCRBY, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZREMRANGEBYLEX, FLUSHDB, FLUSHALL, CONFIG SET, CONFIG RESETSTAT, CLIENT KILL, CLIENT PAUSE, SCRIPT LOAD, SCRIPT FLUSH, EVAL, EVALSHA, MULTI, EXEC, DISCARD`

- `DUMP`、`PUBLISH`、`CLIENT LIST`、`SELECT` 不按数据写入处理，可在只读模式执行；`PUBLISH` 的副作用在提示中说明。

- 多词命令（`CONFIG SET`）要判断前两个 token。
- 读命令：GET/HGETALL/SCAN/INFO/TTL/MEMORY/OBJECT/SLOWLOG/CLIENT LIST/PING/SELECT 等。
- 只读模式下，未知命令一律拦截，并提示“未知命令在只读模式下被拦截”；安全优先于便利。
- UI 上的运行按钮不禁用（允许读命令），只在执行时拦截写命令并显示错误条目。

### 5.4 复制 / 导出

- 复制全部日志到剪贴板（Phase 6 必须）。
- 导出为文件：可选。若实现，使用 JVM `FileDialog` 或注入 `TextExporter`；不要用 common 代码直接操作 `java.io`。v1 可暂不实现，按钮隐藏/禁用并在 `STATUS.md` 标注。

---

## 6. 生命周期与性能

- ConsoleState 随 tab 创建/关闭；关闭取消 scope。
- 大量输出：只保留最近 500 条 entry，超出时从头部移除并提示“已截断更早的输出”（或保留但限制单条数组项数）。
- 自动滚动：默认滚到底部；用户手动上滚后暂停自动滚动，回到底部恢复。可用 `LazyListState` 是否接近底部判断。
- 执行中显示运行按钮 loading；允许继续输入但不要并行执行同一条命令（isRunning）。
- 命令错误是正常结果，不弹 toast；连接异常才 toast 并在 entry 中显示错误。

---

## 7. 非目标

- 不实现完整 Redis 文档/参数补全。
- 不实现 Pub/Sub 订阅面板、Monitor。
- 不实现多行命令编辑（v1 单行 + 引号解析）。
- 不实现导出文件（可选，见 5.4）。
- 不实现命令收藏持久化（v1 静态/内存）。

---

## 8. 验收

```bash
./gradlew :app:jvmTest
./gradlew ktlintCheck
./gradlew build
./gradlew :desktopApp:run
```

- [ ] 输入 PING / DBSIZE / INFO memory / SCAN / HGETALL 得到真实结果。
- [ ] 嵌套数组（SLOWLOG GET）结构保留，缩进正确。
- [ ] 命令错误显示为红色 `(error)` 条目，不弹 toast。
- [ ] 只读模式下 SET/DEL/FLUSHDB 被拦截；GET/INFO/SCAN 正常。
- [ ] ↑↓ 历史、点击历史填充、⌘↵ 执行、Tab 补全可用。
- [ ] 清空/复制日志可用；历史不被清空。
- [ ] 关闭再打开 tab 状态独立；关闭后无残留轮询/job。
- [ ] 大量输出不冻结 UI；自动滚动行为正确。
- [ ] 深色/浅色主题下终端都保持可读的深色底。
- [ ] 更新 `STATUS.md`。

---

## 9. STOP

停止。Phase 7 实现洞察仪表盘。
