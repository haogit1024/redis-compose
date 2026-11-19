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
| 5 — 值编辑器 | 已验收 | 2026-11-18 | 见下文 §Phase 5 | 见“遗留风险” |
| 6 — 控制台 | 已验收 | 2026-11-19 | 见下文 §Phase 6 | 见“遗留风险” |
| 7 — 洞察 | 已验收 | 2026-11-19 | 见下文 §Phase 7 | 见“遗留风险” |
| 8 — 连接库 | 已验收 | 2026-11-19 | 见下文 §Phase 8 | 见“遗留风险” |
| 9 — 设置 + 命令面板 | 已验收 | 2026-11-19 | 见下文 §Phase 9 | 见“遗留风险” |
| 10 — 打磨与发布 | 已验收 | 2026-11-19 | 见下文 §Phase 10 | 见“遗留风险” |

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

## Phase 5 — 值检查器与编辑器（已验收）

### 完成内容

#### 领域 / 文本处理

- `design/text/JsonHighlighter.kt`：无异常 JSON 词法高亮（key/string/number/boolean/punctuation），支持转义引号、嵌套、Unicode、非法输入；`JsonHighlighterTest` 6 个用例。
- `KeyValue.StringValue` 新增 `isBinary` 与 `byteLength`：非 UTF-8 值以 HEX 展示并进入只读，大值用原始字节数计算截断；同步更新 `domain-and-data.md`。
- `util/Formatters`：新增 `truncateUtf8`（不拆码点）与 `currentEpochSeconds` expect/actual；支持大值截断和真实的“读取于 HH:mm:ss”。

#### WorkspaceState

- 详情状态：`keyDetail` / `detailLoading` / `detailError` / `detailLoadedAtEpochSeconds`，选择 Key 时取消旧 job 并用 generation 防止旧详情覆盖新选择；失败保留已选 Key 并可重试。
- 详情加载顺序：TYPE → TTL → MEMORY USAGE → OBJECT ENCODING → 值；Stream 额外用 `XLEN` 取真实条目总数、`XINFO GROUPS` 取组数。
- 值解析：String（UTF-8 / HEX / JSON 检测）、Hash、List、Set、ZSet（按 score 降序）、Stream（最近 50 条，任意字段名）。
- 写操作：String 保存、Hash 新增/改名/删除/批量保存、List LSET/LPUSH/RPUSH/删除、Set 添加/改名/删除（重复校验）、ZSet 添加/改名/改分（有限数字校验）/删除、TTL（-1 → PERSIST）、重命名（目标存在则拒绝）、删除、新建 5 种类型。
- 只读与安全：`isReadOnly`（全局或连接）在 state 层拦截所有写操作；非 UTF-8 String 拒绝文本写回；新建集合必须至少一个有效成员；保存中 `isSaving` 防重入。
- 保存后刷新详情与元信息，同时维护 keys/tree/pinned/keyTypes/keyTtls；删除/重命名/新建会同步列表、树、置顶、选中与总数。
- `saveRequestId` + `consumeSaveRequest()` 支持全局 ⌘S；编辑 dirty 会写入 `hasUnsavedEdits`，切换 Key 时 toast 提醒。

#### 值面板 UI

- `ValuePane`：最大宽度 1020dp 居中；Header → MetaGrid → 只读 banner → 编辑器 → footnote；加载骨架、错误重试、空状态。
- `ValueHeader`：类型 pill、JSON / BINARY / 已置顶 / 即将过期 / 只读 badge；可复制长 Key；db、host:port、OBJECT ENCODING、读取时间；置顶、复制、在控制台打开、更多菜单。
- `ValueMetaGrid`：TTL / MEMORY / ENCODING / 类型相关计数，宽内容 630dp 以上 4 列否则 2 列；TTL 进度条 amber→rose。
- `ValueEmptyState` / `ValueLoadingState`：74dp 品牌式 glyph、快捷键提示；骨架 meta + 编辑器 loading。
- `KeyActionsMenu`：置顶、复制 Key、在控制台打开、复制为命令（GET/HGETALL/LRANGE/SMEMBERS/ZRANGE/XRANGE）、TTL、重命名、删除。
- `StringEditor` + `CodeEditor`：行号 gutter、JSON 高亮、Auto/JSON/TEXT/HEX、格式化、保存；HEX 只读且隐藏保存；非 UTF-8 显示 HEX 并提供复制；大值显示“已截断 + 复制完整值 + 仍然编辑”。
- `HashEditor` / `ListEditor` / `SetEditor` / `SortedSetEditor` / `StreamEditor`：固定表头、34dp 行、行 hover 操作、筛选、空状态、只读文本模式；Hash/List/Set/ZSet 支持行保存与工具栏批量保存；ZSet score 非法时行内红色提示并拦截；Stream 只读、点击条目展开任意字段。
- `NewKeyDialog`（560dp，5 类型卡片 + TTL + 动态字段）、`TtlDialog`、`RenameKeyDialog`、`DeleteKeyDialog`。
- 只读、BINARY、截断、长 Key、长集合值均有明确视觉状态；长集合值提供 hover tooltip。
- `AppButton` 禁用时整体降低透明度，`AppDialog` 增加可选宽度，`AppIconButton` 增加可选 tint；新增值面板/编辑器相关 `AppDimens` 与 `AppTypography` token。
- 替换 `ValuePanePlaceholder.kt`；⌘S 保存、⌘F 搜索、Esc、R/N 继续生效；对话框打开时拦截工作区快捷键。

### 验证

| 命令 | 结果 |
| --- | --- |
| `./gradlew :app:jvmTest` | 128 tests，0 failures（新增 JsonHighlighter、Phase 5 编辑/只读/TTL/重命名/删除/新建/重复成员/有限分数等用例） |
| `./gradlew ktlintCheck` | 通过 |
| `./gradlew build` | 通过 |
| `./gradlew :desktopApp:run` | 启动成功，进程存活 |

离屏渲染（预览 fixture，临时脚本已删除）：

- 深/浅主题：Hash、String(JSON)、List、Set、ZSet、Stream、空值面板、只读值面板。
- 真实内容区宽度 802dp（1440 窗口去掉 rail/侧栏/key pane 后）与 1120dp 窗口：meta grid 自动 2 列、toolbar 自动换行、表格滚动、footer 正常；另在 802×590（720dp 窗口可用高度）检查 String/Hash/List/ZSet 不溢出。
- TTL / 重命名 / 删除 / 新建 Key 四个 Dialog 均正确渲染；禁用按钮透明度可见。

### 真实 Redis 写入联调

在用户提供的测试实例（Redis 8.6.2，db0 原本为空）执行临时只读+写入集成测试，全部使用 `redis-compose:phase5:<timestamp>:` 前缀并在 finally 清理；凭据仅来自环境变量。

- String：创建、读取 JSON/长度、保存、TTL 120s、PERSIST → 全部通过。
- Hash：创建、字段改名 + 改值、删除最后一个字段（Key 自动消失）→ 通过。
- List：创建、LPUSH、RPUSH、LSET、按值删除 → 通过。
- Set：创建、改名、删除最后成员 → 通过。
- ZSet：创建、改名 + 改 score、降序读回、删除 → 通过。
- Rename：真实 RENAME、列表/置顶/选中同步；目标已存在拒绝。
- Delete：真实 DEL、选中清空、列表移除。
- Stream：通过测试命令 XADD 创建，读取真实 ID 与任意字段，组数为 0。
- 非 UTF-8 String：HEX 只读，state 拒绝写回。
- 清理后 `db0 keys` 回到测试前的 0，所有测试 Key 均不存在。

### 遗留风险与边界

