# Redis Compose — 分阶段实施计划（给 AI Agent）

> 本目录是 **实施合同**，不是一次性需求单。每个阶段都是一个可独立交给 AI Agent 的任务包：目标、边界、文件、接口、验收都写清楚。
>
> 设计参考在 `design/`。预览入口：`design/index.html`。

---

## 0. 先读这里

### 0.1 这次是重写，不是改样式

用户明确对现有 UI、目录结构、命名不满意，**允许完全抛弃旧代码**。因此：

- **不要求**兼容旧包名、旧组件、旧 `AppViewModel`、旧 `ui/` 结构。
- **不要求在中间阶段保持功能完整**。阶段 0–10 的应用允许暂时缺少功能，只要每个阶段能编译、能启动、有一个可见的阶段性成果。
- 旧代码只在阶段 0 作为“可参考但不复用”的实现被删除；需要重写的 Redis 逻辑在新架构里重新实现。
- `design/` 与 `plan/` 不属于运行时，不参与打包，不要删除。

### 0.2 最终目标架构（已拍板）

| 项 | 决定 |
| --- | --- |
| UI | Compose Multiplatform / **Compose Desktop** |
| Gradle 模块 | `:desktopApp`（入口）+ `:app`（原 `:shared` 重命名） |
| 包名 | `com.czh.rediscompose` |
| 桌面入口 | `com.czh.rediscompose.desktop.MainKt` |
| 状态管理 | 手写 `AppState` + 每页 `XxxState`，使用 `mutableStateOf` + `CoroutineScope`；**不引入** Decompose / Voyager / Koin / Hilt |
| 导航 | `AppPage` + `WorkspaceTab`，不引入 Navigation 库 |
| 数据层 | `RedisGateway` 接口 + Jedis 实现；Repository 接口 + JSON 文件实现 |
| 持久化 | `~/.redis-compose/settings.json`、`connections.json`，kotlinx.serialization |
| 图标 | Material Icons **Outlined**，通过 `AppIcons` 语义对象集中映射；品牌 Logo 自绘 |
| 字体 | Inter（UI）+ JetBrains Mono（代码），打包进 Compose Resources；失败时用系统回退 |
| 图表 | Compose Canvas 自绘，不引入图表库 |
| 颜色 | 只允许来自 `AppColors`，禁止在 feature 里写 `Color(0x...)` |
| 文案 | 中文界面 + 英文技术术语；集中常量仅用于重复标签，不做 i18n 框架 |

### 0.3 目录目标

```text
redis-compose/
├── desktopApp/                         # 只放桌面入口与 JVM 容器创建
│   └── src/main/kotlin/com/czh/rediscompose/desktop/
│       └── Main.kt
├── app/                                # KMP 应用库（由 shared 重命名）
│   └── src/
│       ├── commonMain/kotlin/com/czh/rediscompose/
│       │   ├── app/                    # RedisComposeApp, AppState, AppContainer, AppPage
│       │   ├── domain/
│       │   │   ├── connection/         # ConnectionProfile 等
│       │   │   ├── repository/         # PasswordCipher 等
│       │   │   ├── key/                # KeyType, KeyNode, KeyTreeBuilder
│       │   │   ├── command/            # RedisReply 等
│       │   │   ├── server/             # ServerSnapshot 等
│       │   │   └── settings/           # AppSettings 等
│       │   ├── data/
│       │   │   └── redis/              # RedisGateway, RedisReply, ValueCodec, InfoParser（接口/纯逻辑）
│       │   ├── design/
│       │   │   ├── theme/              # AppTheme, AppColors, AppTypography, AppDimens, AppShapes
│       │   │   ├── icon/               # AppIcons
│       │   │   ├── component/          # AppButton, AppTextField, AppSheet, AppToast...
│       │   │   └── text/               # JsonHighlighter 等文本处理
│       │   ├── feature/
│       │   │   ├── shell/
│       │   │   ├── workspace/
│       │   │   ├── console/
│       │   │   ├── insights/
│       │   │   ├── connections/
│       │   │   └── settings/
│       │   └── util/                   # Formatters, RedisCommandParser, WriteCommandGuard 等纯函数
│       ├── commonMain/composeResources/
│       │   ├── font/                   # Inter / JetBrains Mono
│       │   └── drawable/               # 品牌占位资源（可选）
│       ├── jvmMain/kotlin/com/czh/rediscompose/
│       │   ├── DesktopAppContainer.kt
│       │   ├── data/jedis/ ...
│       │   └── data/storage/ ...
│       ├── commonTest/
│       └── jvmTest/
├── design/                             # HTML 设计参考（不参与构建）
├── plan/                               # 本目录
└── gradle/
```

### 0.4 包边界规则

