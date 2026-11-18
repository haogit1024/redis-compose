# 设计 Tokens 与组件规格

> 唯一视觉来源：`design/assets/theme.css`、`design/assets/pages.css`、`design/*.html`。
> 本文把 CSS 变量翻译成 Compose 可直接使用的 token 与组件规格。**feature 代码只允许使用这些 token。**

---

## 1. 颜色

### 1.1 深色主题

| Compose 名称 | Hex | 用途 |
| --- | --- | --- |
| `background` | `#07090D` | 应用背景 |
| `surface` | `#0C1017` | 卡片 / 面板 / 顶栏 |
| `surface2` | `#10151F` | 输入框、次级面板 |
| `surface3` | `#151B28` | hover、激活、对话框 |
| `surface4` | `#1A2130` | 最高层级、菜单 |
| `border` | `White @ 7.5%` | 常规分隔线 |
| `borderStrong` | `White @ 13%` | 控件边框、浮层边框 |
| `borderEmphasis` | `White @ 20%` | hover 边框 |
| `text` | `#EEF1F8` | 主文本 |
| `textSecondary` | `#A4ADC1` | 次级文本 |
| `textTertiary` | `#6C7890` | 辅助信息 |
| `textQuaternary` | `#4B5568` | 极弱提示 / 占位 |
| `accent` | `#918DFF` | 主操作、选中态、聚焦 |
| `accentStrong` | `#6B67F4` | 主按钮渐变尾色 |
| `accentSoft` | `accent @ 13%` | 选中背景 |
| `accentSofter` | `accent @ 6%` | 聚焦光晕 |
| `accentLine` | `accent @ 40%` | 聚焦边框 |
| `success` | `#45E3BE` | 在线、成功、低延迟 |
| `warning` | `#F7C566` | 警告、TTL |
| `danger` | `#FF7070` | 删除、连接失败 |
| `info` | `#6FB7FF` | 信息提示 |
| `terminalBackground` | `#080B11` | 控制台 |
| `codeBackground` | `#090C12` | 值编辑器 |
| `codeLineNumber` | `#394254` | 行号 |

### 1.2 浅色主题

| Compose 名称 | Hex |
| --- | --- |
| `background` | `#F5F6FA` |
| `surface` | `#FFFFFF` |
| `surface2` | `#F7F8FC` |
| `surface3` | `#EEF1F7` |
| `surface4` | `#E5E9F2` |
| `border` | `Black @ 8.5%` |
| `borderStrong` | `Black @ 14%` |
| `borderEmphasis` | `Black @ 22%` |
| `text` | `#131726` |
| `textSecondary` | `#58627A` |
| `textTertiary` | `#8A93A7` |
| `textQuaternary` | `#AAB2C2` |
| `accent` | `#5B58E8` |
| `accentStrong` | `#4A47D4` |
| `success` | `#0C9F7F` |
| `warning` | `#A86A12` |
| `danger` | `#D64545` |
| `info` | `#2F7DD1` |
| `codeBackground` | `#FBFCFF` |
| `codeLineNumber` | `#C3CAD6` |

浅色主题不是深色反色：浅色中的 accent 更深，状态色降低明度，代码背景保持接近白色。

### 1.3 数据类型色

| 类型 | 深色 | 浅色 | 用途 |
| --- | --- | --- | --- |
| STRING | `#6FB7FF` | `#2F7DD1` | dot、type pill、编辑器强调 |
| HASH | `#F7C566` | `#A86A12` | 同上 |
| LIST | `#B49BFF` | `#7652D8` | 同上 |
| SET | `#45E3BE` | `#0C9F7F` | 同上 |
| ZSET | `#FF7BA9` | `#D63F78` | 同上 |
| STREAM | `#4FD8E8` | `#0A8FA3` | 同上 |
| NONE | `#6C7890` | `#8A93A7` | 未知类型 |

Type pill 规格：背景 = 类型色 11% + transparent，边框 = 类型色 20%，文字 = 类型色，圆角 5dp，高度 20dp，字号 9sp/600/letterSpacing 0.06em。

### 1.4 代码高亮

