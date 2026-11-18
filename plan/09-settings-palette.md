# Phase 9 — 设置、主题持久化与命令面板

> **目标**：实现完整设置页，把主题/强调色/密度/编辑器/安全等设置接入全局；扩展命令面板与全局快捷键。
>
> 依赖：Phase 5（值编辑器）、Phase 8（连接库）  
> 下一阶段：Phase 10（打磨与发布）

---

## 1. 开始前

- 阅读 `design/settings.html` 与 `design/assets/pages.css` 的 `.settings-*`、`.theme-*`、`.accent-*`、`.set-row`、`.shortcut-table`。
- 参考 `design/assets/shots/settings.png`。
- 阅读 `plan/reference/domain-and-data.md` §5（AppSettings）。
- 命令面板基础版在 Phase 3 已完成；本阶段扩展它，而不是重写。

---

## 2. 文件清单

```text
feature/settings/
├── SettingsScreen.kt
├── SettingsState.kt             # 草稿/保存状态（也可直接用 AppState + 局部 remember）
├── SettingsNav.kt
├── sections/
│   ├── AppearanceSection.kt
│   ├── EditorSection.kt
│   ├── BrowserSection.kt
│   ├── SecuritySection.kt
│   ├── ShortcutsSection.kt
│   └── AboutSection.kt
├── ThemeChoiceCard.kt
└── AccentSwatches.kt

feature/shell/
├── CommandPalette.kt            # 扩展 action 与 recent
└── GlobalShortcuts.kt           # 根级快捷键处理

app/
├── AppState.kt                  # settings 保存 debounce、palette 动作
└── RedisComposeApp.kt           # 把 density 传入 AppTheme
```

同步修改：

- `AppTheme(dark, accent, density)`；`AppDimens.forDensity` 生效。
- `CodeEditor` 读取 `defaultValueFormat`、`wordWrap`、`showLineNumbers`、`editorFontSize`。
- `ConsoleState` 读取 `confirmDangerousCommands`。
- `HashEditor` 读取 `maskSensitiveValues`。
- `WorkspaceState` 读取 `confirmDeleteKeys`。
- 状态栏/侧栏自动刷新读取 `autoRefreshSeconds`（可选）。

---

## 3. 设置页布局

- 左侧导航 208dp；右侧内容滚动，最大 760dp 居中。
- 导航分组：
  - 偏好：外观、编辑器与值、键浏览；
  - 系统：安全、快捷键、关于。
- 点击导航平滑滚动到对应 section，并更新 active。
- 页头：眉标 `PREFERENCES`、标题“偏好设置”、描述；右上/顶栏显示“已自动保存”。
- 每个 section：标题 + 描述 + 卡片面板；行高 55dp，最后一行无底边。
- 设置修改即时生效；不需要“保存”按钮。持久化 debounce 300ms，并在状态栏/页脚显示已保存。

---

## 4. 各 Section

### 4.1 外观

- 主题三张卡：深色 / 浅色 / 跟随系统。
  - 卡片含 mini 界面缩略图（用 Compose 绘制，不用截图）；
  - 激活边框 accent + 2dp soft 外环；
  - 选择后立即更新 `settings.theme`；`SYSTEM` 时按系统深浅解析。
- 强调色：IRIS/MINT/SKY/AMBER/ROSE 五个圆点；激活带白色描边；切换后所有页面即时生效。
  - 主按钮文字使用 `accentInk`，不能在 mint/amber 上强制白字。
- 界面语言：v1 只有“简体中文”，其他选项禁用/标“即将支持”；不要显示可切换但无效果的选项。
- 界面密度：舒适/紧凑；切换 `AppDimens`（行高、padding）并持久化。
  - 紧凑不能破坏 1120×720 布局。

### 4.2 编辑器与值

- 默认值格式：Auto / JSON / Text / HEX（`ValueFormat`）。
- 等宽字体选择：JetBrains Mono（v1 唯一）；其他选项可禁用。
- 字号：stepper 11–18；应用到 CodeEditor 与 Console（控制台字号可稍后统一）。
- 自动换行：影响 CodeEditor。
- 显示行号：影响 CodeEditor。
- 大值截断阈值 KB：数字输入 1–1024；影响 Phase 5 的截断提示。

### 4.3 键浏览

- SCAN 批量大小：数字 100–10,000；影响 WorkspaceState。
- 默认视图：树形/平铺（KeyView）。
- Key 分隔符：全局默认（新连接使用），不能修改已有连接的分隔符；文案说明。
- 显示 TTL 徽标：开关。
- 删除 Key 前确认：开关；关闭后单 Key 删除直接执行，但批量（未来）仍确认。
- 自动刷新间隔：关闭/30/60 秒；用于侧栏会话摘要/DB 计数刷新（不是 Key 列表），实现为 AppState 定时刷新 summary；离开应用不轮询。

### 4.4 安全

- 全局只读模式：立即作用于值编辑器、控制台、新建/删除 Key；状态栏显示只读徽标。
- 加密存储密码：默认开；关闭时保存明文，UI 明确警告。
  - 切换时不自动重写已有文件；提示“下次保存连接时生效”或提供“立即重新加密”按钮（可选）。
- 隐藏敏感值：开启后 Hash 字段名匹配 `password|passwd|secret|token|api[_-]?key|private` 的值默认显示 `••••••`，行内眼睛按钮显示；ZSet/List 不处理。
- 危险命令二次确认：开启时，控制台执行 `FLUSHALL/FLUSHDB/CONFIG SET/SCRIPT FLUSH/EVAL` 等弹出确认对话框；关闭则直接执行（只读仍拦截）。
- 危险区：清除本地缓存（命令历史、筛选、指标样本、最近命令），不删除连接/设置。
  - 需要确认对话框。

