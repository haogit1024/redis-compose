# Phase 4 — Workspace：Key 浏览器

> **目标**：用真实 Redis 数据实现 Key 列表面板：SCAN 加载、树形/平铺、搜索、类型筛选、置顶、TTL/类型预览、空/错/加载状态。值面板留占位。
>
> 依赖：Phase 3  
> 下一阶段：Phase 5（值编辑器）

---

## 1. 开始前

- 阅读 `design/workspace.html` 的 `.keys-pane` 部分与 `design/assets/pages.css` 中 `.keys-pane`、`.tree-*`、`.filter-chips`、`.key-row`。
- 阅读 `plan/reference/domain-and-data.md` §3（Key 模型）、§8.5（WorkspaceState）。
- 复用 Phase 1 的 `AppSearchField`、`AppTypeChip`、`AppIconButton`、`AppEmptyState`。
- 本阶段不实现值编辑器、Key 详情、删除/重命名、新建 Key 表单。

---

## 2. 文件清单

```text
app/src/commonMain/kotlin/com/czh/rediscompose/feature/workspace/
├── WorkspaceScreen.kt           # 左 Key 面板 + 右占位值面板
├── WorkspaceState.kt            # 扫描、搜索、筛选、树、置顶、预览缓存
├── KeyListPane.kt               # 面板骨架
├── KeyPaneHeader.kt             # 标题/计数/操作
├── KeySearchFilter.kt           # 搜索 + 类型 chips
├── PinnedKeyList.kt
├── KeyTree.kt                   # 树 + 平铺 + 搜索结果
├── KeyRow.kt                    # 类型点、名称、TTL、星标
├── KeyPaneFooter.kt             # SCAN 进度
└── ValuePanePlaceholder.kt      # 右侧空/选中占位
```

同时修改：

- `app/AppState.kt`：增加 `workspaces: MutableMap<String, WorkspaceState>` 与生命周期管理。
- `feature/shell/AppShell.kt`：Workspace 页面 `KEYS` 分支渲染 `WorkspaceScreen`。
- `WorkspaceTabBar`：刷新按钮调用当前 WorkspaceState.refresh()。

---

## 3. WorkspaceState

按 reference 实现，并补以下行为：

### 3.1 加载 / 刷新

- `refresh()`：
  - 取消进行中的扫描/搜索任务；
  - 清空 keys、cursor、loadedAll、类型/预览缓存；
  - 重置 `total`；
  - 若没有搜索 pattern，先 `databaseSize()` 获取总数；
  - 连续 `loadMore()`（第一页立即，后续滚动触发）。
- `loadMore()`：
  - `isLoading` 防止重入；
  - `scan(cursor, pattern, settings.scanBatchSize)`；
  - 去重（SCAN 可能返回重复）；
  - `loadedAll = cursor == 0L`；
  - 重新构建 tree；
  - 错误：toast + `loadError` 状态，保留已加载数据。
- 自动加载下一页：KeyTree 接近底部时调用（比 LazyColumn 滚动监听或最后一项 `LaunchedEffect`）。

### 3.2 搜索

- `searchInput` 是输入框状态；300ms debounce 后调用 `applySearch(input)`（Phase 4 可用 `LaunchedEffect(searchInput)` + delay）。
- 空输入 → 清除 pattern，重新扫描全量。
- 非空 → pattern 原样传给 Redis `SCAN MATCH`（支持 `*`、`?`）。
- 搜索时清空旧 keys，显示加载状态；不要保留旧结果制造“假匹配”。
- `searchInput` 右侧显示清除按钮（有内容时）。
- ⌘F / Ctrl+F 聚焦搜索框。

### 3.3 类型筛选

