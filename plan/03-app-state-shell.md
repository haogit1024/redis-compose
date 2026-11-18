# Phase 3 — AppState、会话与外壳

> **目标**：把设计中的应用外壳跑起来：顶栏、图标导航、连接侧栏、工作区 Tab、状态栏、页面路由，以及真实连接状态。页面内容先用占位，Phase 4+ 填充。
>
> 依赖：Phase 1（组件）、Phase 2（数据层）  
> 产出：`AppState`、`RedisSession`、可导航外壳、基础命令面板  
> 下一阶段：Phase 4（Key 浏览器）

---

## 1. 开始前

- 阅读 `plan/reference/domain-and-data.md` §8（Container / AppState / RedisSession）。
- 阅读 `plan/reference/design-tokens.md` §3、§8（布局细节）。
- 对照 `design/workspace.html` 的外壳部分：`.titlebar`、`.rail`、`.sidebar`、`.tabbar`、`.statusbar`。
- 本阶段**不实现** Key 列表、值编辑器、控制台内容、洞察图表、连接表单、设置表单。

---

## 2. 文件清单

```text
app/src/commonMain/kotlin/com/czh/rediscompose/app/
├── RedisComposeApp.kt          # 根组合，创建 AppState / 主题
├── AppContainer.kt             # 接口 + 初始数据（若 Phase 2 未放这里）
├── AppState.kt                 # 全局状态与动作
├── AppPage.kt                  # AppPage / WorkspaceTabKind / WorkspaceTab
├── RedisSession.kt             # 会话状态与连接流程
├── ToastMessage.kt             # ToastKind / ToastMessage / ToastController
└── CommandPaletteItem.kt       # 若放在 shell 也可

app/src/commonMain/kotlin/com/czh/rediscompose/feature/shell/
├── AppShell.kt                 # 外壳布局
├── AppTopBar.kt
├── AppRail.kt
├── ConnectionSidebar.kt
├── WorkspaceTabBar.kt
├── AppStatusBar.kt
├── CommandPalette.kt           # 基础版（导航 + 连接）
└── PlaceholderContent.kt       # 各页面临时占位
```

`desktopApp/src/main/kotlin/com/czh/rediscompose/desktop/Main.kt` 修改为创建 container 并传入 `RedisComposeApp(container)`。

---

## 3. AppState 与会话

### 3.1 RedisComposeApp

```kotlin
@Composable
fun RedisComposeApp(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val systemDark = isSystemInDarkTheme()
    val state = remember(container) {
        AppState(
            container = container,
            scope = scope,
            initialSettings = container.initialSettings,
            initialConnections = container.initialConnections,
        )
    }
    val dark = when (state.settings.theme) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    AppTheme(dark = dark, accent = state.settings.accent) {
        DisposableEffect(state) { onDispose { state.close() } }
        AppShell(state)
    }
}
```

- `Main.kt`：
  ```kotlin
  val container = remember { createDesktopAppContainer() }
  RedisComposeApp(container)
  ```
- 不要在 common 里 import JVM 容器；desktopApp 负责创建。

### 3.2 AppState 行为

按 reference 实现，并补充：

- `openConnection(id)`：
  - 设置 `activeConnectionId`；
  - 若 session 不存在则创建；
  - 启动 `session.connect()`；
  - 打开/聚焦该连接的 KEYS tab；
  - `activePage = WORKSPACE`。
- `openTab(connectionId, kind)`：
  - tab id 复用；
  - 确保 session 存在并连接；
  - 设置 activeTabId、activePage=WORKSPACE。
- `closeTab`：按设计选择相邻 tab。
- `selectDatabase`：校验 session 已连接，调用 gateway select，更新 currentDatabase；后续 feature state 刷新（Phase 4+）。
- `updateSettings`：
  - 更新内存；
  - `scope.launch { container.settingsRepository.save(settings) }`（Phase 3 可以先直接保存，无需 debounce）；
  - 主题/强调色立即生效。
- `saveConnection` / `deleteConnection`：
  - Phase 8 才做完整 UI，但 AppState 先支持持久化与级联关闭；
  - 删除时关闭 session + 该连接 tabs；
  - 若 activeConnection 被删，选择剩余第一个或 null。
- `showToast`：最多保留 3 条，3 秒后自动移除；`ToastController` 可复用。
- `close()`：关闭所有 gateway；取消 scope（scope 由组合根持有，`close` 只负责 gateway）。
- 命令面板状态：`commandPaletteVisible`，基础版先做导航。