| Token | 深色 | 浅色 |
| --- | --- | --- |
| key | `#9DB4FF` | `#3A56C9` |
| string | `#7EE7C8` | `#0B8267` |
| number | `#F7C566` | `#9A640B` |
| boolean | `#C9A3FF` | `#6D45C4` |
| punctuation | `#6C7890` | `#8891A4` |

### 1.5 强调色方案（设置页）

`Accent` 枚举：`IRIS`（默认）、`MINT`、`SKY`、`AMBER`、`ROSE`。

| Accent | Dark base | Light base | Dark ink | Light ink |
| --- | --- | --- | --- | --- |
| IRIS | `#918DFF` | `#5B58E8` | `#FFFFFF` | `#FFFFFF` |
| MINT | `#45E3BE` | `#0C9F7F` | `#090B10` | `#FFFFFF` |
| SKY | `#6FB7FF` | `#2F7DD1` | `#090B10` | `#FFFFFF` |
| AMBER | `#F7C566` | `#A86A12` | `#090B10` | `#FFFFFF` |
| ROSE | `#FF7BA9` | `#D63F78` | `#090B10` | `#FFFFFF` |

主按钮文字色用 `accentInk`，不要一律白色。强调色切换后派生色（soft/line）也要重新计算。

---

## 2. 字体

### 2.1 字体族

- UI：**Inter**（400 / 500 / 600 / 650 / 700 可用 400/500/600 三档）
  - Compose Resources：`Res.font.inter_regular`、`inter_medium`、`inter_semibold`
  - 回退：`FontFamily.SansSerif`
- 代码：**JetBrains Mono**（400 / 500）
  - `Res.font.jetbrains_mono_regular`、`jetbrains_mono_medium`
  - 回退：`FontFamily.Monospace`

字体文件放在 `app/src/commonMain/composeResources/font/`，文件名小写下划线。若网络不可用，Phase 1 可先使用系统字体，但 `AppTypography` 必须保留明确的 TODO 与替换点。

### 2.2 文本样式

| 语义 | 字号 | 字重 | Letter spacing | 行高 | 使用位置 |
| --- | --- | --- | --- | --- | --- |
| `display` | 28sp | 650 | -0.9sp | 34sp | 登录 / 空状态大标题（可按需） |
| `pageTitle` | 22sp | 650 | -0.7sp | 27sp | 洞察 / 连接库 / 设置标题 |
| `keyTitle` | 18sp | 550 | -0.4sp | 24sp | 值面板 Key 名（Mono） |
| `sectionTitle` | 14sp | 600 | -0.15sp | 20sp | 对话框标题、设置分组 |
| `body` | 13sp | 450 | 0sp | 19sp | 默认 UI 文本 |
| `bodyStrong` | 13sp | 550 | 0sp | 19sp | 连接名、列表主文本 |
| `caption` | 11sp | 450 | 0sp | 16sp | 辅助说明 |
| `microLabel` | 9sp | 600 | 0.11em | 12sp | 全大写分组标签 |
| `monoValue` | 11sp | 400 | -0.1sp | 17sp | Key、值、代码 |
| `monoMeta` | 9.5sp | 400 | 0sp | 14sp | TTL、延迟、状态栏 |
| `button` | 12sp | 500 | 0sp | 16sp | 按钮 |
| `buttonSmall` | 11.5sp | 500 | 0sp | 15sp | 小按钮 |

Compose 中负 letterSpacing 用 `(-0.4).sp` 等近似；全大写标签用 `.uppercase()` 或预置文案。

---

## 3. 尺寸 / 间距 / 圆角

### 3.1 布局

| 名称 | 值 | 说明 |
| --- | --- | --- |
| `topBarHeight` | 54dp | 应用顶栏 |
| `railWidth` | 60dp | 图标导航 |
| `sidebarWidth` | 258dp | 连接侧栏 |
| `tabBarHeight` | 46dp | 工作区 tab 栏 |
| `keyPaneWidth` | 316dp | Key 列表面板默认宽度 |
| `keyPaneMin` | 240dp | 拖拽最小 |
| `keyPaneMax` | 420dp | 拖拽最大 |
| `statusBarHeight` | 30dp | 底部状态栏 |
| `windowInitial` | 1440 × 900 | 初始窗口 |
| `windowMin` | 1120 × 720 | 最小窗口 |