- `typeFilter: KeyType?`，UI 为“全部 + String/Hash/List/Set/ZSet/Stream”chips。
- chips 使用类型色；激活背景 = 类型色 12%。
- 筛选在客户端对**已加载的 keys** 生效；未加载的 Key 不受影响，这是 SCAN 的固有限制。
- 类型未知的 Key：先显示 NONE 灰点并保持可见；`ensurePreview` 加载后更新类型；若与当前筛选不符，再隐藏。避免在类型未知时闪烁。
- “全部”清除类型筛选。

### 3.4 树 / 平铺

- 根据 `settings.keyView` 初始化：`TREE` / `FLAT`。
- 面板右上视图按钮切换，并更新 `settings.keyView`（Phase 9 持久化；本阶段可通过 AppState.updateSettings 保存）。
- 树：
  - `KeyTreeBuilder.build(keys, session.profile.keySeparator)`；
  - `expandedDirs` 默认全部收起，只有用户点击目录后才展开；刷新与分页加载保留展开状态；搜索时改为平铺；
  - 目录行：chevron、folder 图标、名称、count；点击切换；
  - Key 行缩进 = depth × 14dp + 基础 8dp；
  - 目录左侧 1dp 竖向引导线（可用 Box 绘制）；
  - 目录排在前面，再按字典序。
- 平铺：直接按名称显示完整 Key。
- 搜索或类型筛选激活时，用**搜索结果平铺列表**替代树，显示完整 Key + 类型 + TTL；顶部显示“N 个结果”，清除按钮。

### 3.5 置顶

- `pinnedKeys: Set<String>` 仅当前 WorkspaceState 内存保存（v1 不持久化）。
- 顶部“置顶”区展示完整 Key 名；星标可点击取消。
- Key 行 hover 显示星标，点击 pin/unpin；点击行本身选择 Key。
- 置顶区与树中的同一个 Key 选中态同步。
- 置顶行不重复触发扫描；如果 Key 已被删除（Phase 5 后），从置顶集合移除。

### 3.6 类型 / TTL 预览缓存

- `keyTypes: SnapshotStateMap<String, KeyType>` 与 `keyTtl: SnapshotStateMap<String, Long>`。
- 每行首次组合时调用 `ensurePreview(key)`：
  - 同一 Key 的 in-flight 请求去重；
  - 最多 6 个并发（`Semaphore` 或计数器）；
  - 失败静默（显示灰点），不 toast 风暴；
  - 缓存命中不重复请求。
- “显示 TTL 徽标”由 settings 控制；关闭时仍可缓存但 UI 不显示。
- 只对当前可见行请求（LazyColumn 懒组合），不要为全部 10k Key 发 TTL/TYPE。

### 3.7 选择

- 点击 Key 设置 `selectedKey`，并调用 `selectKey(key)`；
- Phase 4 右侧只显示占位（Key 名 + “值加载将在下一阶段实现”不要写进最终 UI，用“正在读取值…”的占位）；
- 选择时不要阻塞列表；
- 选中的 Key 在树/置顶/搜索结果中高亮（左侧 2dp accent 条 + accentSoft 背景）。

### 3.8 面板尺寸

- Key 面板默认 316dp，可拖拽 240–420dp。
- 分隔线 4dp，hover 显示 resize 光标；拖动时只更新宽度，不做动画。
- 宽度用 `rememberSaveable` 或 AppState 保存；窗口最小 1120dp 时保证值面板至少 420dp。

---

## 4. UI 细节

### 4.1 KeyListPane

- 顶部 12dp 内边距。
- 标题行：`Keys`（12sp/600）+ 总数（Mono 9.5sp/textQuaternary）；右侧 +、刷新、视图切换。
  - + 按钮本阶段可 toast “新建 Key 将在下一阶段提供”，或禁用并 tooltip “Phase 5”。
- 搜索框：高 30dp，Mono 11sp，placeholder “匹配模式 user:* · ⌘F”。
- 类型 chips：可换行（设计为两行也接受）；每个 chip 含类型点 + 文本？设计 v2 使用纯文本 + 激活类型色。按 `design` 最新：无圆点，激活时才用类型色。
- 树区域 `LazyColumn`，背景透明，底部留 12dp。
- 底部 footer：db 名、SCAN 进度条、`loaded / total`（total 未知时只显示 loaded）。

