# 阶段状态

> 每个 Agent 完成一个阶段后更新本文件。**未完成的阶段不要标记完成。**
>
> 状态取值：`未开始` / `进行中` / `阻塞` / `已验收`

| 阶段 | 状态 | 完成日期 | 完成内容 / 验证 | 遗留风险 |
| --- | --- | --- | --- | --- |
| 0 — 重置脚手架 | 已验收 | 2026-11-18 | 见下文 §Phase 0 | 无阻塞项 |
| 1 — 设计系统 | 已验收 | 2026-11-18 | 见下文 §Phase 1 | 见“遗留风险” |
| 2 — 领域与数据层 | 已验收 | 2026-11-18 | 见下文 §Phase 2 | 见“遗留风险” |
| 3 — AppState 与外壳 | 已验收 | 2026-11-18 | 见下文 §Phase 3 | 见“遗留风险” |
| 4 — Key 浏览器 | 已验收 | 2026-11-18 | 见下文 §Phase 4 | 见“遗留风险” |
| 5 — 值编辑器 | 未开始 | — | — | — |
| 6 — 控制台 | 未开始 | — | — | — |
| 7 — 洞察 | 未开始 | — | — | — |
| 8 — 连接库 | 未开始 | — | — | — |
| 9 — 设置 + 命令面板 | 未开始 | — | — | — |
| 10 — 打磨与发布 | 未开始 | — | — | — |

## 阶段门禁

- Phase 3 开始前：Phase 1、2 必须 `已验收`。
- Phase 5 开始前：Phase 4 必须 `已验收`。
- Phase 9 开始前：Phase 5、8 必须 `已验收`。
- Phase 10 开始前：其余全部 `已验收`。

---

## Phase 0 — 重置脚手架（已验收）

分支：`feat/rewrite-ui`（从 `main` HEAD 创建）。

### 完成内容

- `shared/` → `app/`（`git mv`）；`settings.gradle.kts` 只包含 `:desktopApp`、`:app`。
- 包名统一为 `com.czh.rediscompose`；桌面入口 `com.czh.rediscompose.desktop.MainKt`。
- 删除旧 `com.czh.redis` 的 UI / 数据 / Redis 客户端 / 测试代码，以及旧 `Platform.kt` / expect-actual 与 `compose-multiplatform.xml`。
- 新包骨架：`app/{app,design,domain,data,feature,util}`；`app` 与 `desktopApp` 的 jvm/common test 目录已建。
- 新空壳 `RedisComposeApp`（深色居中占位）；窗口 1440×900，最小 1120×720，标题 “Redis Compose”。
- `desktopApp/build.gradle.kts`：`implementation(projects.app)`、`mainClass`、`packageName`。
- 更新 `AGENTS.md`（模块名、入口、测试命令、资源路径）与 `README.md`。

### 验证

| 命令 | 结果 |
| --- | --- |
| `./gradlew build` | 通过 |
| `./gradlew ktlintCheck` | 通过 |
| `./gradlew :desktopApp:run` | 启动成功（`com.czh.rediscompose.desktop.MainKt` 进程存活，无异常） |
| `rg "com\\.czh\\.redis\\b" app desktopApp` | 无结果 |
| `settings.gradle.kts` / 代码中的 `:shared` 引用 | 无结果（`plan/` 内文档说明除外） |

---

## Phase 1 — 设计系统与组件画廊（已验收）

### 完成内容

- `domain/settings/ThemePreferences.kt`：`ThemeMode` / `Accent` / `UiDensity`（供 Phase 2 的 `AppSettings` 复用）。
- `design/theme/`：
  - `AppColors.kt`：深/浅两套完整 token、5 种强调色派生 base/strong/ink/soft/softer/line、类型色、JSON 高亮色、scrim。
  - `AppTypography.kt`：Inter 400/500/600 + JetBrains Mono 400/500，内置 20 个语义文本样式。
  - `AppDimens.kt`：间距、圆角、布局尺寸、行高、控件/图标尺寸；`forDensity(COMFORTABLE/COMPACT)`。
  - `AppTheme.kt`：`LocalAppColors` / `LocalAppDimens` / `LocalAppTypography`，并映射 Material3 colorScheme / typography / shapes，避免 Material 默认紫色。
  - `AppShapes.kt`：由 `AppDimens` 生成 Material3 `Shapes`。