1. **集合内的非 UTF-8 成员**：String 已正确识别 BINARY/HEX；Hash/List/Set/ZSet 的 ByteArray 成员目前只在显示层做 HEX 回退，没有逐行 binary 标记，编辑这类成员可能把 HEX 文本写回。真实联调只覆盖了非 UTF-8 String；Phase 10 可为集合成员增加 binary 标记与只读行。
2. **List 删除语义**：网关 `LREM count=1`，删除的是第一个同值成员，不一定是指定 index；UI footer 已提示“删除同值时移除首个匹配项”。需要按 index 精确删除时应使用 LSET 哨兵 + LREM 或 Phase 10 的服务端脚本。
3. **写操作非事务**：批量保存逐行写入；中途失败会保留已成功部分并 toast，详情不刷新，用户可重试或刷新。重命名前的“目标是否存在”检查存在 TOCTOU 竞争窗口。
4. **大值**：默认只展示前 `truncateValueKb` KB，“仍然编辑”后会把完整文本放进 BasicTextField，超大值可能卡顿；v1 未做按需加载/分段编辑。
5. **Stream**：Phase 5 只读；不支持 XADD、消费者组管理、分页加载更多（固定最近 50 条）。
6. **布局取舍**：值面板的编辑器统一占用剩余高度并在内部滚动，而不是“集合自然高度 + 外层整页滚动”；String 编辑器也不强制 280dp 最小高度（在 720dp 窗口下会溢出），代码区随可用高度收缩并滚动。Meta grid 使用内容宽度 630dp 作为 4 列阈值（近似设计稿的 1320 窗口断点）而非文档里的 960dp。
7. **Dirty 保护**：切换 Key 只有 toast 提醒，没有“放弃/保留”确认对话框；Phase 10 完善。
8. **真实 UI 点击流**：为避免把真实 Key 名渲染进截图，真实实例验证在 WorkspaceState/gateway 层完成；桌面 UI 仍使用 fixture 离屏验收。
9. **键盘**：⌘S 通过 saveRequest 实现；macOS 未授权自动按键，未做自动化回放。
10. **演示/预览 fixture**：`WorkspacePreviews.kt` 随生产源码编译，仅供 `@Preview` 与离屏验收；Phase 10 可移到 tooling 源集。

---

## Phase 6 — 命令控制台（已验收）

### 完成内容

#### 工具与状态

- `util/RedisCommandParser.kt`：单行命令解析，支持单/双引号、双引号内 `\" \\ \n \t \r \b` 转义、空参数；未闭合引号返回 null，控制台显示解析错误且不发送。
- `util/RedisCommandCatalog.kt`：本地常用命令 + 语法表（含 MEMORY USAGE / SLOWLOG GET / CLIENT LIST / CONFIG GET 等多词命令），按前缀最多 6 条建议。
- `util/WriteCommandGuard.kt`：`READ / WRITE / UNKNOWN` 分类；覆盖计划中的写命令，并额外覆盖 SORT/位图/Streams/Geo/ACL 等；多词命令检查前两个 token；未知命令在只读模式下拦截；`DUMP / PUBLISH / CLIENT LIST / SELECT / CONFIG GET` 等允许。
- `feature/console/ConsoleEntry.kt`：Command / Note / Output / Error；Command 记录执行时 prompt（db 准确）；另维护历史行（时间、耗时、摘要、失败态）。
- `feature/console/ConsoleState.kt`：
  - 初始 Note、欢迎语随连接/版本刷新；历史从 `settings.recentCommands` 恢复（最多 20 条），会话历史上限 200。
  - `execute`：解析 → Command entry → 历史 → 只读拦截 → `gateway.execute` → Output/Error；连接异常才 toast；命令错误只进终端。
  - 历史 ↑↓、点击填充、`fillCommand`、清空只保留 history；输入补全与建议状态独立维护。
  - `SELECT n` 成功后同步 `RedisSession.currentDatabase` 并通知 AppState 刷新 Keys 面板；命令耗时记录，成功后调用 `onRecordCommand`（AppState 写 `recentCommands`，最多 20 条、去重相邻）。
  - 只读模式 PUBLISH 允许但追加 Note 说明副作用；未知命令给出专门提示。
  - 每个 tab 独立 SupervisorJob；关闭 tab 取消进行中的命令。
- `feature/console/ConsoleOutput.kt`：递归 reply → 终端行；Simple/Integer/Bulk/Nil/Error/Array；UTF-8 bulk 引号、非 UTF-8 HEX、多行 bulk 逐行；嵌套数组编号 + 缩进；数组最多 200 项、深度 3、bulk 300 行、单结果 600 行、单条日志 500 entries 截断；`plainText` 供复制日志。

#### UI

- `ConsoleScreen`：主区（toolbar / terminal / quick commands / composer）+ 272dp 历史栏；内容宽度 < 720dp 时隐藏历史栏；深/浅主题下终端始终使用深色 token。
- `ConsoleToolbar`：`Command console` + host:port / dbN + 只读 badge + 真实 RTT badge + 命令帮助 Dialog（快捷键、只读规则、PUBLISH 说明）。
- 终端：37dp 标题栏（三色圆点、`redis-cli — 连接名`、db 徽标、复制/清空）、`LazyColumn` 日志、用户上滚暂停自动滚动、回到底部恢复。
- `ConsoleBlock`：命令时间 + `redis@name:port[db]>` prompt + 命令；结果头部类型/数量/耗时；递归行按 tone 着色；错误红色 `(error)`。
- `CommandComposer`：聚焦环、prompt、单行输入、`⌘↵ 执行` 提示、运行按钮 loading；补全 Popup（最多 6 条，命令 + 语法）；Enter 执行、Tab 补全、Esc 关闭、↑↓ 历史。
- `QuickCommands`：INFO memory / DBSIZE / SLOWLOG GET 10 / SCAN 0 COUNT 100 / MEMORY DOCTOR；点击只填充并聚焦。
- `ConsoleHistoryPane`：真实会话历史（时间、命令、耗时、摘要、失败色）、静态收藏命令（只填充不执行）、快捷键提示；持久命中的 recentCommands 以 `--:-- / 历史` 恢复。
- 复制全部日志到剪贴板并 toast；清空会话保留历史；导出文件按计划未实现（按钮未提供）。
- 终端专用颜色 token 加入 `AppColors`（深/浅主题均保持深色终端），新增 console/terminal 尺寸与 `terminal`、`terminalSmall` 文本样式。
- `AppState` 增加 `consoles` 生命周期：打开 Console tab 创建、关闭 tab/保存或删除连接/退出 dispose；命令成功写入 `settings.recentCommands`。

### 验证

| 命令 | 结果 |
| --- | --- |
| `./gradlew :app:jvmTest` | 164 tests，0 failures（新增 parser/guard/catalog/output/console state/AppState 用例） |
| `./gradlew ktlintCheck` | 通过 |
| `./gradlew build` | 通过 |
| `./gradlew :desktopApp:run` | 启动成功，进程存活 |
| 离屏渲染 | 1440×900 深/浅、802×590 内容宽度、输入填充/历史恢复；终端深色底、嵌套数组、历史栏和 composer 布局正常 |
| 真实 Redis 控制台联调 | PING / DBSIZE / INFO memory / GET / SCAN(嵌套数组) 通过；只读模式 SET 被拦截、GET 正常；测试 Key 清理后 db0 恢复 0 |

真实联调使用 `redis-compose:phase6:<timestamp>:` 前缀，只创建一个测试 String 并在 finally 删除；未记录服务器地址或密码。

### 遗留风险与边界

1. **补全弹层未离屏验证**：补全使用 `Popup`，离屏渲染不包含弹层；逻辑由 catalog 单测与 Composer 代码检查覆盖。macOS 未授权自动按键，Enter/Tab/↑↓/Esc 未做 UI 自动回放。
2. **导出文件未实现**：计划为可选项；当前只提供复制全部日志。
3. **HGETALL 表格**：按计划可选项，当前以递归数组（字段/值交替）显示，没有两列表格。
4. **PING 显示引号**：Jedis 对 PING 返回 bulk，控制台显示 `"PONG"` 而非 `PONG`；属于网关/协议信息损失，不影响结果。
5. **历史恢复**：仅 recentCommands 持久化；重开 tab 后历史栏以 `--:-- / 历史` 恢复命令，耗时/摘要不保留。
6. **只读安全**：未知命令一律拦截；PUBLISH 允许并提示副作用。若未来接入 Pub/Sub 面板需重新评估。
7. **大输出**：输出有项数/深度/行数/entry 上限；极端二进制或超大 bulk 仍可能占用较多内存。
8. **非目标**：多行命令、Monitor、Pub/Sub 订阅、完整参数补全、命令收藏持久化。
9. **剪贴板**：仍使用 `LocalClipboardManager`（deprecation warning），Phase 10 统一替换。

---

