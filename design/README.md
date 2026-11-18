# Redis Compose — UI 重设计方案

一套为 Redis 桌面管理工具重新设计的界面与设计系统。目标是 **更现代、更清晰、更适合高频使用**：以连接为上下文，以 Key 为焦点，减少边框与视觉噪音，同时保留 Redis 管理工具需要的全部信息密度。

> 这里的所有页面都是可交互的 HTML 原型，不是截图拼贴。建议从 [`index.html`](index.html) 开始浏览。

---

## 快速预览

直接双击打开：

```bash
open design/index.html
```

也可以起一个本地静态服务器（可选，相对路径同样可用）：

```bash
python3 -m http.server 8080 --directory design
# 打开 http://localhost:8080/
```

### URL 参数（方便单独展示某个状态）

| 参数 | 示例 | 作用 |
| --- | --- | --- |
| `theme=light \| dark` | `workspace.html?theme=light` | 强制浅色 / 深色主题 |
| `key=<redis key>` | `workspace.html?key=cache:user:1001:profile` | 直接打开某个 Key 的值视图 |
| `new=1` | `connections.html?new=1` | 自动打开“新建连接”侧滑表单 |

### 已实现的交互

- `⌘K` / `Ctrl+K` 命令面板（页面跳转、操作、连接搜索）
- 深色 / 浅色主题切换（右上角太阳 / 月亮按钮，写入 localStorage）
- Key 树展开 / 收起、搜索（支持 `*`、`?` 通配符）、类型筛选、置顶
- 点击 Key 切换 Hash / List / Set / ZSet / Stream / JSON 值编辑器
- String 值的 Auto / JSON / Text / HEX 展示切换、JSON 语法高亮、复制、保存反馈
- 控制台命令执行（PING / DBSIZE / INFO memory / SCAN / TTL 等演示）、历史、补全、清空
- 连接库筛选、网格 / 列表切换、侧滑新建连接、TLS / SSH / 高级表单
- 设置页主题卡片、强调色切换（跨页面生效）、字号调整、滚动导航

---

## 文件结构

```
design/
├── index.html              # 设计总览（入口）
├── workspace.html          # 键浏览器 + 值编辑器
├── workspace.js            # Key 数据、筛选、树、值渲染
├── console.html            # 命令控制台
├── console.js              # 控制台演示逻辑
├── insights.html           # 服务器洞察
├── insights.js             # SVG 图表
├── connections.html        # 连接库 + 新建连接侧滑
├── connections.js          # 连接筛选、侧滑、表单交互
├── settings.html           # 偏好设置
├── settings.js             # 设置导航、字号、强调色等
└── assets/
    ├── theme.css           # 设计 token、应用外壳、通用组件
    ├── pages.css           # 各页面布局
    ├── app.js              # 图标、主题、强调色、命令面板、toast
    └── shots/              # 2x 预览截图（仅用于 index.html）
```

- 无框架、无构建步骤。图标是内联 SVG sprite，不依赖图片或图标字体。
- 字体通过 Google Fonts 加载 Inter / JetBrains Mono；离线时自动回退到系统 UI 字体与 SF Mono / Menlo。
- 所有数据为演示数据，不连接真实 Redis。

---

## 设计方向

**关键词：安静（calm）、精确（precise）、有温度（warm）。**

- **近黑而不是纯黑**：深色主题使用 `#07090D` 作为背景，面板逐级提亮，避免整屏发光。
- **唯一的强调色**：Iris 紫用于主操作与选中态；状态色（mint / amber / rose）与 Redis 类型色互不混用。
- **信息分层替代边框堆叠**：左侧 rail、连接列表、主工作区、值面板，层级通过留白、少量边框和背景差表达。
- **高密度但不拥挤**：13px UI 字号、11px 等宽代码、8pt 间距体系；表格行高 34px，列表行高 28px。
- **键盘优先**：⌘K、⌘F、N、R、⌘S、↑↓。
- **安全状态可见**：只读、即将过期、连接失败、TLS / SSH 都用颜色 + 文案双重表达。

---

## Design Tokens

### 颜色

| Token | Dark | Light | 用途 |
| --- | --- | --- | --- |
| `--bg` | `#07090D` | `#F5F6FA` | 应用背景 |
| `--surface` | `#0C1017` | `#FFFFFF` | 卡片 / 面板 |
| `--surface-2` | `#10151F` | `#F7F8FC` | 次级面板、输入框 |
| `--surface-3` | `#151B28` | `#EEF1F7` | 悬浮、激活 |
| `--border` | `rgba(255,255,255,.075)` | `rgba(17,24,39,.085)` | 分隔线 |
| `--text` | `#EEF1F8` | `#131726` | 主文本 |
| `--text-2` | `#A4ADC1` | `#58627A` | 次级文本 |
| `--text-3` | `#6C7890` | `#8A93A7` | 辅助信息 |
| `--accent` | `#918DFF` | `#5B58E8` | 主操作、选中态 |
| `--mint` | `#45E3BE` | `#0C9F7F` | 成功 / 在线 / 低延迟 |
| `--amber` | `#F7C566` | `#A86A12` | 警告 / TTL |
| `--rose` | `#FF7BA9` | `#D63F78` | ZSet / 异常提示 |
| `--danger` | `#FF7070` | `#D64545` | 删除 / 连接失败 |