### 3.2 间距

`4 / 8 / 12 / 16 / 20 / 24 / 32 dp`。默认：

- 页面外边距：`24dp`（洞察 / 连接库），`28dp` 右 / 下更宽松时可接受。
- 卡片内边距：`12–16dp`。
- 行内元素间距：`6–8dp`。
- 分组间距：`14–20dp`。
- 8dp 网格为默认，不出现 5 / 7 / 13 等随意值（既有设计中的 1px 分割线除外）。

### 3.3 圆角

| 名称 | 值 | 用途 |
| --- | --- | --- |
| `xs` | 6dp | Key 行、chip、kbd |
| `sm` | 8dp | 输入框、按钮、tab |
| `md` | 10dp | 菜单、连接卡片内部块 |
| `lg` | 14dp | 卡片、面板、编辑器 |
| `xl` | 16dp | 对话框、sheet、hero 容器 |

### 3.4 行高

| 名称 | 值 |
| --- | --- |
| `keyRowHeight` | 28dp |
| `treeFolderHeight` | 28dp |
| `listRowHeight` | 34dp |
| `tableHeaderHeight` | 28dp |
| `connectionCardMinHeight` | 168dp 左右（内容驱动） |

---

## 4. 图标

### 4.1 方案

使用 `material-icons-extended` 的 **Outlined** 图标。所有图标通过 `design/icon/AppIcons.kt` 暴露语义名称：

```kotlin
object AppIcons {
    val Search = Icons.Outlined.Search
    val Add = Icons.Outlined.Add
    val Close = Icons.Outlined.Close
    val Key = Icons.Outlined.VpnKey
    val Terminal = Icons.Outlined.Terminal
    val Insights = Icons.Outlined.ShowChart
    val Connections = Icons.Outlined.Storage
    val Settings = Icons.Outlined.Tune
    // ...
}
```

Feature 代码只写 `AppIcons.Search`，不直接写 `Icons.Outlined.Search`。后续若换成自定义线框图标，只改这一个文件。

### 4.2 语义映射

| 语义 | Material Outlined |
| --- | --- |
| search | `Search` |
| add | `Add` |
| close | `Close` |
| chevron-down / right / left / up | `KeyboardArrowDown` / `KeyboardArrowRight` / `KeyboardArrowLeft` / `KeyboardArrowUp` |
| refresh | `Refresh` |
| theme-dark / light | `DarkMode` / `LightMode` |
| key | `VpnKey` |
| console | `Terminal` |
| insights | `ShowChart` / `Timeline` |
| connections | `Storage` |
| server | `Dns` |
| star | `StarBorder`（选中态可用 `Star`） |
| copy | `ContentCopy` |
| delete | `DeleteOutline` |
| edit | `Edit` |
| more | `MoreHoriz` |
| clock / ttl | `Schedule` |
| memory | `Memory` |
| clients | `PeopleOutline` |
| shield / readonly | `Shield` |
| lock | `LockOutline` |
| folder / open | `Folder` / `FolderOpen` |
| check | `Check` |
| warning | `WarningAmber` |
| info | `Info` |
| external | `OpenInNew` |
| command | `Keyboard` |
| hash / list / layers | `Tag` / `FormatListBulleted` / `Layers` |
| grid | `GridView` |
| filter | `FilterList` |
| sort | `SwapVert` |
| eye | `Visibility` / `VisibilityOff` |
| link | `Link` |
| globe | `Public` |
| keyboard | `Keyboard` |
| bell | `NotificationsNone` |
| send / run | `Send` / `PlayArrow` |
| save | `Save` |
| book | `MenuBook` |
| gauge | `Speed` |
| percent | `Percent` |
| download / upload | `Download` / `Upload` |
| code | `Code` |
| sparkles | `AutoAwesome` |
| history | `History` |
| add-circle | `AddCircleOutline` |
| check-circle | `CheckCircleOutline` |
| error-circle | `ErrorOutline` |
| database | `Storage` |
| wifi | `Wifi` |
| zap | `Bolt` |

若某个图标在当前版本不存在，选最接近的 Outlined 图标，**不要**在 feature 里临时画 SVG。