## Phase 7 — 服务器洞察（已验收）

### 完成内容

#### 状态与采样

- `feature/insights/InsightsRange.kt`：`LAST_5_MINUTES / LAST_15_MINUTES / LAST_1_HOUR / SESSION`（标签 5M / 15M / 1H / 会话）。
- `feature/insights/InsightsState.kt`：
  - `start()` 立即 `INFO` + `INFO commandstats` 采样一次，之后每 5 秒一轮；`stop()` 取消轮询但保留 `samples`，重新进入页面立即刷新并恢复图表。
  - 每 3 轮（约 15 秒）刷新 `SLOWLOG GET 10` 与 `CLIENT LIST`；这两项失败只保留旧数据，不覆盖已有内容。
  - `samples` 记录 `MetricsSample`，按最新样本向前裁剪 1 小时并限制 720 条；关闭 tab 时 `dispose()` 丢弃。
  - `autoRefresh = false` 时停止自动请求，手动 `refreshNow()` 仍可用；`isLoading` 只用于首轮/手动刷新，轮询不闪烁。
  - 失败设置 `error` 并保留旧快照、旧样本和旧慢查询；不弹 toast，下一次成功后自动清除错误。
  - `dashboardData()` 统一产出 KPI、吞吐、内存、命令分布、键空间、健康、慢查询与客户端视图数据；区间只影响图表与 KPI 均值，瞬时指标使用最新样本。
- `feature/insights/InsightsMetrics.kt`：纯函数区间过滤、样本裁剪、命中率、内存使用率、读 Ops/s、命令 calls 差值、Top 6 + 其他、keyspace 解析、客户端 name 前缀分组、健康状态推导、图表取整与格式化。
- `util/Formatters` 新增 `currentEpochMillis()` expect/actual（JVM 使用 `Instant`），用于 `MetricsSample` 时间戳。

#### 页面与图表

- `InsightsScreen`：`DisposableEffect` 绑定 `start/stop`；页面可滚动，最大宽度 1220dp，内边距 22/24dp，自绘竖向滚动条。
- `InsightsHeader`：`OBSERVABILITY` 眉标、服务器洞察 + 呼吸 `LIVE`、`host:port · 版本 · 运行时长 · 自本次连接 · 最后更新`；右侧区间 segmented、自动刷新开关/暂停文案、刷新按钮；内容宽度 < 860dp 时控件换到标题下方，避免副标题折行出现孤立分隔点。
- `KpiGrid`：四列；内容宽度 < 900dp 时两列。四张卡：已用内存（含 maxmemory / 无上限）、当前 Ops/s + 区间均值、命中率、连接数；每张卡使用样本 Sparkline，样本 < 2 不画线。
- `ThroughputCard` + `AreaChart`：总 Ops/s（accent 面积/线）与读命令 Ops/s（success 线）；3 条虚线网格、5 个时间刻度、Y 轴 0/中值/最大值；中点三次贝塞尔平滑、900ms 首次揭示；样本 < 2 显示“正在收集数据…”。
- `MemoryCard` + `DonutChart`：有 maxmemory 时 used vs max 圆环 + 已使用/峰值/碎片率/可用 + 峰值/淘汰键；无 maxmemory 时不画圆环，显示实际用量与“未设置 maxmemory”，不推算百分比。
- `CommandDistributionCard`：会话内最近两次 samples 的 calls 差值，横向条形 + 百分比，Top 6 + 其他；首轮显示“正在采样…”，后续无增量显示“暂无命令调用”。
- `KeyspaceCard`：INFO keyspace 的 DB / Keys / Expires / TTL 均值；空 DB 显示 0；显示到最后一个非空 DB 后两行（最少 5、最多 16 行），避免 16 个空 DB 把卡片拉得过长。
- `HealthCard`：主从复制、RDB、AOF、内存水位；状态色 + 文字（正常/关注/异常/未知/未启用/无上限）；缺字段显示未知，不默认正常；右上角状态点取最严重级别。
- `SlowLogCard`：真实 `SLOWLOG GET 10`；命令截断、客户端、耗时（>10ms amber）、时间；点击行复制完整命令并 toast；空状态、打开控制台入口。
- `ClientsCard`：应用连接 / 管理会话 / 空闲 / 最大连接四个 mini stat；按 name 前缀（如 `worker-*`）分组条形；不展示客户端 IP 列表；CLIENT LIST 为空时显示连接数与空状态。
- `chart/`：全部 Compose Canvas 自绘（Sparkline / AreaChart / DonutChart / ChartPrimitives），未引入任何图表库；颜色全部来自 `AppColors`。
- 新增 `AppDimens` insights token（页面宽度、KPI、图表、donut、表格行等）与 `AppTypography` token（KPI 数值、图表轴、表格、健康行、LIVE）。

#### 组合根与生命周期

- `AppState` 新增 `insights` 映射与 `insightsFor(connectionId)`；打开 Insights tab 创建，关闭 tab / 保存或删除连接 / `close()` 时 `dispose()`。
- `PlaceholderContent` 的 `INSIGHTS` 分支路由到真实 `InsightsScreen`，并传入“打开控制台”回调；删除 Insights 占位与不再使用的 PageScaffold/PlaceholderCard。
- `WorkspaceTabBar` 顶部刷新与“更多 → 刷新”在当前页为 Insights 时调用 `refreshNow()`。
- `InsightsPreviews.kt`：仅预览/离屏验收使用的内存 gateway 与固定样本，不读取用户连接文件、不连接真实 Redis；覆盖 1440/1120 内容宽度、深/浅、空状态/错误、无 maxmemory、各子卡片与三个图表组件。

### 与设计稿的有意差异

| 设计稿 | 实现 |
| --- | --- |
| 1H / 6H / 24H / 7D 历史 | `5M / 15M / 1H / 会话`，数据只来自本次打开页面后的采样；`会话` 也受 1 小时内存保留上限约束 |
| 命令 / 读取 24h 曲线 | 总 Ops/s 与读命令 Ops/s 的 5 秒会话采样；不伪造 24h |
| 内存分布 donut（数据集/开销/可用） | used vs max 的 donut；`maxmemory = 0/缺失` 时不画 donut，明确显示未设置上限 |
| 复制/持久化健康卡 | 从真实 `INFO replication` / `INFO persistence` / `INFO memory` 解析；缺字段为未知 |
| 慢查询 10 条 | `SLOWLOG GET 10`，按真实命令、客户端、耗时与时间渲染 |
| 客户端分布 | `CLIENT LIST` 按 name 前缀分组；不展示 IP 列表 |
| KPI “vs 昨天” delta | 内存为区间首尾差值，Ops 为区间均值，命中率/连接数为较上次采样；无历史基线时显示“最近采样”或省略 |
| 页面副标题时间范围 | 明确写“自本次连接”和“最后更新 Ns 前” |
| 底部三列在 1120 保持三列 | 内容宽度 < 900dp 时三卡纵向堆叠；主图表在 < 720dp 时也堆叠，避免压缩内存卡 |
| 键空间列出 5 个 DB | 显示到最后一个非空 DB 后两行（最少 5、最多 16），空 DB 补 0 |

### 验证

| 命令 | 结果 |
| --- | --- |
| `./gradlew :app:jvmTest` | 183 tests，0 failures（Phase 6 基线 164 + 19 个新用例） |
| `./gradlew ktlintCheck` | 通过 |
| `./gradlew build` | 通过 |
| `./gradlew :desktopApp:run` | 启动成功，`MainKt` 进程存活 ≥ 20 秒，无应用异常；验收后结束进程 |

新增测试：

- `InsightsMetricsTest`（12）：区间过滤、1 小时/720 条裁剪、命中率与内存比例空值、读 Ops/s 差值、Top 6 + 其他、keyspace 空 DB、客户端前缀分组、健康状态缺失/主从异常/无 maxmemory、图表取整与格式化、内存统计解析。
- `InsightsStateTest`（6）：真实 gateway 数据进入快照/命令统计/慢查询/客户端与 sample、start/stop 与保留样本、暂停自动刷新时手动刷新、失败保留旧数据、首轮采样与命令差值、区间切换不发网络请求。
- `AppStateTest`（1）：Insights tab 创建并随关闭释放状态。

