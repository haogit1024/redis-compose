# Phase 1 — 设计系统与组件画廊

> **目标**：把 `design/` 的颜色、字体、尺寸、图标与基础组件落成 Compose 代码，并提供一个临时组件画廊用于视觉验收。
>
> 依赖：Phase 0  
> 产出：主题、字体、图标映射、基础组件、`DesignGalleryScreen`  
> 下一阶段：Phase 2（领域与数据层）

---

## 1. 开始前

- 阅读 `plan/reference/design-tokens.md`（颜色、字体、图标、组件规格）。
- 阅读 `design/assets/theme.css`、`design/assets/pages.css` 中组件相关段落。
- 本阶段**不**实现页面外壳、连接侧栏、Key 浏览器、控制台、洞察、连接库、设置页。
- 本阶段结束时，应用临时启动到组件画廊；Phase 3 再换成真正外壳。

---

## 2. 依赖与资源

### 2.1 Gradle

在 `gradle/libs.versions.toml` 增加：

```toml
compose-materialIconsExtended = { module = "org.jetbrains.compose.material:material-icons-extended", version.ref = "composeMultiplatform" }
```

在 `app/build.gradle.kts` 的 `commonMain.dependencies` 增加：

```kotlin
implementation(libs.compose.materialIconsExtended)
```

> 如果该 artifact 与当前 Compose 版本不匹配，优先使用 Compose DSL 的 `compose.materialIconsExtended`；仍不匹配则退回 `compose.materialIcons`（基础集）并在 `AppIcons` 中选近似图标。不要引入第三方图标库。

### 2.2 字体

目标：

- `app/src/commonMain/composeResources/font/inter_regular.ttf`
- `app/src/commonMain/composeResources/font/inter_medium.ttf`
- `app/src/commonMain/composeResources/font/inter_semibold.ttf`
- `app/src/commonMain/composeResources/font/jetbrains_mono_regular.ttf`
- `app/src/commonMain/composeResources/font/jetbrains_mono_medium.ttf`

来源：Google Fonts（Inter / JetBrains Mono，OFL 许可）。若网络不可用：

- 暂时使用 `FontFamily.SansSerif` / `FontFamily.Monospace`；
- 在 `AppTypography` 标记 `TODO(phase-1): bundle Inter / JetBrains Mono`；
- 不阻塞本阶段验收，但要在 `STATUS.md` 记录。

使用 Compose Resources 的生成 `Res.font.*` 访问器；字体加载函数集中写在 `AppTypography.kt`，不要把 `Font(...)` 散落到页面。

---

## 3. 文件清单

```text
app/src/commonMain/kotlin/com/czh/rediscompose/
├── domain/settings/
│   └── ThemePreferences.kt   # ThemeMode / Accent / UiDensity（Phase 2 扩展 AppSettings）
└── design/
    ├── theme/
    │   ├── AppColors.kt          # AppColors + dark/light + accent 计算
    │   ├── AppTypography.kt      # Inter / JetBrains Mono + text styles
    │   ├── AppDimens.kt          # spacing / radii / layout sizes
    │   ├── AppShapes.kt          # 可选；若直接用 AppDimens 可省略
    │   └── AppTheme.kt           # AppTheme + LocalAppColors + LocalAppDimens
    ├── icon/
    │   └── AppIcons.kt           # 语义图标映射（Material Outlined）
    ├── component/
    │   ├── AppButton.kt
    │   ├── AppIconButton.kt
    │   ├── AppTextField.kt
    │   ├── AppSearchField.kt
    │   ├── AppBadge.kt           # Badge + TypeChip
    │   ├── AppSwitch.kt
    │   ├── AppSegmented.kt
    │   ├── AppKbd.kt
    │   ├── AppCard.kt
    │   ├── AppMenu.kt
    │   ├── AppDialog.kt
    │   ├── AppSheet.kt
    │   ├── AppTooltip.kt
    │   ├── AppEmptyState.kt
    │   ├── AppLoading.kt
    │   └── AppToast.kt           # ToastData + AppToastHost
    └── gallery/
        └── DesignGalleryScreen.kt
```

`Accent` / `ThemeMode` / `UiDensity` 是设置领域的只读枚举，本阶段先在 `domain/settings/ThemePreferences.kt` 创建（不依赖 UI）。Phase 2 的 `AppSettings` 直接引用它们，不要再定义第二份。

---

## 4. 主题实现

### 4.1 AppColors

- 严格按 `design-tokens.md` 建 `data class AppColors`，字段覆盖深/浅两套。
- 提供：
  ```kotlin
  object AppColors {
      fun forTheme(dark: Boolean, accent: Accent): AppColors
  }
  ```
- 强调色通过 `Accent` 计算 base / strong / ink / soft / line；不要用全局 mutable 颜色。
- 光照/边框使用 alpha 颜色时，注意在浅色主题下使用黑色 alpha，而不是白色 alpha。
- `AppTheme` 内同时配置 `MaterialTheme` 的最小 colorScheme，避免 Material 组件出现默认紫色。

### 4.2 Typography

- 提供 `AppTypography` 与 `LocalAppTypography`，或直接通过 `MaterialTheme.typography` 暴露。
- 至少包含 `design-tokens.md` 的文本样式；代码样式显式用 Mono。
- 标题/正文/标签的 `letterSpacing` 用近似 sp 值。
- `Text` 使用 `LocalAppColors` 颜色；不要依赖默认 Material 文本色。

### 4.3 Dimens

- `AppDimens` 包含 spacing / radii / layout sizes / row heights / icon sizes。
- 如需密度切换（comfortable/compact），在 `AppDimens.forDensity(UiDensity)` 提供两套值；Phase 9 再接入设置。本阶段可只实现 comfortable。