- `design/icon/AppIcons.kt`：Material Icons **Outlined** 语义映射（60+ 图标，含 `AppIcons.Settings/Terminal/Insights/Connections`）；`BrandMark` 自绘品牌 Logo。
- `design/component/`：`AppButton`、`AppIconButton`、`AppTextField`、`AppSearchField`、`AppBadge`+`AppTypeChip`+`AppTypeDot`、`AppSwitch`、`AppSegmented`、`AppKbd`、`AppCard`、`AppMenu`、`AppDialog`、`AppSheet`、`AppTooltip`、`AppEmptyState`、`AppLoading`、`AppToast`（`ToastData` + host）、`AppScrollbar`。
- 每个组件文件底部有 `@Preview`（包 `AppTheme`），多数提供深色预览，关键组件提供浅色预览。
- `design/gallery/DesignGalleryScreen.kt`：颜色、字体、按钮、输入、徽章/类型/快捷键、分段/开关/提示、卡片/空状态/加载、图标、浮层（Dialog/Sheet/Menu/Toast）区块；顶栏可切换深/浅与 5 种强调色。
- `RedisComposeApp` 临时启动到画廊，默认深色 + IRIS；主题状态用 `remember`，不依赖 domain/data。
- 资源与依赖：
  - `material-icons-extended` 已加入；`compose.resources` 生成包固定为 `com.czh.rediscompose.resources`。
  - 字体已打包：`inter_regular/medium/semibold.ttf`、`jetbrains_mono_regular/medium.ttf`（Inter 由官方可变字体实例化 400/500/600；JetBrains Mono 官方静态字体）。

### 验证

| 命令 | 结果 |
| --- | --- |
| `./gradlew build` | 通过 |
| `./gradlew ktlintCheck` | 通过（含 `ktlintFormat` 后无违规） |
| `./gradlew :desktopApp:run` | 启动成功，进入组件画廊；进程存活 50s 无异常输出 |
| 离屏渲染（ImageComposeScene，临时工具，未提交） | 1440×900 深/浅、1120×720、完整长页、Dialog、Sheet、Toast 均成功出图并逐项目检 |
| `rg "Color\\(0x" app/src` | 仅 `design/theme/AppColors.kt` 与 `design/icon/BrandMark.kt`（允许项） |
| `rg "Icons\\.Outlined"`（`AppIcons.kt` 以外） | 无结果 |

视觉自检结论：

- 深色默认、浅色切换后无残留深色块；浅色主按钮使用深色 `accentInk`，强调色切换后派生色同步变化。
- 1120×720 下画廊无横向溢出，顶栏/分区可用；滚动条为 9dp 自绘，thumb 悬停变亮，可拖拽。
- 输入框 focus 环绘制在控件外，不改变布局尺寸；error/disabled 状态可见。
- 六种类型色、状态 badge、kbd、分段、开关、空状态、加载均正常。
- Dialog/Sheet 支持 Esc 与遮罩关闭；Toast 3 秒自动消失，最多同时 3 条。

### 与设计稿的差异 / 遗留风险

1. **图标库版本**：JetBrains 的 `material-icons-extended` 最后发布版本是 `1.7.3`，因此固定在 `1.7.3` 与 Compose `1.11.1` 搭配。编译/运行正常；未引入第三方图标库。个别 Outlined 名称不存在（如 `LockOutline`）已映射为最接近的 `Lock`。
2. **字体**：Inter 使用官方可变字体实例化出 400/500/600，JetBrains Mono 使用官方 400/500；均离线打包，无运行时下载。Inter 不含 CJK 字形，中文由 Skia 系统字体回退（macOS 为 PingFang SC），这是预期行为；若后续要完全统一字形，需要再打包 CJK 字体（Phase 10 评估）。
3. **Material Icons 形状**：与设计稿的自绘线性 SVG 不完全一致（如 Insights/Connections），属于 `acceptance.md` 允许的差异。
4. **视觉验收方式**：Agent 环境没有屏幕录制权限，无法 `screencapture`；改为离屏 `ImageComposeScene` 渲染 + 真实 `:desktopApp:run` 启动验证。悬停/按压/焦点等瞬时状态未做自动化截图，仅做代码级检查。
5. **浮层细节**：`AppMenu` 与 `AppTooltip` 使用 `Popup`，离屏截图不渲染弹层，未做像素级截图；Dialog/Sheet 的 `18/17/14dp` 内边距直接对齐设计 CSS，尚未全部收敛进 `AppDimens`。
6. **浅色遮罩**：浅色 scrim 使用 46% 而非深色 56%，避免浅色页面整体过暗；属有意偏离，Phase 9 可再校准。
7. **密度**：`COMPACT` 尺寸已实现，但 Phase 1 未接入设置/持久化（按计划留给 Phase 9）。
8. **未使用依赖**：`jedis`、`kotlinx-serialization`、lifecycle 依赖仍在（Phase 0 要求保留），将在 Phase 2/3 使用；Phase 10 做依赖清理。
9. `DesignGalleryScreen` 是临时入口，Phase 3 替换为真正外壳后保留为开发者页面或 Phase 10 删除。
10. 工作区中同时存在 `domain/key/KeyModels.kt`（`KeyType` 枚举，标注 `TODO(phase-2)`）：Phase 1 UI 未引用它，本阶段未扩展其语义，留给 Phase 2。