### 语义类型色

| 类型 | Dark | Light |
| --- | --- | --- |
| String | `#6FB7FF` | `#2F7DD1` |
| Hash | `#F7C566` | `#A86A12` |
| List | `#B49BFF` | `#7652D8` |
| Set | `#45E3BE` | `#0C9F7F` |
| ZSet | `#FF7BA9` | `#D63F78` |
| Stream | `#4FD8E8` | `#0A8FA3` |

### 圆角 / 间距 / 字体

```
圆角      xs 6  · sm 8  · md 10 · lg 14 · xl 16
间距      4 / 8 / 12 / 16 / 20 / 24 / 32
UI        Inter (回退 -apple-system, PingFang SC)
代码      JetBrains Mono (回退 SF Mono, Menlo)
字号      标题 22 / 18 · UI 13 · 次 11 · 标签 9 · 代码 11
行高      UI 1.45 · 代码 1.95
```

### 布局尺寸

| 区域 | 尺寸 |
| --- | --- |
| 顶栏 | 54px |
| 左侧 Rail | 60px |
| 连接侧栏 | 258px |
| 工作区 Tab 栏 | 46px |
| Key 列表面板 | 316px（可拖拽，建议 240–420） |
| 状态栏 | 30px |

---

## 界面说明

### 1. 键浏览器（`workspace.html`）

- 顶栏：品牌、环境 / 连接面包屑、全局搜索（⌘K）、延迟状态、主题、通知、账户。
- 左侧 rail：键浏览器 / 控制台 / 洞察 / 连接库 / 设置，底部运行状态与快捷键。
- 连接侧栏：按环境分组、连接延迟、DB 列表，底部服务器健康卡。
- 工作区 tab：连接级 tab 使用紧凑胶囊，不再使用浏览器式大标签。
- Key 列表：置顶区、树形目录、搜索、类型筛选、TTL、SCAN 进度。
- 值面板：类型 + 置顶 / 过期徽标、可复制的 Key、TTL / 内存 / 编码 / 大小元信息、类型化编辑器。
- String 支持 JSON 自动识别、格式化与 Auto/JSON/Text/HEX。
- Hash / List / Set / ZSet / Stream 使用同一套表格语言，支持筛选、行内操作、新增与保存反馈。

### 2. 命令控制台（`console.html`）

- 终端式深色结果区，但使用卡片与合理的行距，而不是纯文本刷屏。
- 每条命令包含时间、提示符、耗时与结果摘要。
- 右侧历史记录 + 收藏命令 + 快捷提示。
- 底部命令输入卡支持补全建议、↑↓ 历史、⌘↵ 执行。

### 3. 服务器洞察（`insights.html`）

- KPI：内存、Ops/s、命中率、连接数，带迷你趋势线。
- 吞吐量面积图、内存环形图、命令分布、Keyspace、健康检查、慢查询、客户端分布。
- 指标全部来自 INFO / COMMANDSTATS / SLOWLOG 的语义组织。
- 所有图表为纯 SVG，便于在 Compose 中用 Canvas 或图表库复刻。

### 4. 连接库（`connections.html`）

- 卡片网格管理连接：状态、延迟、Key 数、客户端数、TLS / SSH / 只读 / 环境标签。
- 支持环境筛选、搜索、网格 / 列表视图。
- “新建连接”使用右侧滑出面板，基础 / TLS / SSH / 高级分组，底部测试连接与保存。
- 下方最近活动表用于审计与快速回到上下文。

### 5. 偏好设置（`settings.html`）

- 左侧分类导航 + 右侧内容滚动。
- 主题卡片、强调色、语言、密度。
- 编辑器：默认格式、字体、字号、换行、行号、截断阈值。
- 键浏览：SCAN 批量、默认视图、分隔符、TTL 徽标、删除确认、自动刷新。
- 安全：全局只读、密码加密、敏感值遮罩、危险命令二次确认。
- 快捷键表、关于与配置位置。

---

## 与现有 Compose 工程的映射

### 1. 主题

替换 `shared/src/commonMain/kotlin/com/czh/redis/ui/Theme.kt` 中的 token：

- 保留 `AppColorTokens` 思路，但按上表扩展：surface 层级、语义色、类型色、代码高亮色。
- 新增 `TypeColors` 的浅色版本（现有 `TypeColors` 是固定亮色，需要按主题切换）。
- 新增 `AppTypography`：UI 使用 Inter（打包字体或系统回退），值/控制台使用 JetBrains Mono。
- 圆角统一为 `6 / 8 / 10 / 14 / 16`；现有 `AppDimens` 可保留字段名但调整值。
- 新增语义 token：`successSoft`、`warnSoft`、`dangerSoft`、`accentSoft`，对应 HTML 中的 soft 变量。

建议新增：

```kotlin
data class AppColorTokens(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val border: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentInk: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val codeBackground: Color,
    // ...
)
```

### 2. 应用外壳（`App.kt`）