### 4.2 KeyRow

- 高 28dp，圆角 6dp，水平内边距 7–9dp。
- 类型点 6dp，带 3dp 类型色 11% 光晕。
- Key 名 Mono 10.5sp，单行省略。
- TTL：Mono 8.5sp/textQuaternary，仅在设置开启且 TTL > 0 时显示；TTL <= 60s 用 danger 色。
- 星标默认隐藏，hover/selected/已置顶时显示；已置顶为 amber。
- 行选中：accentSoft 背景 + 左侧 2dp accent 条。
- 空目录/无结果：`AppEmptyState` 小型版本，文案“当前 DB 没有 Key” / “没有匹配的 Key”。

### 4.3 加载 / 错误

- 首次加载：列表内 3–4 行骨架或居中 Loading；不要全屏遮挡。
- 分页加载：底部一行 `加载中…` / `点击加载更多`；SCAN 可能返回空页或重复 Key，单次加载会继续扫描直到新增 Key 或迭代结束，手动点击后提示本次新增数量；失败时该行变为 `加载失败 · 点击重试`。
- 错误：面板内 `AppEmptyState` + 重试按钮，保留已加载内容。
- 全部加载完：footer 显示 “已加载全部 N 个 Key”。

### 4.4 右侧占位（本阶段）

- 未选择：品牌图标 + “从左侧选择一个 Key 查看值”。
- 已选择：Key 名 + 类型 pill + “正在读取…”/“值编辑器即将在 Phase 5 实现”的占位（最终会被替换）。
- 不要让占位看起来像最终界面。

---

## 5. 交互

- ⌘F / Ctrl+F：聚焦搜索。
- Esc：搜索框有内容时清空；否则移除 Key 选择。
- N：本阶段 toast（Phase 5 接新建 Key）。
- R：刷新当前 workspace（避免在输入框聚焦时触发）。
- 上下箭头：可选，列表键盘导航（Phase 10 完善）。
- 右键 Key：本阶段只提供“复制 Key”；重命名/删除/TTL 在 Phase 5。

---

## 6. 非目标

- 不加载 Key 详情/值。
- 不实现新建/重命名/删除/TTL 对话框。
- 不实现批量选择、多选删除。
- 不实现搜索结果高亮（可 Phase 10）。
- 不实现 Key 收藏持久化（v1 内存即可）。

---

## 7. 验收

```bash
./gradlew :app:jvmTest
./gradlew ktlintCheck
./gradlew build
./gradlew :desktopApp:run
```

- [ ] 连接真实 Redis 后，Key 列表能加载并分页；10k Key 滚动流畅。
- [ ] 搜索 `user:*` 返回匹配项；清空后恢复全量；快速输入不会出现旧结果覆盖新结果。
- [ ] 类型筛选只影响已加载 Key，切回“全部”恢复。
- [ ] 树形展开/收起正常，目录计数正确，分隔符为空时退化为平铺。
- [ ] 置顶/取消置顶同步树、搜索结果与顶部区域。
- [ ] 选中行高亮；右侧占位显示正确 Key。
- [ ] TTL 徽标只在开启时显示，<=60s 使用 danger 色。
- [ ] 面板拖拽范围 240–420dp；窗口 1120dp 时值区仍可用。
- [ ] 空 DB / 无匹配 / 加载失败 都有明确状态。
- [ ] 不直接调用 gateway；所有请求由 WorkspaceState 发起。
- [ ] 关闭 tab 后扫描任务取消，无后台请求残留。
- [ ] 更新 `STATUS.md`。

---

## 8. STOP

停止。Phase 5 负责值面板、6 种类型编辑器与 Key 操作。