---

## Phase 2 — 领域与数据层（已验收）

### 完成内容

#### 领域模型（commonMain / `domain/`）

- `connection/ConnectionProfile.kt`：`ConnectionProfile`、`TlsConfig`、`SshConfig`、`ConnectionEnvironment`、`ConnectionColor`、`TlsVerifyMode`、`SshAuthMethod`，全部 `@Serializable` 且带兼容默认值。
- `connection/ConnectionValidation.kt`：`ConnectionField`、`ConnectionValidation`、`validateConnection`（逐字段中文错误，不抛异常）。
- `connection/ConnectionRepository.kt`、`settings/SettingsRepository.kt`、`repository/PasswordCipher.kt`。
- `key/KeyModels.kt`：`KeyType`、`ScanPage`、`DatabaseInfo`、`KeySummary`、`KeyMetadata`、`HashField`、`SortedSetEntry`、`StreamEntry`、`KeyValue`（6 种 + sealed）、`KeyDetail`。
- `key/KeyNode.kt`、`key/KeyTreeBuilder.kt`：目录优先、大小写不敏感排序、目录计数、空分隔符平铺。
- `command/`：`RedisReply`（Simple/Error/Integer/Bulk/Array/Nil）、`CommandStat`、`SlowLogEntry`、`ClientConnection`。
- `server/`：`ServerSnapshot`、`MetricsSample`。
- `settings/ThemePreferences.kt`（Phase 1 已建）补充 `@Serializable`；`settings/AppSettings.kt`：`AppSettings`、`KeyView`、`ValueFormat`。

#### 数据层（commonMain + jvmMain）

- `data/redis/RedisGateway.kt`：完整接口 + `HashFieldBytes` / `SortedSetEntryBytes` / `StreamEntryBytes`；所有方法已实现。
- `data/redis/RedisGatewayFactory.kt`、`RedisException.kt`。
- `data/redis/ValueCodec.kt`：严格 UTF-8 校验、HEX（空格分隔，支持 `0x`）、JSON 检测与 pretty。
- `data/redis/InfoParser.kt`：`parse` / `snapshot` / `parseKeyspace` / `commandStats`；缺字段为 null，0 保留为有效值。
- `data/redis/RedisReplyParser.kt`：Jedis 原始 reply → 递归 `RedisReply`，保留嵌套。
- `data/redis/SlowLogParser.kt`、`ClientListParser.kt`、`StreamParser.kt`。
- `util/Formatters.kt`：TTL / bytes / count / duration；`formatEpochSeconds` 使用 expect/actual 取 JVM 本地时区。
- `jvmMain/data/jedis/JedisRedisGateway.kt`：
  - `Dispatchers.IO` + `Mutex` 串行化；懒连接；超时 / ACL 用户密码 / 默认 DB。
  - 全部读写、TTL、内存、编码、删除/重命名/过期、INFO/SLOWLOG/CLIENT LIST/commandstats、递归 `execute`。
  - Stream 使用二进制 `XRANGE` + `XINFO GROUPS`，字段保持字节。
  - SSH：`ssh.enabled` 时抛 `RedisException("SSH 隧道尚未启用，将在 Phase 8 支持")`，不会静默直连。
  - TLS：系统信任链可用；自定义 CA PEM 可用；客户端证书/私钥直接报错（不降级明文），留 Phase 8。
- `jvmMain/data/storage/`：
  - `FilePaths`：`~/.redis-compose/{settings.json,connections.json,secret.key}`，baseDir 可注入测试。
  - `JsonSettingsRepository` / `JsonConnectionRepository`：`suspend` + `Dispatchers.IO`、临时文件 + 原子 move、损坏/缺失回退默认、`ignoreUnknownKeys`。
  - `AesGcmPasswordCipher`：AES-256-GCM、12 字节 IV、`enc:v1:` 前缀、32 字节 `secret.key`、POSIX 0600；密文损坏返回空、密钥不可用回退明文。
  - `connections.json` 保持裸数组，旧明文密码可直接读入。