- 新增 60px 的 `AppRail`（图标导航 + 工具提示），对应 `.rail`。
- 保留左侧连接面板，但按 `.sidebar` 重构：分组标题、连接卡片、延迟 / 版本 / 客户端数、DB 列表、底部健康卡。
- 顶栏按 `.titlebar` 实现：品牌、面包屑、居中搜索（点击打开命令面板）、状态、主题、通知、头像。
- Tab 栏改为 30px 高的圆角胶囊（`.tab`），不再使用全高方块。
- 状态栏保持 30px，但补充延迟、内存与版本信息。

建议新文件：

```
ui/shell/AppRail.kt
ui/shell/TitleBar.kt
ui/shell/ConnectionSidebar.kt
ui/shell/WorkspaceTabs.kt
ui/shell/StatusBar.kt
ui/command/CommandPalette.kt
ui/components/AppSheet.kt
ui/components/MetaCard.kt
ui/components/TypeChip.kt
ui/components/DataTable.kt
ui/icons/AppIcons.kt
```

### 3. 键浏览器

- `KeyBrowser.kt`：增加置顶区（`pinnedKeys`）、类型筛选 chips、搜索模式提示（`user:*`）、SCAN 进度条。
- `ValuePanel.kt`：
  - 新增 Key 头部（类型、JSON / 置顶 / 过期徽标、复制、控制台、更多菜单）。
  - 新增四格元信息（TTL / 内存 / 编码 / 字段数或大小），TTL 用进度条表达。
  - String 编辑器增加 Auto/JSON/Text/HEX 切换与 JSON 高亮（可用 `AnnotatedString` 着色）。
  - 集合编辑器统一表头、行高、悬停行操作；保存后显示“已同步”。
- 树形行高从 30px 调整到 28px，减少分割线，使用 hover / selected 背景和左侧 2px 强调条。

### 4. 控制台

- `ConsolePage.kt` 改为两栏：主终端 + 右侧历史 / 收藏。
- 结果使用卡片化 `ConsoleBlock`（命令、耗时、字段表、错误色）。
- 输入区独立成 composer，带命令建议弹出层。
- 可复用 `AppTextField`，建议新增 `CommandSuggestionPopup`。

### 5. 服务器信息 → 洞察

- `ServerInfoPage.kt` 改造为仪表盘：
  - KPI 卡片 + SVG/Canvas 迷你趋势线。
  - 吞吐/内存图表、命令分布、Keyspace 表、慢查询、健康检查。
  - 数据仍从 `INFO` 解析，额外读取 `SLOWLOG GET`、`COMMANDSTATS`、`CLIENT LIST`。
- 建议使用 Compose Canvas 绘制折线/面积/环形图，或引入轻量图表库；HTML 中的 SVG 路径可直接移植为 Canvas 路径。

### 6. 连接管理

- `NewConnectionDialog.kt` 改为右侧 `AppSheet`，按基础 / TLS / SSH / 高级分 tab。
- 新增连接库页面（卡片网格 + 筛选 + 最近活动），可作为 `TabType.CONNECTIONS` 或独立入口。
- 连接卡片复用现有 `RedisConnection` 字段：`color`、`group`、`readonly`、`ssh`、`ssl`。

### 7. 设置

- `SettingsPage.kt` 改为左侧导航 + 右侧内容。
- 新增主题预览卡片、强调色、密度、快捷键表。
- 强调色建议持久化到 `AppSettings`（当前 `theme` 之外新增 `accent` 字段）。

### 8. 需要新增的状态

```kotlin
// 浏览
var pinnedKeys: Set<String>
var keyTypeFilter: String?     // null = all
var searchQuery: String

// 界面
var commandPaletteOpen: Boolean
var connectionDrawerOpen: Boolean
var accent: String             // iris / mint / sky / amber / rose

// 设置页持久化
data class AppSettings(
    // 现有字段...
    val accent: String = "iris",
    val uiDensity: String = "comfortable",
    val showKeyTtl: Boolean = true,
    val confirmDangerousCommands: Boolean = true,
    val maskSensitiveValues: Boolean = false,
)
```

---

## 建议实施顺序

1. **Token + 外壳**：Theme、rail、顶栏、连接侧栏、tab、状态栏。
2. **键浏览器**：Key 列表筛选 / 置顶 + 值头部 / 元信息 + String/Hash 编辑器。
3. **值编辑器补全**：List / Set / ZSet / Stream。
4. **控制台**：结构 + 历史 / 收藏 + 补全。
5. **洞察**：KPI + 主图表 + 慢查询。
6. **连接库 / 新建连接 sheet**。
7. **设置页**：主题、强调色、快捷键。
8. **打磨**：键盘焦点、空状态、错误态、加载骨架、只读提示、Toast 一致性。

---

## 备注

- 本目录是 **设计原型**，不参与 Gradle 构建，也不会被桌面应用打包。
- 预览中的 Toast、复制、保存、测试连接等均为前端模拟，用于确认交互与视觉。
- 若要继续实现，建议先从 `Theme.kt` 与 `App.kt` 外壳开始，这样可以最快看到整体视觉变化。