品牌 Logo：用 `Canvas` 或 `Box` + 三个叠层圆角矩形；尺寸 27dp，圆角 8dp，渐变 `#A49BFF → #6C67F2 → #27D3B4`。不要依赖图片资源。

---

## 5. 组件规格

> 所有组件放在 `design/component/`。Props 的 `modifier: Modifier = Modifier` 必须是最后一个参数（slot 参数另算）。

### 5.1 AppButton

```kotlin
enum class AppButtonVariant { Primary, Secondary, Ghost, Danger }
enum class AppButtonSize { Small, Medium }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Secondary,
    size: AppButtonSize = AppButtonSize.Medium,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
)
```

| 变体 | 背景 | 边框 | 文本 |
| --- | --- | --- | --- |
| Primary | accent 78% → accentStrong 渐变 | white 14% | accentInk |
| Secondary | surface2 | border | text |
| Ghost | transparent | transparent | textSecondary |
| Danger | danger 12% | danger 26% | danger |

尺寸：Medium 32dp 高 / 12dp 水平内边距 / 12sp；Small 28dp / 9dp / 11.5sp。图标 14dp，间距 6dp。

### 5.2 AppIconButton

30×30dp（Small 26×26dp），圆角 8/7dp，默认透明、textTertiary；hover = surface3 + text；active = accentSoft + accent。

### 5.3 AppTextField / AppSearchField

- 高度 32dp（紧凑 28dp），圆角 8dp。
- 背景 surface2，边框 border。
- focus：边框 accentLine + 3dp 的 accentSofter 外光晕。
- placeholder：textQuaternary。
- 内部水平 padding 10dp；带 leading icon 时左 padding 33dp。
- `AppSearchField` 带 Search 图标与右侧 `⌘F` / `⌘K` kbd 提示。
- 密码字段通过 `visualTransformation` 实现，不自行造点。

### 5.4 AppBadge / AppTypeChip

- Badge 高 20dp，圆角 5dp，字号 9.5sp/550，背景 surface3，边框 border；语义变体：accent / success / warning / danger / plain。
- TypeChip 严格按 1.3 节颜色与尺寸。
- 类型筛选 chip：高 21dp，字号 9sp，圆角 5dp；激活时背景为类型色 12%，文字为类型色。

### 5.5 AppSwitch

- 34×19dp，圆角 20dp；关闭背景 surface4，开启 accent；滑块 13dp 白色，动画 180ms。
- 点击切换；支持键盘 Enter / Space（如果需要无障碍）。

### 5.6 AppSegmented

- 外层 surface2 + border，padding 2dp，圆角 8dp。
- 内项高 24dp（Small 22dp），圆角 6dp，激活 surface4 + text。
- 用于 Auto/JSON/Text/HEX、时间范围、密度。

### 5.7 AppKbd

- 高 17dp，最小宽 17dp，字号 9.5sp，圆角 5dp，surface3，border，底部阴影 1dp。

### 5.8 AppCard

- 背景 surface，边框 border，圆角 14dp，可选 padding 13–16dp。
- 卡片头：标题 12sp/600 + 描述 10sp/textTertiary + 右侧 actions。

### 5.9 AppDialog

- 宽 520dp（TTL / 重命名），最大高 `min(560dp, 视口 - 80dp)`。
- 背景 surface，圆角 16dp，borderStrong，阴影。
- 结构：head（标题 + 副标题 + 关闭）/ body（可滚动）/ footer（右对齐操作）。
- 遮罩：`#04060A @ 56%` + 6dp blur（桌面可用半透明黑，不强制 blur）。

### 5.10 AppSheet

- 右侧滑出，宽 470dp（连接表单），全高。
- 头部 / tab / 可滚动 body / footer。
- 进入动画：220ms `FastOutSlowIn` + 24dp 位移；遮罩淡入 160ms。
- 用于新建/编辑连接；不要用普通对话框。

### 5.11 AppMenu

- Popup，min 宽 168dp，圆角 10dp，背景 surface3，borderStrong。
- item 高 29dp，字号 11.5sp；hover = accentSoft；danger 项红色。
- 右键菜单与更多菜单共用此组件。