- `jvmMain/DesktopAppContainer.kt`：`createDesktopAppContainer()` 同步读取初始 settings/connections（文件小），组装 repository / cipher / gateway factory。
- `app/AppContainer.kt`：组合根接口，供 Phase 3 创建 `AppState`。

#### 测试

14 个测试类、75 个用例全部通过：

- `InfoParserTest`：分段、冒号值、完整/缺字段/0 值、keyspace、commandstats。
- `KeyTreeBuilderTest`：多层、空、单 Key、空分隔符、排序、去重计数。
- `FormattersTest` / `FormattersDesktopTest`：TTL 边界、bytes、count、duration、本地时间。
- `ValueCodecTest`：UTF-8、非法/过长序列、HEX、JSON。
- `RedisReplyParserTest`、`SlowLogParserTest`、`ClientListParserTest`、`StreamParserTest`。
- `ConnectionValidationTest`：必填、端口、DB、TLS 路径、SSH 条件。
- `SettingsSerializationTest`：默认值、未知字段、枚举、旧连接 JSON 兼容。
- `AesGcmPasswordCipherTest`：round-trip、随机 IV、旧明文、错误密文、密钥复用/损坏。
- `StorageRepositoriesTest`：缺失/损坏回退、原子写、密码加密落盘、旧裸数组兼容。
- `JedisRedisGatewaySafetyTest`：SSH 与客户端证书快速失败。

### 验证

| 命令 | 结果 |
| --- | --- |
| `./gradlew :app:jvmTest` | 75 tests，0 failures |
| `./gradlew ktlintCheck` | 通过 |
| `./gradlew clean build` | 通过 |
| `./gradlew :desktopApp:run` | 启动成功，仍进入 Phase 1 画廊，无 UI 回归 |

**真实 Redis 手动验收未执行**：当前环境没有 `redis-server` / `redis-cli`，也没有 Docker/Podman，无法启动本地实例。已用真实格式的 INFO / SLOWLOG / CLIENT LIST / XRANGE 样例覆盖解析器，并用 `JedisRedisGatewaySafetyTest` 验证失败路径；真实网络路径留待 Phase 3 联调或用户本地 Redis 验证。

### 实现边界与遗留风险

1. **SSH**：仅做显式拒绝，未实现隧道（按计划 Phase 8）；启用 SSH 的连接在首次操作时报明确错误。
2. **TLS**：
   - 系统信任链默认可用；`caPath` 支持 PEM（替换系统信任，而非叠加）。
   - 客户端证书/私钥在 Phase 2 直接报错，不会降级成明文。
   - `ALLOW_SELF_SIGNED` / `NONE` 在无自定义 CA 时使用 trust-all；`NONE` 额外关闭 hostname 校验。属于用户显式选择的不安全模式，Phase 8 可细化。
3. **`AppSettings.encryptPasswords = false`**：尚未接入（`TODO(phase-9)`），当前连接密码始终加密；默认值 `true` 与验收一致。
4. **`formatEpochSeconds`**：common 声明 + jvm actual（`java.time` 本地时区），是唯一新增的 expect/actual；其余格式化逻辑跨平台纯函数。
5. **Gateway 集成测试**：没有真实 Redis 或 fake server，jedis 命令映射只做了编译与失败路径验证；Phase 3 首次连接时需人工跑 `ping → scan → 6 类型读写 → TTL → 删除`。
6. **启动读取**：`createDesktopAppContainer` 用 `runBlocking(Dispatchers.IO)` 同步读取小文件（计划允许），后续设置写入 debounce 留给 Phase 9。
7. **旧数据**：Phase 3 已补充 pre-rewrite `settings.json` / `connections.json` 迁移（字符串端口、扁平 SSH/TLS 字段、lowercase theme）。

---

## Phase 3 — AppState、会话与外壳（已验收）

### 完成内容

#### 状态与容器