- `feature/**` 只能依赖 `design/**`、`domain/**`、`app/AppState`、`util/**`。
- `feature/**` **不能**直接依赖 `data/jedis`、`data/storage`，也不能直接 import Jedis。
- 已知例外：Phase 5–6 的 state/编辑器和预览 fixture 仍引用 `data/redis` 的纯编解码 `ValueCodec` 与 `RedisGateway` 接口；Phase 10 评估把这些纯类型下沉到 `util`/`domain`，不把 Jedis/文件存储带进 feature。
- `data/**` 实现 `domain/**` 的接口。
- `design/**` 不能依赖 `feature/**` 或 `data/**`。
- `app/**` 是组合根：持有 container、创建 state、把数据传给 feature。
- `util/**` 只放纯函数，不依赖 Compose UI。返回 `AnnotatedString` 的 JSON 高亮放在 `design/text/JsonHighlighter.kt`。

---

## 1. 阶段总览

| 阶段 | 文档 | 产出 | 依赖 | 状态 |
| --- | --- | --- | --- | --- |
| 0 | [`00-bootstrap.md`](00-bootstrap.md) | 模块/包重置，可启动的空壳 | — | 已验收 |
| 1 | [`01-design-system.md`](01-design-system.md) | 主题、字体、图标、基础组件、组件画廊 | 0 | 已验收 |
| 2 | [`02-domain-data.md`](02-domain-data.md) | 领域模型、Redis Gateway、持久化、加密 | 0 | 已验收 |
| 3 | [`03-app-state-shell.md`](03-app-state-shell.md) | AppState、会话、外壳、导航、连接侧栏、状态栏、基础命令面板 | 1, 2 | 已验收 |
| 4 | [`04-workspace-keys.md`](04-workspace-keys.md) | Key 浏览器：树、搜索、筛选、置顶、空/错状态 | 3 | 已验收 |
| 5 | [`05-workspace-values.md`](05-workspace-values.md) | 值检查器、6 种类型编辑器、Key 操作 | 4 | 已验收 |
| 6 | [`06-console.md`](06-console.md) | 命令控制台、历史、补全、结果渲染 | 3, 5 | 已验收 |
| 7 | [`07-insights.md`](07-insights.md) | 实时指标、图表、慢查询、健康检查 | 3 | 已验收 |
| 8 | [`08-connections.md`](08-connections.md) | 连接库、筛选、新建/编辑侧滑、测试连接 | 3 | 已验收 |
| 9 | [`09-settings-palette.md`](09-settings-palette.md) | 设置页、主题/强调色/密度、命令面板扩展、全局快捷键 | 5, 8 | 已验收 |
| 10 | [`10-polish-release.md`](10-polish-release.md) | 全量打磨、测试、性能、删除遗留、文档 | 全部 | 未开始 |

> 状态以 [`STATUS.md`](STATUS.md) 为准；本表只同步粗粒度结果，完成内容、验证命令与遗留风险见各阶段章节。

**不要跳阶段。** 每个阶段结束必须停下，更新 `STATUS.md`，向用户报告，不要自行开始下一阶段。

---

## 2. AI Agent 工作协议

### 2.1 每次只做一个阶段

1. 读 `plan/README.md`、本阶段文档、相关 reference 文档。
2. 只修改本阶段文档允许的文件；不要顺手重构其他页面。
3. 保持 `./gradlew build`、`./gradlew ktlintCheck` 通过。
4. 运行本阶段验收命令；无法运行视觉验收时，按 `reference/acceptance.md` 做代码级自检。
5. 更新 `plan/STATUS.md`：状态、完成内容、遗留风险。
6. 停止，不开始下一阶段。

### 2.2 不允许的行为

- 不要把旧 `com.czh.redis.ui` 的代码复制到新包“先跑起来”。
- 不要新增框架/依赖（除了阶段文档明确列出的）。
- 不要在 feature 里写硬编码颜色、圆角、字号；使用 `AppColors` / `AppDimens` / `AppTypography`。
- 不要在 composable 里直接发 Redis 请求；通过 state holder。
- 不要用 `runBlocking` 做网络请求。
- 不要为了“视觉接近”而写死窗口尺寸或假数据；真实数据不可得时，使用明确标注的 empty/loading 状态。
- 不要实现文档“非目标”里的功能。

### 2.3 每个阶段的完成定义

- 代码可编译，应用可启动。
- 新增公开 composable 有 `@Preview`（纯桌面窗口级页面可例外，但要有静态预览或用 gallery 验证）。
- 新逻辑至少有必要的纯函数单测（树构建、格式化、解析、序列化）。
- `ktlintCheck` 通过。
- 没有未处理的 TODO 扩散；确需后续阶段处理的，用 `TODO(phase-N): ...` 标注。
- 视觉上至少覆盖：默认、hover/selected、empty、loading、error 五类状态中的相关部分。

