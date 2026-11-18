# 阶段状态

> 每个 Agent 完成一个阶段后更新本文件。**未完成的阶段不要标记完成。**
>
> 状态取值：`未开始` / `进行中` / `阻塞` / `已验收`

| 阶段 | 状态 | 完成日期 | 完成内容 / 验证 | 遗留风险 |
| --- | --- | --- | --- | --- |
| 0 — 重置脚手架 | 已验收 | 2026-11-18 | 见下文 §Phase 0 | 无阻塞项 |
| 1 — 设计系统 | 已验收 | 2026-11-18 | 见下文 §Phase 1 | 见“遗留风险” |
| 2 — 领域与数据层 | 未开始 | — | — | — |
| 3 — AppState 与外壳 | 未开始 | — | — | — |
| 4 — Key 浏览器 | 未开始 | — | — | — |
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

## 当前基线

- 分支：`feat/rewrite-ui`；模块：`:desktopApp` + `:app`。
- 包名：`com.czh.rediscompose`；入口：`com.czh.rediscompose.desktop.MainKt`。
- 应用启动即进入 `DesignGalleryScreen`（深色、IRIS）。
- 构建/风格/启动均通过；Phase 2 尚未开始。
- 下一阶段开始前无需用户额外提供密钥或环境；Phase 2 只新增 `domain/**` 与 `data/**`，不修改画廊视觉。