### 3.3 RedisSession

按 reference 实现。额外要求：

- `connect()` 可重入：CONNECTING 时忽略重复调用。
- `latencyMillis` 用 `TimeSource.Monotonic` 或 `System.nanoTime` 测 ping。
- ERROR 状态保留首行错误，详细堆栈只 `println`。
- `refreshDatabases()` 解析 `INFO keyspace` → 固定 16 个 DB（与现有行为一致）；保留已存在的 DB 计数。
- `refreshSummary()` 解析 `INFO` 为 `ServerSnapshot`，更新侧栏/状态栏。
- `disconnect()` 幂等；取消会话 scope；清空 summary/latency。
- 会话作用域：使用 `SupervisorJob(scope.coroutineContext.job)` 或记录 feature jobs；确保断开/关闭 tab 时取消。

---

## 4. 外壳 UI

### 4.1 结构

```text
Column(fillMaxSize)
├── AppTopBar (54dp)
├── Row(weight 1)
│   ├── AppRail (60dp)
│   ├── ConnectionSidebar (258dp)   // 仅 Workspace 页面显示
│   └── Column(main)
│       ├── WorkspaceTabBar (46dp)  // 仅 Workspace
│       └── Box(weight 1) { content }
└── AppStatusBar (30dp)
```

- Connections / Settings 页面不显示连接侧栏与 Tab 栏（与设计一致）；只保留 Rail + 内容。
- 内容区背景 `colors.background`；侧栏/顶栏背景按 token。
- 窗口窄于 1120dp 时保持最小尺寸；不实现移动端折叠。

### 4.2 AppTopBar

- 左侧：`BrandMark` + “redis compose” 字标 + `v0.1` 胶囊。
- 面包屑：environment / active connection name，右侧 chevron；点击可回 Workspace 或打开连接侧栏（简单实现：回 Workspace）。
- 中间：搜索框（最大 400dp），点击打开命令面板，显示 `⌘K`。
- 右侧：状态 pill（已连接 / 连接中 / 未连接 + 延迟）、主题切换、通知（toast“暂无通知”）、头像 `RC`。
- 主题切换：
  - 当前 dark → 保存 `ThemeMode.LIGHT`；
  - 当前 light → 保存 `ThemeMode.DARK`；
  - 使用 AppState.updateSettings 持久化。

### 4.3 AppRail

- 图标：Keys、Console、Insights、分隔线、Connections、Settings；底部状态点 + 键盘图标。
- 激活态：38×38dp 圆角 11dp，accentSoft 背景 + 左侧 2dp accent 条。
- 点击 Keys/Console/Insights：
  - 若无 activeConnection，跳转 Connections 并 toast “请先选择一个连接”；
  - 否则打开对应 tab 并切换 activePage=Workspace。
- 点击 Connections / Settings：切换 activePage；关闭命令面板。
- Tooltip 必须配置。

### 4.4 ConnectionSidebar

- 头部：标题“连接”、数量、+ 按钮（跳 Connections 页面）。
- 搜索框：按名称/Host 过滤连接。
- 分组：按 `ConnectionEnvironment`（Production / Staging / Local / Other）显示分组标签；组内按最近打开或名称排序。
- 连接卡：
  - 27dp 色块（ConnectionColor 映射到颜色）+ 名称 + 环境小标签；
  - host:port Mono 10sp；
  - 状态：已连接显示 mint 点和延迟；连接中显示 warning；错误显示 danger + 错误首行；
  - 已连接时展示版本 / clients（来自 summary），未连接显示“—”。
- 选中连接展开 DB 列表（最多显示 16 个，滚动）。DB 行显示 `dbN` + key 数量；当前 DB 高亮。
- 点击连接：`state.openConnection(id)`。
- 点击 DB：`state.selectDatabase(id, index)`。
- 底部健康卡：
  - 已连接：内存使用条（used/max 或 “—”）、百分比、内存值；下方洞察/控制台按钮。
  - 未连接：显示“未连接”与提示。
- 侧栏可滚动，连接很多时不撑破外壳。

### 4.5 WorkspaceTabBar

- 仅 Workspace 页面显示。
- 左侧 tabs：每个 tab 30dp 高胶囊；图标按 kind；激活态 surface3 + accent 图标；hover 显示关闭按钮。
- 加号按钮：跳转 Connections 页面（v1 简化）。
- 右侧：
  - `db` pill：点击弹出菜单切换当前连接 DB（仅当前 session 已连接可用）；
  - “可写 / 只读”徽标；
  - 刷新（Phase 4 接入真实刷新；本阶段 toast“已刷新”或禁用）；
  - 更多（菜单占位）。