离屏渲染（临时 `ImageComposeScene` 工具，验收后删除临时测试与 `jvmTest` 的 `compose.desktop.currentOs` 依赖）：

- 1440 窗口主内容 1074×770、1120 窗口主内容 802×590、更窄 754×590，以及 1074×1400 长页；深/浅主题。
- 有样本、无样本 + 错误条、`maxmemory` 未设置、KPI 2/4 列、主图表并排/堆叠、键空间 5 行、慢查询/客户端两列均检查。
- 确认图表路径、面积渐变、donut、网格/刻度、LIVE 呼吸点、滚动条与浅色主题对比可读。

### 真实 Redis 联调边界

本阶段没有使用真实 Redis 实例：页面只新增 `INFO`、`INFO commandstats`、`SLOWLOG GET 10`、`CLIENT LIST` 四类只读命令，解析器与网关已在 Phase 2/4/6 的真实实例联调中覆盖；本轮离线使用 FakeRedisGateway 与固定 INFO / SLOWLOG / CLIENT LIST fixture 验证状态与渲染。未读取或打印用户 `connections.json`，未点击真实远端连接，未执行任何写命令。

### 遗留风险与边界

1. **真实 UI 采样未人工观察**：没有连接真实 Redis 观察 5–10 秒后的 KPI 增长；Fake gateway 单测覆盖了采样、差值和错误路径，建议用户在自己的测试实例打开一次 Insights 页确认 `SLOWLOG` / `CLIENT LIST` 字段与真实服务端一致。
2. **CLIENT LIST 分组是启发式**：按 name 的 `-` / `_` / `:` 前缀分组，空 name 归为 `other`；`redis-compose*` / `redis-cli*` 计为“管理会话”，其余计为“应用连接”。不使用 IP，无法区分未命名客户端。
3. **命令分布只比较最近两次 samples**：上一个 5 秒没有命令时显示“暂无命令调用”；首轮显示“正在采样…”。不做会话累计，避免把累积计数器误当近期分布。
4. **会话区间不等于无限历史**：内存最多保留 1 小时 / 720 条；关闭 Insights tab 会丢弃样本。跨会话历史、告警、导出按非目标未实现。
5. **轮询与窗口生命周期**：离开页面（tab 切换/关闭）会 `stop()`；窗口最小化但 tab 仍在前台时轮询继续，未监听窗口可见性。
6. **图表动画**：首次进入 900ms 揭示；后续每 5 秒数据更新直接跳变，避免持续闪烁。Sparkline 使用原始采样点，客户端数等离散值会呈锯齿状。
7. **健康判断阈值**：内存水位 ≥90% 异常、≥75% 关注，属于本阶段约定，非可配置告警规则。
8. **时钟差**：RDB “最近保存 N 前”使用本机当前时间减服务端 `rdb_last_save_time`；客户端与服务端时钟偏差会影响显示。
9. **键空间空库**：Redis 的 `INFO keyspace` 不列出空 DB；全部 DB 为空时没有 keyspace section，页面显示空状态，而不是 16 行 0。
10. **剪贴板**：慢查询复制仍使用 `LocalClipboardManager`（与 Phase 4–6 相同的 deprecation warning），Phase 10 统一替换。
11. **保存连接后的状态**：`saveConnection` 会 dispose insights state，但保留已打开的 Insights tab（与 Keys/Console 现有行为一致）；Phase 8 已改为保存时关闭旧会话与相关 tabs（见 §Phase 8）。
12. **预览 fixture 随生产源集编译**：`InsightsPreviews.kt` 仅服务 `@Preview`/离屏验收，不参与运行时；Phase 10 可移到 tooling 源集。

---

## Phase 8 — 连接库与连接表单（已验收）

### 完成内容

#### 连接库页面

- `feature/connections/`：`ConnectionsScreen`、`ConnectionsState`、`ConnectionsHeader`、`ConnectionCard`、`AddConnectionRow`、`RecentConnectionsTable`、`ConnectionSheet`、`ConnectionDraft`、`ConnectionCardMenu`。
- 页头：`CONNECTION LIBRARY`、连接库、真实在线数 / 连接总数、描述、禁用并带 tooltip 的“导入配置”、新建连接。
- 工具栏：本地实时搜索（名称 / Host / 分组）、环境 tabs（全部 / Production / Staging / Local / Analytics / Other）、连接计数、网格/列表切换、最近使用/名称排序。
- 网格：内容宽度 ≥1150dp 三列、≥850dp 两列、其余一列；新增连接行始终跨整行；列表模式单列。
- 连接卡：顶部 2dp 颜色渐变线、颜色缩写块、环境 badge、host:port、真实状态 pill（已连接 + 延迟 / 连接中 / 失败 / 未连接）、TLS / SSH / 只读 / Redis 版本 / DB 数 / 默认 db 标签、keys / clients / 最近使用统计；错误卡显示错误首行。
- 卡片菜单：打开工作区、编辑、复制连接、测试连接、删除。卡片点击进入 Workspace。
- 最近连接表：只用真实 `lastOpenedAtEpochMillis`，最多 6 条；空时显示“还没有连接记录”。
- 空状态：无连接时大空状态 + 新建连接；筛选无结果时“没有匹配的连接”+ 清除筛选。
- 删除连接：确认对话框，级联关闭 session / tabs / feature state，持久化后更新 activeConnection。

#### 连接表单（侧滑）

- `AppSheet` 增加可选 `headerLeading` 与 `footerLeading`，保持既有调用兼容。
- 470dp 全高侧滑；头部颜色块 + 标题 + 副标题 + 关闭；tabs 基础 / TLS / SSH 隧道 / 高级；打开时焦点落到连接名称。
- 基础：名称、Host、Port、ACL 用户名、密码（可显示/隐藏）、环境、默认 DB、Key 分隔符、分组、6 种颜色、只读开关。
- TLS：启用开关、CA 路径、客户端证书/私钥路径、校验模式（严格 / 允许自签名 / 不校验），非严格模式 amber 警告。
- SSH：启用开关、Host/Port、用户名、私钥/密码认证、路径或密码字段；明确提示 v1 不校验 known_hosts。
- 高级：连接/读取超时、Key 分隔符、禁用的 Sentinel / Cluster 行（明确“后续版本提供”，没有假开关）。
- 校验：字段下方红字 + 顶部错误汇总；端口/DB/超时非数字单独提示；TLS 证书与私钥必须成对；SSH 密码认证必须填写密码。
- 测试连接：使用当前未保存的 draft 构造临时 profile，经 `gatewayFactory` 真实 ping，成功后读取 INFO 版本；成功/失败显示在 footer，失败不关闭 sheet，临时 gateway 始终关闭。
- 保存：新建生成 id；编辑保留 id / createdAt / lastOpened 并更新 updatedAt；复制使用 `AppState.duplicateConnection` 的新 id 和“副本”名称；失败保持 sheet 打开。
- 未保存修改关闭确认按计划留到 Phase 10。

#### AppState / 持久化

- `AppState` 新增 `connectionLibrary`、`duplicateConnection()`、`suspend testConnection()`，并把 `runtimeFor` 映射为卡片所需的连接状态/延迟/版本/Key/客户端/DB 信息。
- `openTab` 会更新 `lastOpenedAtEpochMillis` 并持久化；最近连接与排序使用真实打开时间。
- `saveConnection` 会先关闭该连接的旧 session / tabs / Workspace / Console / Insights，避免编辑后继续使用旧 gateway；新连接直接加入列表。
- `deleteConnection` 统一走同一套运行时清理。
- `close()` 会 `dispose()` 连接库 state。
- `PlaceholderContent` 的 `CONNECTIONS` 分支改为完整 `ConnectionsScreen`，删除占位列表。
- `ConnectionField` 增加 `SSH_PASSWORD / CONNECT_TIMEOUT / READ_TIMEOUT`；`validateConnection` 覆盖新增字段与 TLS 证书成对规则。
- 新增 `ConnectionTestResult` 领域类型，供表单和 AppState 共用。

#### TLS / SSH 运行时（8B）

