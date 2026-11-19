# Redis Compose — AGENTS.md

## 项目概述

Redis Compose 是一个 **Compose Desktop** 的 Redis 桌面工作台，使用 Kotlin Multiplatform 共享 UI 与业务逻辑。当前只构建 JVM/Desktop，但保留 KMP 结构以便未来扩展。

核心能力：键浏览、6 种值编辑器、命令控制台、服务器洞察、连接库（TLS/SSH）、设置与命令面板。

## 技术栈

| 分类 | 技术 | 版本 |
| --- | --- | --- |
| 语言 | Kotlin | 2.4.0 |
| UI | Compose Multiplatform | 1.11.1 |
| 设计系统 | Material3 | 1.11.0-alpha07 |
| 图标 | Material Icons Extended（Outlined，经 `AppIcons`） | 1.7.3 |
| 协程 | kotlinx.coroutines | 1.11.0 |
| 序列化 | kotlinx.serialization JSON | 1.9.0 |
| Redis | Jedis | 5.2.0 |
| SSH | JSch (mwiede) | 2.28.8 |
| 构建 | Gradle Kotlin DSL | 9.6.0 |
| 目标 | Desktop (JVM) | JDK 17+ |

## 模块

```text
redis-compose/
├── desktopApp/src/main/kotlin/com/czh/rediscompose/desktop/Main.kt
├── app/src/
│   ├── commonMain/kotlin/com/czh/rediscompose/
│   │   ├── app/          # RedisComposeApp, AppState, RedisSession, AppContainer, AppPage, AppInfo
│   │   ├── domain/       # connection / key / command / server / settings / repository
│   │   ├── data/redis/   # RedisGateway 接口、ValueCodec、InfoParser、Reply/SlowLog/ClientList 解析
│   │   ├── design/       # theme / icon / component / text
│   │   ├── feature/      # shell / workspace / console / insights / connections / settings
│   │   └── util/         # Formatters、命令解析、WriteCommandGuard、SettingsLimits 等纯函数
│   ├── commonMain/composeResources/font/   # Inter + JetBrains Mono
│   ├── jvmMain/          # Jedis/Ssh/Tls 实现、JSON 存储、AesGcm、DesktopAppContainer
│   ├── commonTest/
│   └── jvmTest/
├── desktopApp/icons/     # 应用图标（ICNS / ICO / PNG）
├── scripts/              # 图标生成等本地脚本
├── design/               # HTML 视觉参考（不参与构建）
├── plan/                 # 阶段合同、参考与验收记录
└── gradle/libs.versions.toml
```

允许的依赖方向：

```text
desktopApp -> app
app/app    -> feature, design, domain, data, util
feature    -> design, domain, util, app/AppState（只读 + 回调）
design     -> domain（只读类型）/ util
data       -> domain
domain     -> 无（纯 Kotlin）
util       -> 无 UI
```

- feature **不能** import Jedis 或 `data/storage`；通过 `RedisGateway` 与组合根回调。
- `design` 不能依赖 `feature` / `data`。
- `app` 是唯一组合根：创建 container、AppState、会话与 feature state。

## 命令

```bash
./gradlew build
./gradlew ktlintCheck
./gradlew :app:jvmTest
./gradlew :desktopApp:run
./gradlew :desktopApp:hotRun --auto
./gradlew :desktopApp:packageDistributionForCurrentOS

# 重新生成品牌图标（Pillow + macOS iconutil）
python3 scripts/generate-app-icon.py
```

- 打包：macOS 生成 DMG（jpackage bundle version 1.0.0，应用版本 0.1.0）；Windows/Linux 需在对应平台执行。
- 图标：`desktopApp/icons/app-icon.icns|ico|png`，由 `scripts/generate-app-icon.py` 生成（颜色与形状对应 `BrandMark`）。
- 不提交 `build/`、`.gradle/`、临时截图。

## 架构与约定

### 状态

- 手写 `AppState` + 每页 `XxxState`：`mutableStateOf` / `mutableStateListOf` / `mutableStateMapOf` + 注入的 `CoroutineScope`。
- 不引入 Decompose、Voyager、Koin、Hilt、Navigation 或图表库。
- State holder 不依赖 Compose UI；Composable 只读状态并调用方法。
- 所有长任务可取消；关闭 tab/连接时 dispose 对应 state 和 gateway。
- Redis 调用必须在 `Dispatchers.IO` 上（`JedisRedisGateway` 内部处理），UI 不直接发起。

### 持久化

- `~/.redis-compose/settings.json`、`connections.json`、`secret.key`。
- JSON：`ignoreUnknownKeys`、pretty print、临时文件 + 原子 move。
- 密码：AES-256-GCM，`enc:v1:` 前缀；关闭加密后下一次连接保存写明文。
- 设置：内存即时更新，300ms debounce 写文件；退出时 flush 待写快照。
- `initialSettings` / `initialConnections` 在组合根同步读取（文件很小），用于首帧正确主题。

### 设计系统

- 只用 `LocalAppColors` / `LocalAppDimens` / `LocalAppTypography`；禁止 feature 内 `Color(0x...)`、裸 `sp`、魔法圆角。
- 图标只写 `AppIcons.*`；不要直接使用 `Icons.Outlined.*`。
- `AppDimens` 顶层 data class 已接近 JVM 255 参数上限；新的分组请放进嵌套 data class（如 `SettingsDimens`）。
- 快捷键文案使用 `util/ShortcutLabels` 的 Cmd/Shift/Enter；打包字体不含 `⌘/⇧/↵`。
- 通用组件位于 `design/component/`；不要重复造按钮/输入框/开关/对话框。

### Compose

- 公开 Composable：必填参数 → 可选参数 → `modifier: Modifier = Modifier` 最后（slot 除外）。
- 每个公共组件/屏幕提供深/浅 `@Preview`；预览使用 fake state/remember，不连接 Redis、不读用户文件。
- 列表使用 `LazyColumn`/`LazyRow`；长 Key、值、日志、命令必须截断/省略。
- 文本输入需调用 `Modifier.trackTextInputFocus()`（`design/theme/TextInputFocus.kt`），保证 N/R 等单键快捷键不在输入时误触。
- 颜色/状态不能只靠颜色表达；图标按钮必须有 `contentDescription`/tooltip。

### 测试

- 纯函数必须有单元测试：解析、格式化、树构建、校验、命令分类、设置范围、指标过滤。
- fake gateway 位于 `commonTest/app/FakeBackend.kt`。
- 测试不依赖网络、不依赖真实 Redis，使用临时目录写文件。
- 常用命令：`./gradlew :app:jvmTest ktlintCheck build`。

## 已知限制

详见 `README.md`：洞察仅会话采样、Stream 只读、集合内非 UTF-8 成员编辑风险、SSH 主机密钥不校验、TLS 私钥格式限制、未支持 Sentinel/Cluster/导入导出、控制台字号未跟随编辑器、近似滚动条。

## 参考

- 计划入口：`plan/README.md`
- 状态与风险：`plan/STATUS.md`
- QA 清单：`plan/QA-CHECKLIST.md`
- 设计 token：`plan/reference/design-tokens.md`
- 数据/接口契约：`plan/reference/domain-and-data.md`