- `app/AppPage.kt`：`AppPage`、`WorkspaceTabKind`、`WorkspaceTab`（tab id 复用规则）。
- `app/RedisSession.kt`：连接生命周期（DISCONNECTED / CONNECTING / CONNECTED / ERROR）、ping 延迟、固定 16 个 DB、`INFO` 摘要、切换 DB、幂等断开、会话作用域。
- `app/AppState.kt`：settings / connections / sessions / tabs / active page/tab/connection / 命令面板 / toast；连接、tab 复用与关闭、DB 切换、主题与设置持久化、连接增删级联关闭。
- `app/ToastMessage.kt`：`ToastMessage` + `ToastController`（最多 3 条、3 秒自动消失）。
- `app/RedisComposeApp.kt` 接收 `AppContainer`，按 `ThemeMode` 解析系统/深/浅主题，创建 `AppState` 并在 dispose 时关闭会话。
- `desktopApp/Main.kt` 创建 `createDesktopAppContainer()` 并传入根组合。
- **旧数据迁移**：`JsonSettingsRepository` 识别 pre-rewrite 小写 theme 与旧字段名；`JsonConnectionRepository` 识别字符串 `port`、扁平 `ssh`/`ssl` 字段、`defaultDb`/`separator`/`readonly` 等，映射到新模型。真实用户连接文件可直接加载。

#### 外壳 UI（`feature/shell/`）

- `AppShell.kt`：54dp 顶栏 + Row(rail / sidebar / main) + 30dp 状态栏；Connections/Settings 页隐藏侧栏与 tab 栏；根级 ⌘K/Ctrl+K、Esc；toast 位于状态栏之上。
- `AppTopBar.kt`：品牌 + v0.1、环境/连接面包屑、点击式全局搜索（⌘K）、连接状态 pill（真实延迟/状态）、主题切换、通知、头像。
- `AppRail.kt`：Keys/Console/Insights、Connections/Settings、底部在线点与快捷键；激活态 38dp、accentSoft、左侧 2dp 条；全部有 tooltip。
- `ConnectionSidebar.kt`：连接数量/搜索/环境分组（最近打开→名称）、连接卡（颜色、名称、环境、host:port、真实状态/版本/clients）、展开 DB 列表（16 个、限高滚动）、底部 Server Health（真实内存条/百分比）+ 洞察/控制台入口；空侧栏状态。
- `WorkspaceTabBar.kt`：tab 胶囊、hover 关闭、加号、db 切换菜单、只读/可写徽标、刷新与更多菜单（刷新为占位提示）。
- `AppStatusBar.kt`：真实连接状态、host:port、db、keys、内存、clients、RTT、Redis 版本；无连接时只显示“未连接”和应用信息，不显示假数据。
- `CommandPalette.kt`：600dp 导航型命令面板，页面 + 连接分组，输入过滤、↑↓/Enter/Esc、最多 3 条 Toast 不受影响；无连接时控制台/洞察置灰并标注“需要连接”。
- `PlaceholderContent.kt`：空工作区、Keys/Console/Insights 占位页、连接库空状态/已有连接只读列表（可点击打开）、设置页只读摘要。
- `ShellPreviews.kt`：预览专用 container + fake gateway，提供 1440×900 深/浅、1120×720、空状态、已连接状态与各子组件 `@Preview`。
- `AppDimens` 新增 rail / tab / palette / 状态栏 / 面包屑尺寸 token；shell 代码无 `Color(0x...)`、无裸 `sp`。

### 验证

| 命令 | 结果 |
| --- | --- |
| `./gradlew :app:jvmTest` | 89 tests，0 failures（新增 AppState/主题持久化/旧数据迁移用例） |
| `./gradlew ktlintCheck` | 通过 |
| `./gradlew build` | 通过 |
| `./gradlew :desktopApp:run` | 启动直接进入新外壳；真实旧连接文件成功加载并显示在侧栏 |

视觉检查（ImageComposeScene 离屏 + 实际窗口截图）：

- 1440×900 深/浅、1120×720：顶栏 / rail / 侧栏 / tab 栏 / 状态栏比例正常，无溢出。
- 已连接状态：DB 列表、健康卡、状态栏 keys/内存/clients/RTT、tab 与 db pill 均使用会话数据。
- 空状态：无连接 / 无 tab / 空侧栏 / 空命令面板均正确。
- 命令面板：页面与连接分组、选中态、快捷键提示、无连接时禁用项可见。
- 浅色主题下无深色残留块；强调色派生色同步变化。

### 遗留风险与边界