- 新增依赖 `com.github.mwiede:jsch:2.28.8`（记录于 `gradle/libs.versions.toml`）。
- `data/jedis/SshTunnel.kt`：
  - JSch session + `setPortForwardingL(0, targetHost, targetPort)`，Jedis 连接 `127.0.0.1:本地端口`。
  - 支持私钥（`~` 展开、可选 passphrase 使用 SSH 密码字段）与密码认证。
  - `close()` 释放端口转发与 session；通常由 Jedis gateway 的 `closeInternal()` 调用。
  - `StrictHostKeyChecking=no`，UI 明确标注；错误映射为“SSH 隧道连接失败：…”。
- `data/jedis/TlsContextFactory.kt`：
  - 严格模式 + 无 CA：系统信任链。
  - 自定义 CA：PEM → KeyStore → TrustManager。
  - ALLOW_SELF_SIGNED / NONE：trust-all；NONE 额外关闭主机名校验。
  - 客户端证书：证书链 + 未加密 PKCS#8 / RSA PKCS#1 私钥 → KeyManager；加密私钥与无法识别格式明确报错，不降级明文。
- `JedisRedisGateway`：按 profile 选择直连 / TLS / SSH 隧道；SSH + TLS 时按原始 Redis host 做主机名校验；任何失败都关闭临时隧道/客户端。
- `JedisGatewayFactory` 无需改动，测试连接和正式连接共用同一 factory。

#### 预览与测试

- `ConnectionsPreviews.kt`：固定连接 fixture、真实感 runtime、空状态、列表、删除确认、四个 sheet tab、测试成功/失败；不读取用户连接文件、不联网。
- 新增 33 个用例（详见验证），覆盖 draft 转换/校验、筛选/排序/最近连接、保存/删除/复制/测试、AppState 生命周期、TLS/SSH 失败路径。

### 与设计稿 / 计划的有意差异

| 设计稿 / 计划 | 实现 |
| --- | --- |
| 导入配置按钮 | 禁用 + tooltip “暂不支持”，不做假功能 |
| 新建卡展示 Sentinel / Cluster | 只展示真实支持的 TLS / SSH；Sentinel / Cluster 在高级 tab 显示禁用行 |
| 配置变更关闭 sheet 时确认 | 按计划留到 Phase 10 |
| 文件选择器“浏览”按钮 | 未实现，路径手动填写（支持 `~`） |
| SSH agent / known_hosts | 未实现；v1 `StrictHostKeyChecking=no`，UI 与 STATUS 明示 |
| 加密客户端私钥 / EC SEC1 | 明确拒绝并提示转换为未加密 PKCS#8 / RSA PEM |
| 连接卡审计活动 | 用真实最近连接表替代，不伪造事件 |
| 列表模式紧凑横排 | 列表为单列完整卡片，与设计 CSS 的 `.conn-grid.list` 一致 |
| 编辑连接后保留 tab | 保存编辑会关闭旧会话与相关 tabs，避免继续使用旧 gateway |

### 验证

| 命令 | 结果 |
| --- | --- |
| `./gradlew :app:jvmTest` | 216 tests，0 failures（Phase 7 基线 183 + 33 个新用例） |
| `./gradlew ktlintCheck` | 通过 |
| `./gradlew build` | 通过 |
| `./gradlew :desktopApp:run` | 启动成功，进程存活；未点击用户真实连接 |

新增/更新测试：

- `ConnectionDraftTest`（7）：必填字段、trim/转换、数字错误、编辑保留 id/createdAt/lastOpened、复制模式、TLS/SSH 字段、fallback id。
- `ConnectionsStateTest`（11）：搜索/环境/排序、最近 6 条、新建/编辑/复制 draft、校验拦截、新建/编辑保存、测试成功/失败、已有连接测试 toast、删除确认、卡片打开回调。
- `AppStateTest`（5）：保存后级联关闭运行时、复制连接、测试连接成功/失败与 gateway 关闭、打开连接更新最近时间并持久化。
- `ConnectionValidationTest`（+2）：SSH 密码必填（改写既有用例）、TLS 证书/私钥成对、超时必须为正。
- `TlsContextFactoryTest`（5）：系统信任链、非严格 trust-all、证书成对、CA 缺失、客户端证书缺失错误。
- `SshTunnelTest`（3）：未启用、私钥缺失、私钥文件不存在；均不打开外部 socket。

离屏渲染（临时 `ImageComposeScene`，验收后删除临时测试与 `jvmTest` 的 `compose.desktop.currentOs` 依赖）：

- 1380×816（1440 窗口、连接页隐藏侧栏）、1060×636（1120 窗口）、900×636 窄内容宽度；深/浅主题，长页。
- 3 列 / 2 列网格、列表、空状态、删除确认、四个 sheet tab、测试成功与失败 footer。
- 检查了卡片顶部渐变、状态 pill、标签、错误文案、最近连接表、sheet 滚动与 footer 按钮不溢出。

### 真实 SSH / TLS / Redis 联调边界

本阶段没有使用真实远端 Redis、SSH 跳板机或 TLS 服务端：

- 未读取/打印用户 `~/.redis-compose/connections.json`，未点击任何真实连接。
- SSH 只做单测覆盖“未启用 / 私钥缺失 / 私钥文件不存在”等失败路径；未验证真实主机密钥、真实 sshd 端口转发与关闭释放。
- TLS 只验证 trust-all / 自定义 CA / 客户端证书的配置构建与错误路径；未对真实 Redis TLS 服务端做握手。
- 连接测试/正式连接共用 `JedisGatewayFactory`，代码路径一致，但真实网络路径需要用户本地环境验证。
- 依赖 `jsch` 仅用于 JVM，未把 JSch 类型泄漏到 feature/domain。

### 遗留风险与边界

1. **SSH 主机密钥不校验**：v1 使用 `StrictHostKeyChecking=no`，存在中间人风险；UI 已提示，Phase 10 应支持 known_hosts / 用户确认。
2. **SSH 私钥格式**：JSch 支持 RSA/OpenSSH/Ed25519 等常见格式；加密私钥使用“私钥口令（可选）”字段，未实现 agent 认证、多跳跳板与 ProxyJump。
3. **TLS 客户端私钥**：只支持未加密 PKCS#8 与 RSA PKCS#1；EC SEC1、加密私钥、PKCS#12 未支持，会明确报错并拒绝连接。
4. **SSH + TLS 主机名**：通过原始配置 host 做主机名校验；如果证书 SAN 只包含内网名而配置写的是 IP，仍会失败，这是严格校验的预期行为。
5. **真实联调缺失**：没有真实 SSH/TLS 环境，端口转发、认证失败、证书链、连接关闭后的端口释放未在真实网络验证。
6. **编辑连接关闭 tabs**：保存编辑会断开旧会话并关闭该连接所有 tab，用户需要重新打开；这是为了避免复用旧 gateway 的有意取舍。
7. **最近连接写入频率**：每次打开 tab 都更新 `lastOpenedAt` 并写文件；文件很小，但频繁切换会有多次磁盘写入，Phase 10 可 debounce。
8. **表单草稿为内存明文**：密码只保存在当前 sheet state 中，保存由 repository 加密落盘；关闭后从内存释放。未实现剪贴板/日志脱敏检查。
9. **文件选择器未实现**：CA / 证书 / 私钥路径需要手动输入，支持 `~`；Windows 反斜杠路径已由 `Paths` 处理，但未在 Windows 实机验证。
10. **导入/导出、Sentinel/Cluster、SSH agent、未保存确认**：均按非目标未实现。
11. **预览 fixture 随生产源集编译**：`ConnectionsPreviews.kt` 仅用于 `@Preview`/离屏验收；Phase 10 可移到 tooling 源集。

---

## Phase 9 — 设置、主题持久化与命令面板（已验收）

### 完成内容

#### 设置页（`feature/settings/`）