### 4.4 主题入口

```kotlin
@Composable
fun AppTheme(
    dark: Boolean,
    accent: Accent = Accent.IRIS,
    density: UiDensity = UiDensity.COMFORTABLE,
    content: @Composable () -> Unit,
)
```

- 用 `CompositionLocalProvider` 提供 colors/dimens。
- 根背景由调用方（画廊/外壳）铺 `colors.background`，`AppTheme` 本身不强制全屏 Box。

---

## 5. 图标

- 创建 `AppIcons`，按 `design-tokens.md` 的映射表使用 `Icons.Outlined.*`。
- 仅映射本阶段和后续会用到的图标；至少覆盖：
  `Search, Add, Close, KeyboardArrow*, Refresh, DarkMode, LightMode, VpnKey, Terminal, ShowChart, Storage, Dns, StarBorder, ContentCopy, DeleteOutline, Edit, MoreHoriz, Schedule, Memory, PeopleOutline, Shield, LockOutline, Folder, FolderOpen, Check, WarningAmber, Info, OpenInNew, Tag, FormatListBulleted, Layers, GridView, FilterList, SwapVert, Visibility, VisibilityOff, Link, Public, Keyboard, NotificationsNone, Send, PlayArrow, Save, MenuBook, Speed, Percent, Download, Upload, Code, AutoAwesome, History, AddCircleOutline, CheckCircleOutline, ErrorOutline, Wifi, Bolt`
- 品牌 Logo 单独用 `BrandMark` composable 自绘，不放进 AppIcons。
- 图标默认 16dp；按钮内 14dp；大图标 18–22dp。统一 tint = `currentContentColor` 或显式传入。

---

## 6. 基础组件

实现 `design/component/` 下的组件，规格见 `design-tokens.md` §5。要求：

- 所有组件 `modifier` 最后；提供合理默认值。
- 不使用 Material3 的 `Button` / `OutlinedTextField` 作为最终外观（可用其交互原语），因为默认 Material 视觉与设计差异大。
- `AppTextField` 使用 `BasicTextField` + focus 状态；让 placeholder、leading/trailing slot、密码、单行/多行都可配置。
- `AppSearchField` 组合 `AppTextField` + Search 图标 + kbd 提示。
- `AppDialog` 用 Compose Desktop `DialogWindow` 或 `Dialog`；背景/圆角/结构按设计。
- `AppSheet` 用全屏 overlay（Box + 遮罩）+ 右侧滑入；必须支持 `onDismiss` 和 Esc。
- `AppMenu` 用 `Popup`；右键菜单和更多菜单共用。
- `AppTooltip` 用 hover 延迟；不要引入实验性 API 以免版本问题。
- `AppToast` 提供 `ToastData`、`ToastKind`、`AppToastHost(toasts, modifier)`；本阶段不接业务。
- `AppEmptyState` 接受图标、标题、描述、可选 action。
- `AppLoading` 提供行内/块级两种形态。
- 每个组件文件底部提供 `@Preview`；预览包 `AppTheme`。

### 6.1 组件验收重点

- 按钮 hover / active / disabled 有可见差异。
- 输入框 focus 环不改变尺寸。
- TypeChip 六种类型颜色正确。
- 开关和 segmented 的动画自然。
- 对话框与 sheet 在浅色/深色下都不透出错误底色。
- 图标与文字基线对齐。

---

## 7. 组件画廊

创建 `DesignGalleryScreen`：

- 顶栏：`Redis Compose / Design system`，右侧主题切换（dark/light）和强调色切换（至少 IRIS/MINT/SKY）。
- 区块：
  1. Colors：背景/表面/文字/accent/status/type 色板。
  2. Typography：设计 token 的所有字号。
  3. Buttons：四种变体 × 两种尺寸 × disabled/loading。
  4. Inputs：普通、搜索、密码、多行、错误/禁用。
  5. Badges / TypeChips / Kbd。
  6. Segmented / Switch / Tooltip。
  7. Card / EmptyState / Loading。
  8. Overlays：按钮打开 Dialog、Sheet、Menu、Toast（临时状态）。
- 画廊状态用 `remember`，不依赖 domain/data。
- 根 `RedisComposeApp` 暂时改为：
  ```kotlin
  AppTheme(dark = isSystemInDarkTheme(), accent = Accent.IRIS) { DesignGalleryScreen() }
  ```
- Phase 3 会用真正外壳替换；画廊保留为开发者页面或 Phase 10 删除。

---

## 8. 非目标

- 不实现 `AppState`、连接、Redis、导航、外壳。
- 不实现任何业务页面。
- 不实现窗口自定义 chrome。
- 不引入主题切换持久化（Phase 9）。

---

## 9. 验收

```bash
./gradlew ktlintCheck
./gradlew build
./gradlew :desktopApp:run
```

- [ ] 应用启动到组件画廊，默认深色。
- [ ] 主题切换后所有组件同步变浅，无残留深色块。
- [ ] 强调色切换后主按钮/选中态变化，文字对比度可读。
- [ ] 画廊滚动流畅，所有组件在 1120×720 下可用。
- [ ] `grep` feature/design 无裸 `Color(0x`（主题文件、品牌 Logo、画廊色板除外）。
- [ ] 每个基础组件有 `@Preview`。
- [ ] `plan/STATUS.md` 记录字体是否成功打包、图标库版本。

---

## 10. STOP

停止。Phase 2 实现领域模型、Redis Gateway 与存储；不要提前写 Key 浏览器。