1. **真实 Redis 联调仍未执行**：环境没有 Redis/Docker；连接成功态用 fake gateway（预览/测试）与离屏渲染验证。真实 `ping → scan → 6 类型读写 → TTL → 删除` 需要用户本地 Redis 手动跑一次。
2. **键盘自动化受限**：macOS 未授权 osascript 发送按键，⌘K/↑↓/Enter/Esc 只做了代码级检查与面板静态渲染，未做自动键盘回放。
3. **主题切换的真实写入**：为避免改动用户现有 `settings.json`，验收时没有在真实窗口点击主题按钮；改用 `ThemePersistenceTest` 验证 `updateTheme → 文件 → 重新加载`。
4. **连接库/设置页是占位**：只提供空状态、只读连接列表与设置摘要；新建/编辑表单是 Phase 8，完整设置是 Phase 9。
5. **连接增删**：`AppState.saveConnection/deleteConnection` 已支持持久化与级联关闭，但 Phase 3 没有对应表单 UI。
6. **DB 列表滚动**：选中连接后 DB 列表限高，超出部分在列表内滚动；未再加内层滚动条，避免与侧栏滚动条重叠。
7. **刷新/更多菜单**：刷新与部分操作仍是占位 toast，Phase 4/9 接入真实行为。
8. **启动读取**：容器在首帧前同步读取小文件；设置写入尚未 debounce（Phase 9）。
9. **旧数据迁移**：已覆盖 pre-rewrite 文件格式；更早/损坏文件回退默认值。

---

## Phase 4 — Key 浏览器（已验收）

### 完成内容

#### 状态与生命周期

- `feature/workspace/WorkspaceState.kt`：
  - 真实 `SCAN cursor MATCH pattern COUNT settings.scanBatchSize` 分页加载；无搜索模式时先 `DBSIZE` 取总数；`loadedAll` 由 cursor 归零决定。
  - SCAN 结果去重（Redis SCAN 可能重复返回）；分页失败保留已加载 Key，设置 `loadError` 并发 danger toast，`retry()` 从原 cursor 继续或重新开始。
  - 每次扫描带 generation 编号，`refresh()`/`applySearch()` 取消旧任务并丢弃过期结果，快速输入不会出现旧结果覆盖新结果。
  - 搜索状态 `searchInput` + `appliedPattern`：空值清除模式并全量重扫；非空原样传给 `SCAN MATCH`。
  - 类型筛选 `typeFilter` 仅对已加载 Key 生效；类型未知的 Key 先保留（灰点），TYPE 返回后再按筛选隐藏。
  - 置顶 `pinnedKeys` 为当前 WorkspaceState 内存态，按名称稳定排序，搜索/类型筛选时仍显示。
  - 树状态 `tree`（`KeyTreeBuilder` + profile.keySeparator）、`expandedDirs`；首次加载自动展开深度 0 目录，用户手动收起后刷新不再强制展开；分隔符为空时退化为平铺。
  - `toggleKeyView()` 通过 `AppState.updateSettings` 更新 `settings.keyView`（Phase 9 再决定完整持久化体验）。
  - TYPE/TTL 预览缓存 `keyTypes`/`keyTtls`：每个 Key 只请求一次，`Semaphore(6)` 限并发，失败静默（灰点），刷新时清空。
  - WorkspaceState 使用 session scope 下的独立 `SupervisorJob`；`dispose()` 取消 SCAN 与 TYPE/TTL 任务。关闭 Keys tab、删除连接、退出应用都会 dispose。

#### 组合根与路由

- `app/AppState.kt`：新增 `workspaces` 与 `workspaceFor(connectionId)`；打开 Keys tab 时创建；关闭 Keys tab / 保存或删除连接 / `close()` 时 dispose；切换 DB 成功后 `refresh()`。
- `feature/shell/PlaceholderContent.kt`：`WorkspaceTabKind.KEYS` 路由到真实 `WorkspaceScreen`，删除 `KeysPlaceholder`；Console/Insights/Connections/Settings 占位结构未动。
- `feature/shell/WorkspaceTabBar.kt`：顶部刷新按钮与“更多 → 刷新”调用当前 WorkspaceState.refresh()；非 Keys 页仍提示暂不支持。

#### Key 面板 UI（`feature/workspace/`）

- `WorkspaceScreen.kt`：左 Key 面板 + 4dp 可拖拽分隔线 + 右侧占位；面板 240–420dp，并在窗口缩小时把上限压到“值面板至少 420dp”；宽度 `rememberSaveable`。
- `KeyListPane.kt` / `KeyPaneHeader.kt` / `KeySearchFilter.kt`：
  - 标题 `Keys` + 真实总数（Mono 9.5sp）+ 新建/刷新/视图切换 + 只读锁提示。
  - 30dp 搜索框（Mono、SCAN 模式 placeholder、⌘F 提示、有内容时显示清除按钮）。
  - “全部 + 6 类型” chips，激活时使用对应类型色 12% 背景 / 24% 边框。