---

## 3. 设计参考的使用方式

- 页面结构、间距、颜色、文案、状态：`design/*.html` + `design/assets/theme.css` + `design/assets/pages.css`。
- 组件尺寸、字体、颜色 token：`plan/reference/design-tokens.md`。
- 数据模型与接口：`plan/reference/domain-and-data.md`。
- 编码规范：`plan/reference/coding-standards.md`。
- 验收清单：`plan/reference/acceptance.md`。

**以设计 HTML 为准，但以本计划的技术决策为准。** 两者冲突时：

1. 安全/正确性优先；
2. Compose/桌面可用性优先；
3. 视觉保真最后。

例如：设计稿里的 24 小时历史图表，Redis 本身不提供历史指标；阶段 7 用“本次会话采样”替代，这是有意偏离，不是 bug。

---

## 4. 环境与命令

```bash
# 编译
./gradlew build

# 代码风格
./gradlew ktlintCheck

# 单元测试（模块重命名后）
./gradlew :app:jvmTest

# 运行桌面应用
./gradlew :desktopApp:run

# 开发热重载（如果现有工程支持）
./gradlew :desktopApp:hotRun --auto
```

运行要求：JDK 由 Gradle toolchain 处理；桌面环境 macOS / Windows / Linux。

---

## 5. 关键决策记录（ADR 摘要）

| # | 决策 | 原因 |
| --- | --- | --- |
| 1 | 完全重写 UI 与状态层，删除旧 `com.czh.redis` 包 | 旧结构与命名不再维护；避免边改边妥协 |
| 2 | `shared` 重命名为 `app`，包名改为 `com.czh.rediscompose` | 命名表达真实职责；一次性切干净 |
| 3 | 不引入 DI / 导航 / 图表框架 | 规模可控；减少 Agent 误用与版本风险 |
| 4 | 自绘主题与基础组件，保留 Material3 仅作底层 | 设计语言与 Material 差异较大；统一 token 才能保证还原 |
| 5 | 使用 Material Icons Outlined，经 `AppIcons` 映射 | 比手写 60 个 ImageVector 更稳定；后续可无痛替换 |
| 6 | 指标页采用会话内采样，不伪造 24h 历史 | Redis 无历史数据；宁可诚实显示“自本次连接” |
| 7 | 密码加密放在数据层，UI 只传明文 profile | 安全边界清晰；后续可替换系统钥匙串 |
| 8 | 保留 KMP 结构（commonMain/jvmMain） | 当前只做桌面，但未来可扩平台；不引入多余模块 |

---

## 6. 风险与处理

| 风险 | 处理 |
| --- | --- |
| 模块重命名导致 Gradle/IDE 配置失效 | 阶段 0 同时更新 `settings.gradle.kts`、`desktopApp/build.gradle.kts`、mainClass、packageName；跑完整 build |
| 字体与中文字形 | Inter / JetBrains Mono 已打包进 Compose Resources；Inter 不含 CJK，中文由系统字体回退（macOS 为 PingFang SC）。若需完全统一字形，Phase 10 再评估打包 CJK 字体 |
| Material Icons 版本缺少个别图标 | `AppIcons` 里选最接近的 Outlined 图标；feature 代码不直接引用 Material 图标名 |
| 旧连接的 JSON 数据格式不同 | `ConnectionProfile` 字段保持宽松；`Json { ignoreUnknownKeys = true }`；Phase 3 已覆盖 pre-rewrite 的字符串端口/扁平字段等格式，Phase 10 再做长期迁移与清理 |
| 大 Key / 大值 / 大输出卡顿 | 值截断、集合 LazyColumn、SCAN 分批、控制台条目/数组/行数上限、IO 不在主线程（详见 acceptance 与 STATUS.md） |
| AI Agent 跳阶段 | 每份阶段文档首尾都有 "STOP" 约束；`STATUS.md` 作为门禁 |

---

## 7. 交给 Agent 的推荐提示词

```text
你正在实现 Redis Compose 的 Phase N。

先读：
- plan/README.md
- plan/NN-<phase>.md
- plan/reference/design-tokens.md
- plan/reference/domain-and-data.md
- plan/reference/coding-standards.md
- plan/reference/acceptance.md

只实现 Phase N 的范围，不实现后续阶段，不保留旧 com.czh.redis UI。
每个任务完成后运行 ./gradlew build 和 ./gradlew ktlintCheck。
结束时更新 plan/STATUS.md，并停下来汇报：完成项、验证结果、已知风险、下一阶段需要的前置条件。
```
