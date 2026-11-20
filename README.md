# Redis Compose

一个面向桌面的 Redis 工作台：Key 浏览器、6 种值编辑器、命令控制台、服务器洞察、连接库与完整设置页。使用 **Kotlin Multiplatform + Compose Desktop** 构建，界面与状态层完全重写，不依赖 Redis GUI 框架。

> 设计参考在 `design/`，分阶段实施与验收记录在 `plan/`。两者都不参与打包。

## 功能

- **键浏览**：SCAN 分页、模式搜索、类型筛选、树形/平铺、置顶、TTL 标记、类型色；只对可见行懒加载 TYPE/TTL。
- **值检查与编辑**：String（Auto/JSON/Text/HEX，大值截断与只读 HEX）、Hash、List、Set、ZSet、Stream（只读）；TTL、重命名、删除、新建 5 种 Key。
- **命令控制台**：命令历史、补全、嵌套结果渲染、只读拦截、危险命令二次确认；输出有行数/深度/条目上限。
- **服务器洞察**：会话内 5 秒采样、吞吐/内存/命中率/连接数、命令分布、慢查询、客户端分组、复本/RDB/AOF/内存健康；离开页面停止轮询。
- **连接库**：搜索、环境筛选、网格/列表、最近连接、新建/编辑侧滑、测试连接；支持 TLS 与 SSH 隧道。
- **设置**：深/浅/跟随系统、5 种强调色、舒适/紧凑密度、编辑器格式/字号/换行/行号、SCAN 批量、Key 分隔符、TTL、删除确认、自动刷新、全局只读、密码加密、敏感值遮罩、危险命令确认、命令历史清理。
- **命令面板与快捷键**：页面/操作/连接/最近命令统一搜索；Cmd/Ctrl+K、F、S、Enter、W、Shift+L、逗号等。
- **快捷键的文本输入保护**：输入框聚焦时不会误触 N/R 等单键操作。

## 环境

- JDK 17+（推荐 JDK 21；Gradle 会通过 toolchain resolver 获取打包所需 JDK）
- macOS / Windows / Linux 桌面
- 首次运行不需要 Redis；没有连接时显示空状态

## 构建与运行

```bash
# 编译 + 测试
./gradlew build

# 代码风格
./gradlew ktlintCheck

# JVM 单元测试
./gradlew :app:jvmTest

# 启动桌面应用
./gradlew :desktopApp:run

# 热重载开发（如 IDE 支持）
./gradlew :desktopApp:hotRun --auto

# 打包当前平台安装包
./gradlew :desktopApp:packageDistributionForCurrentOS
```

macOS 产物：`desktopApp/build/compose/binaries/main/dmg/RedisCompose-1.0.0-arm64.dmg`（CI）或 `RedisCompose-1.0.0.dmg`（本地打包）。

Windows/Linux 分别生成 MSI/DEB；需在对应平台执行打包任务。

## 发布

推送 `v*` 标签会触发 `.github/workflows/build.yml`：

1. 在 macOS（Apple Silicon + Intel）、Ubuntu、Windows 上分别运行 `./gradlew build`（代码风格、测试、桌面端编译）；
2. 打包 macOS DMG（`-arm64` / `-x64`）、Linux DEB、Windows MSI；
3. 创建 GitHub Release，附带四个安装包与 `SHA256SUMS.txt`；`v0.2.0-beta.1` 这类标签会自动标记为预发布。

```bash
git tag v0.1.0
git push origin v0.1.0
```

普通 `main` push 和 PR 只做代码风格检查与三平台测试，不生成安装包；需要临时产物时在 Actions 里手动运行 `workflow_dispatch`。安装包当前未做 macOS 公证或 Windows 代码签名。

## 配置目录

```text
~/.redis-compose/
├── settings.json      # 主题、编辑器、安全等设置
├── connections.json   # 连接配置（密码按设置加密或明文）
└── secret.key         # AES-256-GCM 密钥（仅当前用户可读）
```

- 设置修改即时生效，300ms 防抖写入；退出时刷新最后一次待写入设置。
- 关闭“加密存储密码”后，**下一次保存连接**才会以明文写入，不会自动重写已有文件。
- 旧版 `settings.json` / `connections.json` 字段会在读取时尽量兼容并回退默认值。

## 键盘快捷键