- `PinnedKeyList.kt`：顶部“置顶”区展示完整 Key 名，星标可取消置顶，选中态与树/搜索结果同步；无置顶时不占用空间。
- `KeyTree.kt` / `KeyRow.kt`：
  - Tree/Flat 两种视图；目录优先、chevron + folder 图标 + 计数、depth × 14dp 缩进、1dp 竖向引导线。
  - Key 行 28dp：类型点（6dp + 11% 光晕）、Mono 10.5sp 名称、TTL（8.5sp，≤60s 用 danger）、hover/选中/置顶显示星标。
  - 选中行 accentSoft 背景 + 左侧 2dp accent 条；右键菜单仅提供“复制 Key”。
  - 搜索/类型筛选激活时改为平铺结果列表，头部显示“N 个结果”与清除按钮。
  - 接近底部时自动 `loadMore()`；底部提供虚线 “SCAN 加载更多 · cursor N” 行。
  - 空 DB / 无匹配 / 首次加载 / 加载失败（含保留已加载内容的内联错误条）分别有明确状态与重试/清除操作。
- `KeyPaneFooter.kt`：db 名、SCAN 进度条（accent→success 渐变）、`loaded / total`；全部加载完成显示“已加载全部 N 个 Key”。
- `ValuePanePlaceholder.kt`：未选择显示品牌图标与提示；已选择显示真实 Key 名、类型 pill、`正在读取值…` 占位，明确标注值编辑器属于 Phase 5。
- 图标全部走 `AppIcons`；新增 `AppDimens`（Key 面板/树/行/chip/scan bar）与 `AppTypography`（`monoKeyRow`、`monoMicro`、`filterChip`）token；`AppTextField` 增加可选高度参数（默认行为不变）。
- 新增 `horizontalResizeCursor()` expect/actual（common + jvm），分隔线 hover 显示左右缩放光标。

#### 键盘与操作

- ⌘F / Ctrl+F 聚焦搜索；搜索框 Enter 立即应用；Esc 清空搜索 → 取消选中 → 释放焦点。
- R 刷新（搜索框聚焦时不触发）；N toast 提示 Phase 5；右键 Key 复制 Key（写入系统剪贴板并 toast）。

#### 预览

- 新增 `WorkspacePreviews.kt`（预览专用 in-memory gateway + 22 个设计稿 Key），不读取用户连接文件、不连接真实 Redis。
- `WorkspaceScreen`、`KeyListPane`、`KeyPaneHeader`、`KeySearchFilter`、`KeyTree`、`KeyRow`、`PinnedKeyList`、`KeyPaneFooter`、`ValuePanePlaceholder` 均有 `@Preview`（覆盖深/浅、空、搜索、加载、错误、只读、平铺）。

### 真实 Redis 联调（只读）

使用用户提供的测试实例（Redis 8.6.2，仅记录版本，不记录地址/密码）执行了一次临时只读集成测试；测试文件在完成后已删除，凭据只从环境变量读取，未写入项目或 `connections.json`。

- 仅执行只读命令：`PING`、`SELECT`、`DBSIZE`、`SCAN`、`TYPE`、`TTL`、`MEMORY USAGE`、`OBJECT ENCODING`、`INFO`；没有任何写入/删除/过期操作。
- 连接：PING ≈ 69–88 ms；会话 `CONNECTED`，RTT ≈ 11 ms；3 个非空 DB（5 / 35 / 15 Keys）。
- SCAN：选择 35 Key 的 DB，`COUNT 10` 扫描 4 页正好覆盖 35 个 Key，cursor 归零，总耗时 ≈ 30 ms；未发现重复 Key。
- TYPE/TTL 抽样 10 个 Key：`STRING=9`、`ZSET=1`；永久 / 有 TTL 均正确；`MEMORY USAGE` 与 `OBJECT ENCODING` 10/10 成功。
- `SCAN MATCH`：用刚确认存在的精确 Key 作为 pattern，返回 1 条，耗时 ≈ 8 ms。
- WorkspaceState：首屏加载 10 个 Key、`total=35`、`loadedAll=false`；连续 `loadMore()` 后 30 个；`ensurePreview` 写入 TYPE 与 TTL 缓存；精确 Key 搜索命中 1 条；清除搜索与 `refresh()` 均无错误。
- 观察到 Redis `SCAN COUNT` 只是提示值：`COUNT 10` 的首屏实际返回 10–11 个 Key；实现没有假设每页数量固定。
- 未用真实连接做桌面 UI 点击流/截图，避免把服务器 Key 名渲染进截图；UI 视觉仍由 fixture 离屏验收覆盖。