- `SettingsScreen`：左侧 208dp 导航 + 右侧可滚动内容（最大 760dp 居中），顶部 `PREFERENCES` 眉标、标题、描述与“已自动保存”徽标，底部保存状态页脚。内容使用 `LazyColumn`，导航点击 `animateScrollToItem` 平滑滚动，滚动时按首个可见 section 更新 active。
- `SettingsState`：本地 UI 状态（active section、清除缓存确认框）；持久化值仍由 `AppState.settings` 提供。
- `SettingsNav`：偏好（外观、编辑器与值、键浏览）/ 系统（安全、快捷键、关于）分组、激活态 accentSoft + accent 边框、底部“同步状态”提示卡。
- `ThemeChoiceCard`：深色 / 浅色 / 跟随系统三张卡；mini 界面缩略图由 Compose 绘制（rail + 内容 + 面板），选中卡 accent 边框 + 2dp accentSoft 外环。
- `AccentSwatches`：IRIS / MINT / SKY / AMBER / ROSE 圆点；选中态主题对比色描边 + 内点（新增 `AppColors.swatchDot`）。
- `SettingsControls`：section 标题/面板/分隔线/55dp 设置行、数字输入（失焦提交并夹取范围）、字号 stepper、静态值 chip。
- `sections/AppearanceSection`：主题三态、强调色、语言（简体中文 + v1 徽标，其他语言不伪装可切换）、界面密度（舒适/紧凑）。
- `sections/EditorSection`：默认值格式 Auto/JSON/Text/HEX、等宽字体（v1 JetBrains Mono）、字号 11–18、自动换行、行号、大值截断 1–1024 KB。
- `sections/BrowserSection`：SCAN 批量 100–10,000、默认视图树形/平铺、全局 Key 分隔符（仅新连接使用）、TTL 徽标、删除确认、自动刷新关闭/30/60 秒。
- `sections/SecuritySection`：全局只读、加密存储密码（关闭时明确“明文风险”，下次保存连接生效）、隐藏敏感值、危险命令二次确认；危险区“清除本地缓存”带确认对话框。
- `sections/ShortcutsSection`：只列出本版本真实可用的快捷键；macOS 显示 Cmd/Shift/Enter，Windows/Linux 显示 Ctrl/Shift（原因见差异表）。
- `sections/AboutSection`：品牌 Logo、0.1.0、技术栈、`~/.redis-compose/` 复制路径、真实 GitHub 仓库链接；不实现“检查更新”。

#### 设置持久化与全局生效

- `AppState.updateSettings`：立即更新内存；300ms debounce 后写 `settings.json`；`SettingsSaveStatus`（SAVED/SAVING/FAILED）与 `lastSettingsSavedAtEpochMillis` 供顶栏、状态栏和页脚显示；保存失败 toast；`settingsSaveDebounceMillis` 可注入以便测试。
- `JsonSettingsRepository.save` 现在把写入异常向上抛出，由 `AppState` 统一提示（读取仍保持损坏回退默认）。
- `RedisComposeApp`：`AppTheme(dark, accent, density)` 接入 `settings.density`；`SYSTEM` 按系统深浅解析。
- `AppTopBar`：设置页显示“已自动保存/保存中/未保存”pill；页面 chip 在设置/连接页显示对应页面名。
- `AppStatusBar`：设置页显示保存状态；全局/连接只读时显示“只读”chip。
- 自动刷新：窗口聚焦且间隔 >0 时，定时刷新所有已连接会话的 `INFO` 摘要与 `INFO keyspace` DB 计数；不刷新 Key 列表；窗口失焦/最小化时停止。
- `AppState.clearLocalCache()`：清空命令历史/最近命令、Key 搜索与类型筛选、洞察采样；不删除连接与设置。
- `ConnectionRepository.setEncryptPasswords` + `JsonConnectionRepository`：关闭加密后下一次保存使用明文；切换本身不改写已有文件。
- `ConnectionsState` 接收 settings provider，`ConnectionDraft.new(separator)` 使用全局默认分隔符。

#### 编辑器 / 值 / 键浏览 / 控制台接入

- `CodeEditor`：读取字号（11–18，行高同步）、显示行号、自动换行；关闭换行时按最长行测量（上限 2000 行 / 4000 字符）放入水平滚动，避免大值卡顿；默认字号/行高与原 Phase 5 行为一致。
- `StringEditor`：初始格式使用 `settings.defaultValueFormat`（二进制仍强制 HEX）。
- `HashEditor`：`maskSensitiveValues` 开启时，字段名匹配 `password|passwd|secret|token|api[_-]?key|private` 的值默认遮罩，行内眼睛按钮逐行显示/隐藏；`CellTextField` 增加 `masked`。
- `WorkspaceState`：新增编辑器设置 getter、`confirmDeleteKeys` 行为（关闭后单 Key 直接删除）、`requestSearchFocus()`；删除确认仍走原对话框。
- `ConsoleState`：新增 `PendingDangerousCommand`；`confirmDangerousCommands` 开启时 FLUSHALL/FLUSHDB/CONFIG SET/SCRIPT FLUSH/EVAL 等进入确认队列，确认后才写终端与历史；只读仍优先拦截。新增 `ConsoleConfirmDialog`。
- `ConsoleScreen` / `CommandComposer`：⌘Enter 执行（输入框内和全局均可）；快捷键提示改用 Cmd/Ctrl 文案。

#### 命令面板扩展

- 页面：工作区、命令控制台、服务器洞察、连接库、设置。
- 操作：新建 Key（无 Keys tab 时先打开）、新建连接（跳转连接库并打开 sheet）、刷新当前 Keyspace、切换深/浅主题、复制当前 Key、打开设置。
- 连接：全部连接，显示 host:port + 真实状态点/文案。
- 最近命令：`settings.recentCommands` 最多 5 条，选择后打开控制台并填入（不直接执行）。
- 600dp / 15dp 圆角 / 14sp 输入 / 36dp 行高 / 9sp 分组；空结果、过滤、↑↓/Enter/Esc 保留。

#### 全局快捷键（`feature/shell/GlobalShortcuts.kt`）

- ⌘/Ctrl+K 命令面板；⌘/Ctrl+F 聚焦当前 Keyspace 搜索；⌘/Ctrl+S 保存当前值；⌘/Ctrl+Enter 执行控制台命令；N 新建 Key；R 刷新 Keyspace；⌘/Ctrl+W 关闭当前 Workspace tab；⌘/Ctrl+Shift+L 切换主题；⌘/Ctrl+, 打开设置；Esc 关闭命令面板/弹层。
- 修饰键快捷键在根节点 preview 阶段处理，因此文本输入中仍可用；N/R 在冒泡阶段处理，并新增 `TextInputFocusTracker`（`design/theme/TextInputFocus.kt`，所有 AppTextField/CellTextField/CodeEditor/Composer/命令面板输入框上报焦点）保证输入时不误触。
- `AppState.hasBlockingOverlay`：命令面板、连接 sheet、Workspace 对话框、控制台危险命令确认期间屏蔽全局快捷键。
- 新增 `util/Platform.kt`（expect/actual `isMacOs`，仅用于快捷键文案）。
- Keys 页的旧本地 N/R/⌘F/⌘S 处理改为全局统一；Esc 仍保留在 WorkspaceScreen（可清搜索/取消选中）。

#### 设计系统补充

- `AppDimens`：新增 `SettingsDimens` 嵌套类（设置页 token）与 `paletteRadius`；嵌套是为了避开 JVM 方法参数 255 上限（顶层 data class 已接近该限制）。
- `AppTypography`：`settingsLabel/settingsHint/settingsSectionTitle/settingsSectionDesc/paletteInput/aboutMono`。
- `AppIcons`：`ThemeSystem`、`Remove`。
- `AppColors`：`swatchDot`。
- `AppTextField`：可选 `onFocusChanged`（数字输入失焦提交）；`AppScrollbar` 新增 `LazyListState` 版本。
- 快捷键 glyph 文案集中到 `util/ShortcutLabels`，避免 `⌘/⇧/↵` 在打包字体中显示为豆腐块。

### 与设计稿 / 计划的有意差异