### 4.5 快捷键

只列出**已实现**的快捷键；不要列出未来功能：

| 动作 | 快捷键 |
| --- | --- |
| 命令面板 | ⌘K / Ctrl+K |
| 搜索当前 Keyspace | ⌘F / Ctrl+F |
| 保存当前值 | ⌘S / Ctrl+S |
| 执行控制台命令 | ⌘↵ / Ctrl+Enter |
| 新建 Key | N（非输入焦点） |
| 刷新当前 Keyspace | R（非输入焦点） |
| 关闭当前 Tab | ⌘W / Ctrl+W |
| 切换深/浅主题 | ⌘⇧L / Ctrl+Shift+L |
| 关闭弹层 | Esc |

- v1 不支持自定义键盘映射；表格为只读展示。
- 快捷键实现放在 `GlobalShortcuts.kt`，通过 `Window` 的 `onPreviewKeyEvent` 或根 Box 的焦点处理；必须避免在文本输入中误触发 N/R。
- 文本输入中的 ⌘K/F/S 仍应生效（全局），Esc 先关闭最上层弹层。

### 4.6 关于

- 品牌 Logo + 版本（0.1.0）+ 技术栈（Kotlin / Compose Desktop）。
- 配置路径 `~/.redis-compose/`，复制按钮。
- GitHub / 文档链接（可选；不要伪造链接）。
- “检查更新”无更新服务，不要实现假按钮；可省略。
- 参与贡献/打开仓库可保留真实链接占位，若没有仓库地址则隐藏。

---

## 5. 设置持久化与生效

- `AppState.updateSettings`：
  - 立即更新内存；
  - 300ms debounce 保存；
  - 保存失败 toast，并在状态栏显示“未保存”状态（可选）。
- `RedisComposeApp` 解析：
  ```kotlin
  val dark = when (settings.theme) { SYSTEM -> systemDark; DARK -> true; LIGHT -> false }
  AppTheme(dark = dark, accent = settings.accent, density = settings.density) { ... }
  ```
- 主题卡激活状态基于 `settings.theme`，不是解析后的 dark/light；跟随系统时“系统”卡激活。
- 顶部主题按钮：点击在 DARK/LIGHT 之间切换并写入 settings（不写 SYSTEM）。
- 所有设置读取通过 `settingsProvider()` 或传入的 `AppSettings`，不要在 feature 里持有旧的 settings 副本。
- 字体/密度变化需要触发重组；不要重启应用。

---

## 6. 命令面板扩展

在 Phase 3 的导航型面板基础上增加：

### 6.1 项

- 页面：工作区、控制台、洞察、连接库、设置。
- 操作：
  - 新建 Key（当前 Workspace 有连接时）；
  - 新建连接（跳转连接库并打开 sheet）；
  - 刷新当前 Keyspace；
  - 切换深/浅主题；
  - 复制当前 Key（如果有选择）；
  - 打开设置。
- 连接：所有连接（名称 + host + 状态）。
- 最近命令：`settings.recentCommands`（最多 5 条，Phase 6 执行命令时记录）。
- 过滤与分组；无结果空状态。

### 6.2 键盘

- ⌘K / Ctrl+K 开关；
- ↑↓ 选择，Enter 执行，Esc 关闭；
- 输入过滤 label/hint；
- 打开时焦点在输入框；关闭后焦点返回原控件（可选）。

### 6.3 视觉

- 600dp 宽，15dp 圆角，遮罩 + blur（可选）；
- 输入 14sp，item 36dp，分组标题 9sp uppercase；
- 底部快捷键提示；
- 复用 token，不新增硬编码颜色。

---

## 7. 全局快捷键

- 完整快捷键在上表；实现后更新设计文档/界面提示。
- N/R 只在没有文本焦点时触发。
- ⌘W 关闭当前 workspace tab；在 Connection/Settings 页面无效果。
- ⌘⇧L 切换主题并持久化。
- ⌘, 打开设置（可选，若实现则加入表格）。
- 命令面板打开时，快捷键优先由面板处理。

---

## 8. 非目标

- 不实现 i18n（英文/日文）切换。
- 不实现自定义快捷键映射。
- 不实现检查更新/自动更新。
- 不实现设置云同步。
- 不实现主题编辑器/自定义颜色。

---

## 9. 验收

```bash
./gradlew ktlintCheck
./gradlew :app:jvmTest
./gradlew build
./gradlew :desktopApp:run
```

- [ ] 设置页 6 个 section 均可到达，导航滚动/高亮正常。
- [ ] 主题三态正确：选浅色后重启仍浅色；跟随系统时主题卡高亮“系统”。
- [ ] 强调色切换即时影响按钮/选中态/图表；mint/amber 上文字可读。
- [ ] 密度切换影响间距/行高，不破坏布局。
- [ ] 编辑器设置影响 CodeEditor：默认格式、字号、换行、行号。
- [ ] SCAN 批量、Key 视图、TTL 徽标、是否确认删除都真实生效。
- [ ] 全局只读立即禁用编辑/控制台写命令；状态栏显示只读。
- [ ] 错误密码加密开关行为清晰；文件内容符合预期。
- [ ] 隐藏敏感值在 Hash 编辑器中生效，可逐行显示。
- [ ] 危险命令确认在控制台生效。
- [ ] 命令面板：⌘K、过滤、连接跳转、新建连接、刷新、切换主题均可用。
- [ ] 快捷键表中的项全部真实可用；未实现的不要列出。
- [ ] 设置修改 300ms 内保存，重启保持。
- [ ] 更新 `STATUS.md`。

---

## 10. STOP

停止。Phase 10 进行全量打磨与发布准备。