| 动作 | macOS | Windows / Linux |
| --- | --- | --- |
| 命令面板 | Cmd K | Ctrl K |
| 搜索当前 Keyspace | Cmd F | Ctrl F |
| 保存当前值 | Cmd S | Ctrl S |
| 执行控制台命令 | Cmd Enter | Ctrl Enter |
| 新建 Key | N（非输入焦点） | N |
| 刷新 Keyspace | R（非输入焦点） | R |
| 关闭当前 Tab | Cmd W | Ctrl W |
| 切换深/浅主题 | Cmd Shift L | Ctrl Shift L |
| 打开设置 | Cmd , | Ctrl , |
| 关闭弹层 | Esc | Esc |

> Inter / JetBrains Mono 不含 `⌘`、`⇧`、`↵` 字形，界面统一使用 Cmd/Shift/Enter 文案，避免显示为方框。

## 模块

| 模块 | 说明 |
| --- | --- |
| `desktopApp` | 桌面入口、窗口、jpackage 打包配置 |
| `app` | KMP 共享代码：`app/`、`domain/`、`data/`、`feature/`、`design/`、`util/` |

```text
app/src/
├── commonMain/kotlin/com/czh/rediscompose/
│   ├── app/          # AppState、RedisSession、组合根
│   ├── domain/       # 连接、Key、命令、服务端、设置模型
│   ├── data/redis/   # Gateway 接口、编解码、INFO/慢查询/CLIENT 解析
│   ├── design/       # 主题 token、图标、通用组件、文本高亮
│   ├── feature/      # shell / workspace / console / insights / connections / settings
│   └── util/         # 格式化、命令解析、安全判定等纯函数
├── jvmMain/          # Jedis/SSH/TLS 实现、JSON 存储、桌面容器
├── commonTest/
└── jvmTest/
```

- 状态管理为手写 `AppState` + 每页 `XxxState`（`mutableStateOf` + `CoroutineScope`），不使用 Decompose/Voyager/Koin/Hilt。
- 导航使用 `AppPage` + `WorkspaceTab`，不使用 Navigation 库。
- 数据通过 `RedisGateway` 接口访问，feature 不直接依赖 Jedis。
- 所有颜色/间距/字号来自 `AppColors` / `AppDimens` / `AppTypography`；图标经 `AppIcons`。

## 已知限制

- 洞察页只有**本次会话**采样（最多 1 小时 / 720 条），Redis 不提供历史指标；区间不是 24h/7d 历史。
- **Stream 只读**：不支持 XADD、消费者组管理、分页加载更多（最近 50 条）。
- **集合中的非 UTF-8 成员**：Hash/List/Set/ZSet/Stream 的成员在展示层回退为 HEX，但没有逐行 binary 标记；编辑这类成员可能把 HEX 文本写回，建议先用控制台/`redis-cli` 处理。
- **SSH 主机密钥不校验**（v1 使用 `StrictHostKeyChecking=no`）；支持私钥/密码认证，不支持 SSH agent、多跳、ProxyJump。
- **TLS 客户端私钥**仅支持未加密 PKCS#8 / RSA PKCS#1；不支持加密私钥、EC SEC1、PKCS#12。
- **未支持** Sentinel、Cluster、导入/导出配置、命令导出文件、检查更新/自动更新、自定义快捷键、i18n 切换。
- 控制台字号暂未跟随编辑器字号（值编辑器已生效）。
- 命令面板/设置页的滚动条在 `LazyListState` 上使用近似 thumb；滚轮与常规滚动正常。
- 中文字形由系统字体回退（macOS 为 PingFang SC）；应用只打包 Inter / JetBrains Mono。
- 大值只限制展示，不截断原始数据；超大文本“仍然编辑”仍可能带来输入延迟。
- 安装包图标已包含；代码签名/公证与 Windows/Linux 实机打包仍未配置。
- 批量删除 Key 尚未实现；单 Key 删除确认可关闭。
- macOS 安装包的 bundle version 为 1.0.0（jpackage 要求首位非 0），应用内产品版本为 0.1.0。

## 参考

- 计划与逐阶段验收：[`plan/README.md`](plan/README.md)、[`plan/STATUS.md`](plan/STATUS.md)
- 最终 QA 清单：[`plan/QA-CHECKLIST.md`](plan/QA-CHECKLIST.md)
- 设计参考：[`design/index.html`](design/index.html)

## 开发约定

- `design/` 与 `plan/` 不参与构建，不要删除。
- 新增公开 Composable 需提供深/浅主题 `@Preview`；预览不得读取真实连接文件或连接 Redis。
- 提交前运行 `./gradlew ktlintCheck :app:jvmTest build`。