- 无 tab 时不显示 db/状态操作，只显示空工作区；有 tab 时才显示 db pill 与读写状态。

### 4.6 内容路由

```kotlin
when (state.activePage) {
    AppPage.WORKSPACE -> {
        val tab = state.activeTab()
        if (tab == null) EmptyWorkspace(state) else when (tab.kind) {
            KEYS -> WorkspacePlaceholder(state, tab)
            CONSOLE -> ConsolePlaceholder(state, tab)
            INSIGHTS -> InsightsPlaceholder(state, tab)
        }
    }
    AppPage.CONNECTIONS -> ConnectionsPlaceholder(state)
    AppPage.SETTINGS -> SettingsPlaceholder(state)
}
```

- 占位页也要有正确背景/内边距/标题，便于 Phase 4+ 替换。
- 空工作区：品牌标记 + “从左侧选择一个连接开始” + 若没有连接则“新建连接”按钮跳 Connections。
- 无连接时 Connections 占位页显示空状态 + “Phase 8 将提供表单”不必写在 UI；显示“添加你的第一个 Redis 连接”即可。

### 4.7 AppStatusBar

- 左：连接状态点 + 文本；host:port；db；keys 数；内存；clients；RTT。
- 右：UTF-8、Redis 版本、Redis Compose 0.1.0。
- 无连接：显示“未连接”、app 版本；不要显示假数据。
- 字号 9.5sp Mono；高度 30dp。

---

## 5. 基础命令面板

本阶段先实现**导航型**命令面板，Phase 9 再扩展操作与快捷键矩阵。

- 触发：⌘K / Ctrl+K、点击顶栏搜索框。
- Overlay：遮罩 + 600dp 面板（最大视口 − 32dp），圆角 15dp，背景 surface。
- 内容：搜索输入、分组列表（页面 / 连接）、底部提示（↑↓ / Enter / Esc）。
- 页面项：工作区、控制台、洞察、连接库、设置。无 activeConnection 时控制台/洞察置灰或点击提示。
- 连接项：所有连接，点击后 `openConnection`。
- 键盘：↑↓ 选择，Enter 执行，Esc 关闭；输入过滤。
- 视觉参考：`design/assets/theme.css` 的 `.palette*` 与 `design/index.html` 中的命令面板样式（HTML 里只有样式，没有展开状态，可按 token 实现）。
- 不在本阶段实现“新建 Key / 新建连接 / 切换主题”等操作项。

---

## 6. 非目标

- 不实现 Key 树、值编辑器、真实控制台、图表、连接表单、设置表单。
- 不实现 SSH 隧道/完整 TLS（沿用 Phase 2 边界）。
- 不实现指标轮询（Phase 7）。
- 不实现命令面板操作项（Phase 9）。
- 不实现自定义窗口装饰。

---

## 7. 验收

```bash
./gradlew :app:jvmTest
./gradlew ktlintCheck
./gradlew build
./gradlew :desktopApp:run
```

- [ ] 应用启动直接进入外壳（不再显示画廊）。
- [ ] 1440×900 下顶栏/rail/侧栏/tab/状态栏比例与设计一致。
- [ ] 深色/浅色切换立即生效并持久化；重启后保持。
- [ ] 已有连接显示在侧栏；没有连接时显示完整空状态。
- [ ] 点击连接 → 连接状态从 CONNECTING → CONNECTED；DB 列表出现；延迟/版本/内存有值。
- [ ] 连接错误时侧栏显示错误首行，应用不崩溃，可重试。
- [ ] 点击 rail 的 Keys/Console/Insights 能打开/复用 tab；tab 可关闭。
- [ ] 切换 DB 会更新会话与状态栏。
- [ ] 命令面板可用：⌘K 打开、搜索过滤、Enter 跳转、Esc 关闭。
- [ ] 无假数据：未连接状态不显示伪造的延迟/内存/Key 数。
- [ ] 状态栏在无连接/已连接两种状态下都完整。
- [ ] 所有颜色/字号/尺寸来自 token；有 `@Preview`（外壳至少一个）。
- [ ] 更新 `plan/STATUS.md`。

---

## 8. STOP

停止。Phase 4 实现 Key 浏览器，用真实 SCAN 数据替换 Workspace 占位。