### 验证

| 命令 | 结果 |
| --- | --- |
| `./gradlew :app:jvmTest` | 105 tests，0 failures（新增 13 个 WorkspaceState 用例、2 个 AppState workspace 生命周期用例、1 个 compact TTL 用例） |
| `./gradlew ktlintCheck` | 通过 |
| `./gradlew build` | 通过 |
| `./gradlew :desktopApp:run` | 启动成功，进程存活；未点击任何用户连接 |
| 临时真实 Redis 只读测试（已删除） | 通过；SCAN/TYPE/TTL/MATCH/会话/WorkspaceState 均正常 |

离屏渲染（ImageComposeScene + 预览 fixture，临时脚本未提交）检查了：

- 1440/1120 宽度、深/浅主题；空 DB、SCAN 错误、搜索命中/无匹配、类型筛选、平铺视图、置顶 + 完整展开树。
- 目录计数、类型点/类型色、TTL（含 ≤60s danger）、置顶星标、选中条、结果头、footer 进度与自适应省略均正常；右侧 Key 名与类型 pill 使用真实 state。
- 侧栏与用户连接信息未出现在渲染中（使用 fixture，不读取 `connections.json`）。

### 遗留风险与边界

1. **真实 Redis 联调**：已在用户提供的测试实例（Redis 8.6.2）上完成只读路径联调（见上节）。尚未覆盖：10k+ Key 的真实滚动性能、UI 内真实点击流（避免截图暴露服务器 Key 名）、以及 Phase 5 的写路径。真实 SCAN cursor 与 MATCH 行为已验证。
2. **键盘回放未自动化**：⌘F/Esc/R/N 只做了代码级检查与离屏静态渲染；macOS 未授权 osascript 自动按键。
3. **面板宽度**：使用 `rememberSaveable`，同一 tab 内保持；关闭并重开 Keys tab 会回到 316dp（未写入 AppState/设置）。
4. **置顶不持久化**：按计划为 WorkspaceState 内存态，关闭 Keys tab 即清空。
5. **TYPE/TTL 失败不重试**：单 Key 预览失败会保留灰点并跳过该 Key 的后续自动重试，刷新后重新请求；这是为了避免 toast 风暴。
6. **自动展开层级**：按文档只自动展开深度 0 目录，因此首次进入只看到一级目录，需要再展开一层才能看到 Key；与设计稿“部分二级目录默认展开”不同，属于按计划的取舍。
7. **搜索无结果时**：直接显示“没有匹配的 Key”空状态（含清除按钮），不额外保留“0 个结果”头部。
8. **剪贴板 API**：使用 `LocalClipboardManager`（编译有 deprecation 警告）；Compose 新的 `LocalClipboard` 是 suspend API，留 Phase 10 统一替换。
9. **预览 fixture 在生产源码中**：`WorkspacePreviews.kt` 与 `ShellPreviews.kt` 一样只服务于 `@Preview`/离屏验收，不参与运行时路径；Phase 10 可移到 tooling 源集。

---

## 当前基线

- 分支：`feat/rewrite-ui`；模块：`:desktopApp` + `:app`。
- 包名：`com.czh.rediscompose`；入口：`com.czh.rediscompose.desktop.MainKt`。
- 应用启动直接进入新外壳（顶栏 / rail / 连接侧栏 / tab / 状态栏），不再显示设计画廊。
- `AppState` + `RedisSession` 已接管连接、tab、主题、设置与旧数据迁移；测试 105/105 通过。
- Keys 页已接入真实 SCAN Key 浏览器（树/平铺、搜索、类型筛选、置顶、TTL/SCAN 状态）；Console / Insights / Connections / Settings 仍是占位内容。
- 下一步 Phase 5：值检查器、6 种类型编辑器与 Key 操作；不要提前实现控制台/洞察。
- 下一阶段前置条件：Phase 4 已完成真实只读联调；Phase 5 涉及写入，开始前请确认该测试实例可安全写入，或指定一个独立的测试 DB/实例。