### 5.12 AppToastHost

- 右下角，距底部状态栏 14dp，右侧 16dp。
- 高约 36dp，圆角 10dp，背景 surface3，borderStrong。
- Success / Warning / Danger 用图标着色；3 秒自动消失，最多同时 3 条。
- 支持详情（错误堆栈）可选，但 v1 不必做详情弹窗。

### 5.13 AppTooltip

- hover 500ms 后显示；surface4 背景、圆角 6dp、字号 10sp、最多一行。
- 图标按钮必须配置 tooltip；不要在界面里塞一堆常驻说明。

### 5.14 AppEmptyState / AppLoading

- 空状态：图标容器 50dp（accentSoft 背景、圆角 14dp）+ 标题 13sp/550 + 描述 11sp + 可选操作按钮。
- 加载：骨架或 16dp 的环形进度；不要用大面积 spinner 遮盖已有内容。

### 5.15 滚动条

- 宽 9dp，thumb 为 borderStrong，hover 为 borderEmphasis，轨道透明。
- 列表内滚动条可覆盖显示；不要用系统默认粗滚动条。

---

## 6. 主题实现

```kotlin
data class AppColors(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val surface4: Color,
    val border: Color,
    val borderStrong: Color,
    val borderEmphasis: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textQuaternary: Color,
    val accent: Color,
    val accentStrong: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val accentSofter: Color,
    val accentLine: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val codeBackground: Color,
    val codeLineNumber: Color,
    val typeString: Color,
    // ... hash/list/set/zset/stream/none
    val terminalBackground: Color,
    val jsonKey: Color,
    val jsonString: Color,
    val jsonNumber: Color,
    val jsonBoolean: Color,
    val jsonPunctuation: Color,
)

@Composable
fun AppTheme(
    dark: Boolean,
    accent: Accent,
    density: UiDensity = UiDensity.COMFORTABLE,
    content: @Composable () -> Unit,
)
```

- 内部同时提供 `MaterialTheme` 的最小映射（背景、surface、primary、error、outline），让 Text/TextField 等底层组件不出现 Material 默认紫色。
- `LocalAppColors`、`LocalAppDimens` 通过 `CompositionLocalProvider` 提供。
- 强调色由 `AppColors.forTheme(dark, accent)` 计算，不修改全局单例。
- 字体由 `AppTypography` 提供；`Text` 默认使用 UI 字体，代码区域显式传入 Mono。

---

## 7. 动效

| 场景 | 时长 / 曲线 |
| --- | --- |
| hover / 颜色变化 | 130–150ms linear |
| tab / segmented 切换 | 120ms |
| switch 滑块 | 180ms |
| 对话框 / 菜单出现 | 140–160ms，缩放 0.985→1 + 淡入 |
| sheet 进入 | 220ms，水平 24dp |
| 图表绘制 | 900–1400ms，ease-out |
| toast | 200ms 进入，220ms 退出 |

不做无意义的持续动画；`live` 状态的呼吸点可以 2.4s 循环，但仅用于“在线”。

---

## 8. 布局细节（来自设计稿）

- 顶栏高度 54dp；品牌区至少 158dp；中间搜索最大 400dp；右侧状态 pill / 主题 / 通知 / 头像。
- Rail 60dp，图标按钮 38dp，激活态左侧 2dp accent 高亮条。
- 连接侧栏 258dp；连接卡 padding 8–9dp；host 用 10sp Mono；分组标签 9.5sp 全大写。
- 工作区 tab 高 30dp 的圆角胶囊，激活背景 surface3 + accent 图标。
- Key 面板：标题行 / 搜索 / 类型 chips / 树 / 底部 SCAN 状态；行高 28dp。
- 值面板：内边距 20–24dp；Key 标题 18sp Mono；四格元信息；编辑器卡片填满剩余高度（String 视图）。
- 控制台：终端区域深色，输入 composer 独立卡片，右侧 272dp 历史栏。
- 洞察：KPI 四列，主图表 1.65fr + 内存 1fr，底部三列；页面整体可滚动。
- 连接库：3 列卡片；新增连接卡片跨整行；表单从右侧滑出。
- 设置：左导航 208dp，内容最大 760dp 居中。