| 设计稿 / 计划 | 实现 |
| --- | --- |
| 快捷键显示 `⌘` / `⇧` / `↵` | Inter / JetBrains Mono 不含这些字形，实际渲染为方框；统一改为 `Cmd` / `Shift` / `Enter`（Windows/Linux 为 `Ctrl`），表格保持只读 |
| 界面语言为 select（含 English/日本語） | 只读“简体中文”chip + `v1 仅此一项`徽标；不提供无效果的切换 |
| 等宽字体 select（SF Mono / Menlo 等） | 静态 `JetBrains Mono` + `v1 唯一`徽标 |
| 主题卡缩略图固定紫色 | 缩略图使用当前强调色派生色；system 卡左右明暗渐变 |
| “检查更新”按钮 | 未实现（无更新服务）；GitHub 指向真实仓库，不显示许可证（仓库无 LICENSE 文件） |
| 自动刷新“空闲时” | 以窗口聚焦为生命周期；侧栏摘要/DB 计数定时刷新，Key 列表不自动刷新 |
| 关闭加密后“立即重新加密” | 未实现；按计划提示“下次保存连接时生效” |
| 命令面板“新建 Key”要求已有 Workspace | 有活动连接即可；自动打开 Keys tab 再弹出新建对话框 |
| 设置页滚动条 | `LazyListState` 近似滚动条（按可见项数计算 thumb），拖拽按估算像素滚动 |
| 控制台字号跟随编辑器字号 | v1 未接入（计划允许稍后统一） |
| 快捷键表 | 额外列出已实现的 `↑/↓` 历史与 `Cmd ,` 打开设置；不列未实现项 |

### 验证

| 命令 | 结果 |
| --- | --- |
| `./gradlew ktlintCheck` | 通过 |
| `./gradlew :app:jvmTest` | 231 tests，0 failures（Phase 8 基线 216 + 15 个新用例） |
| `./gradlew build` | 通过 |
| `./gradlew :desktopApp:run` | 启动成功，`MainKt` 进程存活 ≥ 20 秒，无应用异常；未点击真实连接 |

新增/更新测试：

- `SettingsLimitsTest`（4）：范围夹取、分隔符回退、敏感字段匹配、危险命令分类。
- `SettingsPersistenceTest`（3，jvm）：Phase 9 全字段 debounce 后重载一致、debounce 只保留最后快照、保存失败状态与 toast。
- `StorageRepositoriesTest`（+1）：关闭加密后下一次保存写入明文并可读回。
- `ConsoleStateTest`（+4）：危险命令待确认、确认执行、取消/关闭设置后直接执行、只读优先拦截、清缓存保留终端日志。
- `WorkspaceEditingTest`（+1）：关闭删除确认后单 Key 直接删除；开启时弹对话框。
- `ConnectionDraftTest`（+1）：新连接使用全局默认 Key 分隔符。
- `AppStateTest`（+1）：清除本地缓存清空最近命令与控制台历史。

离屏渲染（临时 `ImageComposeScene` 工具，验收后已删除临时测试与 `jvmTest` 的 `compose.desktop.currentOs` 依赖）：

- 设置页 1380×816 深/浅、1060×636 紧凑、1380×1650 长页；安全/快捷键 section 定向滚动；全外壳 1440×900 浅色；命令面板 1000×700。
- 检查了主题卡缩略图、选中外环、强调色切换（IRIS/MINT/ROSE）、紧凑行高、危险区、保存状态、快捷键表格与 About；文本无截断/溢出。
- 修正了 `⌘/⇧/↵` 豆腐块、SYSTEM 缩略图面板颜色、`AppDimens` JVM 参数上限问题。

### 真实联调边界

- 本阶段没有连接真实 Redis、没有读取/打印 `~/.redis-compose/connections.json`、没有点击用户真实连接；启动验证只进入默认页面。
- 设置持久化使用临时目录测试；加密开关的明文写入测试同样使用临时目录，不触碰真实文件。
- 全局只读、危险命令确认、删除确认通过状态层测试与离屏 UI 验证，未在真实服务器执行任何写命令。
- 未做自动键盘回放（macOS 未授权 osascript）；⌘/Ctrl 快捷键只做代码级检查与静态界面验证。
- 未在真实窗口验证系统深/浅切换、窗口失焦暂停轮询与真实 Redis 下的自动刷新。

### 遗留风险与边界

1. **关闭前最后一次设置可能丢失**：debounce 300ms 内直接关窗，最后一次修改可能未落盘；Phase 10 可在 `close()` 增加同步 flush 或应用级 scope。
2. **自动刷新生命周期**：依赖 `LocalWindowInfo.isWindowFocused`；窗口失焦/最小化会暂停，但“应用在后台但窗口仍聚焦”的桌面场景仍会轮询；同时会与洞察页轮询叠加 `INFO` 请求。
3. **快捷键自动回放缺失**：N/R 的文本焦点保护通过 `TextInputFocusTracker` 实现，新增文本控件必须调用 `trackTextInputFocus()`；未做真实键盘事件测试。
4. **命令面板滚动条**：`LazyListState` 滚轮/滚动正常，拖拽 thumb 为估算像素，长列表末端可能不够精确。
5. **字体 glyph**：快捷键文案已改为 Cmd/Shift/Enter；设计稿中的符号美感有所牺牲，换取可读性。
6. **加密切换不迁移历史密文**：关闭加密只影响下次保存；已有密文仍保留在文件中，开启后再次保存会重新加密。切换后是否“立即重新加密”留 Phase 10。
7. **控制台字号未统一**：编辑器字号只作用于值编辑器；控制台仍使用固定 terminal 字号。
8. **设置保存失败提示**：`JsonSettingsRepository.save` 由静默失败改为抛出异常，AppState 会 toast；极端磁盘错误下的错误文案仍为异常首行。
9. **全局只读范围**：值编辑器、控制台、新建/删除 Key、TTL/重命名均拦截；连接测试、只读命令与本地设置不受影响（符合计划）。
10. **设置页 section 预览**：各 section 的 `@Preview` 已提供，但未逐个包裹布局容器；实际页面已通过整页离屏渲染验证。
11. **依赖/源码体积**：新增设置 token 使用嵌套 `SettingsDimens`；后续继续向 `AppDimens` 顶层加字段会再次触及 JVM 255 参数上限，Phase 10 应考虑拆分更多嵌套组。
12. **测试与预览 fixture**：`SettingsPersistenceTest` 使用真实 `JsonSettingsRepository` + 临时目录；未新增 UI 测试框架。

---

## Phase 10 — 打磨与发布准备（已验收）

发布版本：**0.1.0**（macOS 安装包 bundle version 为 1.0.0，见下）。

### 完成内容

#### 清理

- 删除 `design/gallery/DesignGalleryScreen.kt`（Phase 1 临时组件画廊，运行时已不可达）。
- 移除未使用的依赖：`androidx.lifecycle:lifecycle-*`、`junit`、`kotlin-test-junit`；`libs.versions.toml` 同步删除条目。
- 清理代码注释中的阶段编号与“gallery / Phase placeholder”描述；删除 UI 中“将在后续版本提供”的承诺式文案，改为明确的 v1 限制。
- 确认不存在第二套主题/AppState/图标；`com.czh.redis` 旧包只存在于 `plan/` 历史记录。
- `AppInfo` 集中管理产品名、版本（0.1.0）、短版本（v0.1）、配置目录与仓库 URL；窗口标题、顶栏、状态栏、设置/关于统一读取。
- 预览 fixture 仍随生产源集编译（仅 `@Preview` 使用，不参与运行时）；未迁移到独立 tooling 源集，已列为后续改进。
- `design/`、`plan/` 保留，未删除。

#### 构建与打包

- `settings.gradle.kts` 仅包含 `:app` 与 `:desktopApp`。
- `desktopApp` 仅依赖 `:app` + Compose Desktop + coroutines-swing + preview。
- `desktopApp/build.gradle.kts`：
  - `packageName = RedisCompose`、`bundleID = com.czh.rediscompose`、`dockName = Redis Compose`；
  - target `Dmg / Msi / Deb`，macOS/Windows/Linux 菜单分组；
  - 说明 jpackage 版本限制：bundle version 使用 `1.0.0`，产品内版本仍为 `0.1.0`。
- 成功生成 macOS DMG：`desktopApp/build/compose/binaries/main/dmg/RedisCompose-1.0.0.dmg`。
- Windows MSI / Linux DEB 目标已配置，本轮未在对应平台执行；未配置代码签名/公证。

#### 视觉与交互打磨

- 离屏渲染检查 5 个页面在 1440、1200/1060、1120/802 内容宽度、深/浅主题、空/数据状态下的布局。
- 修复/改进：
  - `AppButton` / `AppIconButton` 增加键盘焦点边框（accent），主要操作 Tab 聚焦可见；
  - 控制台每条日志包裹 `SelectionContainer`，日志可选中复制；
  - 连接库空状态说明改为“密码按安全设置加密或明文存储”，不再默认写死加密；
  - Sentinel / Cluster 文案从“后续版本提供”改为“v1 暂不支持”；
  - 长 Key、长连接名、长 Host 检查省略/换行，无横向溢出；
  - 品牌版本、状态栏、关于页统一使用 `AppInfo`。
- 全部 Phase 占位文案与临时画廊已移除。

#### 性能与稳定性检查

- 代码检查通过：Key 列表/集合编辑器 `LazyColumn`；TYPE/TTL 只对可见行懒加载（并发 6）；搜索 300ms debounce；切换 Key 取消旧请求。
- 大值：默认按 `truncateValueKb` 截断展示，保留完整复制/“仍然编辑”；保存有 `isSaving` 防重复。
- 控制台：entry 500、输出数组 200 项/深度 3、单结果 600 行等上限保留。
- 洞察：5 秒采样、720 条/1 小时上限、离开 tab `stop()`、关闭 tab `dispose()`。
- 设置：300ms debounce；`AppState.close()` 新增退出前 flush 待写设置，避免最后 300ms 修改丢失。
- 关闭 tab/连接/窗口会取消 session、feature scope 与 gateway；自动摘要刷新在窗口失焦时停止。
- 除组合根启动时读取小体积 settings/connections（Plan §8.1 允许）外，无主线程网络/大文件 IO。

#### 可访问性

- 图标按钮强制 `contentDescription`；Rail/工具栏有 tooltip。
- 主要按钮/图标按钮新增焦点边框；文本输入保持焦点环；Esc 关闭顶层弹层。
- 值 Key 标题、控制台日志、值编辑器可选中复制；状态与错误使用“文本 + 颜色”双通道。
- 深/浅主题正文与次级文本在离屏渲染中检查对比度。
- 未完成：全量 Tab 顺序键盘回放、屏幕阅读器语义树检查（需要人工/专用工具）。

#### 测试

- 新增 `TextInputFocusTrackerTest`：输入焦点计数与负数保护，支撑 N/R 不误触。
- 新增 `SettingsPersistenceTest` 退出 flush 用例：debounce 未完成时 `close()` 仍写入最后快照。
- 既有测试全部保留并通过。

#### 文档

- `README.md` 重写：功能、环境、构建/运行/打包、配置目录、快捷键、模块结构、架构约定、已知限制。
- `AGENTS.md` 重写：真实技术栈、模块/包结构、依赖方向、状态/持久化/设计系统/Compose/测试约定、AppDimens 参数上限、快捷键 glyph 与文本焦点追踪注意事项。
- 新增 `plan/QA-CHECKLIST.md`：构建门禁、Redis 测试数据准备、手动走查表、视觉/性能/可访问性/打包结果与未执行项。
- `plan/README.md` 与 `plan/STATUS.md` 更新 Phase 10 为已验收。

### 验证

| 命令 | 结果 |
| --- | --- |
| `./gradlew clean build` | 通过 |
| `./gradlew ktlintCheck` | 通过 |
| `./gradlew :app:jvmTest --rerun-tasks` | 233 tests，0 failures |
| `./gradlew :desktopApp:run` | 启动成功，`MainKt` 进程存活 ≥ 20 秒，无异常 |
| `./gradlew :desktopApp:packageDistributionForCurrentOS` | 通过，生成 `RedisCompose-1.0.0.dmg` |

离屏渲染（临时 `ImageComposeScene`，验收后已删除）：

- Key 浏览器 1074×770 深/浅、802×590 深、超长 Key；控制台 1074×770 深/浅、802×590 浅。
- 连接库 1380×816 深/浅、1060×636 浅、超长名称/Host；洞察 1074×770 深、802×590 浅。
- 设置 1380×816 深、1060×636 浅；外壳 1120×720 空状态深、1440×900 连接库浅。
- 检查结果：无横向溢出；长文本省略/换行；窄宽下工具栏换行、meta grid 2 列、历史栏按阈值隐藏。

### 真实联调边界

- 本轮 Agent 环境没有 Docker、`redis-server` 或 `redis-cli`，未连接真实 Redis，未读取/打印用户 `connections.json`，未点击真实连接。
- 真实 Redis 的首次连接、写路径、10k Key 滚动、1MB+ 值、SSH/TLS 网络握手仍需按 `plan/QA-CHECKLIST.md` 在本机/CI 补跑。
- macOS 未授权自动按键，键盘快捷键与焦点顺序只做代码/离屏验证，未做自动回放。
- 未在 Windows/Linux 实机验证打包、窗口最小尺寸与字体回退。

### 遗留风险与边界

1. **真实 Redis 手动回归未执行**：所有 Redis 读写路径依赖既有单测与 fake gateway；发布前应按 QA 清单跑一次真实实例。
2. **SSH 主机密钥不校验**：v1 `StrictHostKeyChecking=no`，存在中间人风险；未实现 known_hosts/首次确认。
3. **TLS 私钥格式有限**：仅未加密 PKCS#8 / RSA PKCS#1；加密私钥、EC SEC1、PKCS#12 明确拒绝。
4. **未支持 Sentinel / Cluster / 导入导出 / 检查更新 / 自定义快捷键 / i18n**；README 已明确。
5. **洞察无历史基线**：只有本次会话 1 小时/720 样本；关闭 tab 丢弃，符合设计取舍。
6. **Stream 只读**，最近 50 条，无 XADD/消费者组管理。
7. **控制台字号未跟随编辑器字号**；值编辑器字号已生效。
8. **滚动条近似**：设置/命令面板的 Lazy 滚动条 thumb 与拖拽为估算，滚轮与常规滚动正常。
9. **焦点与屏幕阅读器**：主要按钮/输入已可聚焦并有视觉焦点；未做全量 Tab 顺序与语义树审计。
10. **macOS 包版本**：jpackage 要求首段非 0，安装包显示 1.0.0，应用内 0.1.0；后续发布应统一版本策略（例如产品直接使用 1.x）。
11. **无应用图标/签名/公证**：使用 Compose 默认图标；发布商店或对外分发前需补齐品牌图标与签名。
12. **预览 fixture / 工具**：`*Previews.kt` 仍在生产源集，Phase 10 未迁移；临时渲染测试已删除。
13. **启动读取**：组合根同步读取小文件以保证首帧主题正确；若未来连接/设置文件变大，应改为启动 loading。
14. **设置 flush**：`close()` 使用短 `runBlocking` 写入本地小文件，极端磁盘阻塞时退出可能延迟；不会影响正常运行。
15. **自动刷新**：窗口聚焦时每 30/60 秒刷新摘要与 DB 计数，会与洞察页轮询叠加 `INFO` 请求。

### 后续建议（非本阶段范围）

- Sentinel / Cluster 支持、连接导入/导出。
- 洞察跨会话历史与告警规则（需要持久化采样）。
- SSH known_hosts、SSH agent/ProxyJump；TLS 私钥格式扩展。
- 自定义快捷键、命令收藏持久化、批量删除 Key。
- 品牌应用图标、代码签名/公证、Windows/Linux 打包 CI。
- 全量键盘/屏幕阅读器自动化测试；预览 fixture 移到 tooling 源集。

---

## 当前基线

- 分支：`feat/rewrite-ui`；模块：`:desktopApp` + `:app`；发布版本 **0.1.0**。
- 包名：`com.czh.rediscompose`；入口：`com.czh.rediscompose.desktop.MainKt`；窗口标题 `Redis Compose`。
- Phase 0–10 全部 `已验收`；测试 **233/233** 通过；`clean build`、`ktlintCheck`、`desktopApp:run` 通过。
- 功能：Keys / Console / Insights / Connections / Settings 全部接入真实网关/设置；TLS/SSH 隧道可用；命令面板与全局快捷键完成。
- 打包：macOS DMG `desktopApp/build/compose/binaries/main/dmg/RedisCompose-1.0.0.dmg`（jpackage bundle version 1.0.0；应用内版本 0.1.0）。
- 文档：`README.md`（功能/构建/配置/限制）、`AGENTS.md`（架构与约定）、`plan/QA-CHECKLIST.md`（发布验收）。
- 发布前仍需：真实 Redis 手动回归、Windows/Linux 打包、应用图标与签名/公证、键盘/屏幕阅读器人工检查。
