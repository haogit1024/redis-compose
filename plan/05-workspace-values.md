# Phase 5 — Workspace：值检查器与编辑器

> **目标**：实现右侧值面板：Key 头部、元信息、JSON / Hash / List / Set / ZSet / Stream 编辑器，以及 TTL / 重命名 / 删除 / 新建 Key。
>
> 依赖：Phase 4  
> 下一阶段：Phase 6（控制台）

---

## 1. 开始前

- 阅读 `design/workspace.html` 的值面板与 `design/assets/pages.css` 中 `.value-*`、`.meta-*`、`.editor-*`、`.collection-*`、`.code-*`。
- 参考 `design/assets/shots/workspace.png`（JSON）与 `workspace-hash.png`（Hash）。
- 阅读 `plan/reference/domain-and-data.md` §3.2、§8.5。
- 不实现控制台完整页面、洞察、连接表单、设置表单。

---

## 2. 文件清单

```text
feature/workspace/
├── ValuePane.kt
├── ValueHeader.kt
├── ValueMetaGrid.kt
├── ValueEmptyState.kt
├── ValueLoadingState.kt
├── KeyActionsMenu.kt
├── editor/
│   ├── CodeEditor.kt
│   ├── StringEditor.kt
│   ├── HashEditor.kt
│   ├── ListEditor.kt
│   ├── SetEditor.kt
│   ├── SortedSetEditor.kt
│   ├── StreamEditor.kt
│   ├── CollectionTable.kt
│   └── EditorToolbar.kt
├── dialog/
│   ├── NewKeyDialog.kt
│   ├── TtlDialog.kt
│   ├── RenameKeyDialog.kt
│   └── DeleteKeyDialog.kt
└── WorkspaceState.kt           # 扩展详情/编辑/操作
design/text/
└── JsonHighlighter.kt
```

同步修改：

- `AppShell` 的 workspace KEYS 分支渲染 `WorkspaceScreen` 的完整值面板。
- `WorkspaceState.selectKey` 加载详情。
- `AppState.openTab(...CONSOLE)` 供“在控制台打开”使用（Phase 6 才实现完整控制台，本阶段可跳转并显示占位）。

---

## 3. WorkspaceState 扩展

新增状态：

```kotlin
var selectedKey by mutableStateOf<String?>(null)
var keyDetail by mutableStateOf<KeyDetail?>(null)
var detailLoading by mutableStateOf(false)
var detailError by mutableStateOf<String?>(null)
var ttlDialogKey by mutableStateOf<String?>(null)
var renameDialogKey by mutableStateOf<String?>(null)
var deleteDialogKey by mutableStateOf<String?>(null)
var newKeyDialogVisible by mutableStateOf(false)
```

新增动作：

```kotlin
fun selectKey(key: String)
fun refreshSelected()
fun saveString(text: String)
fun addHashField()
fun saveHashField(row: EditableHashField)
fun deleteHashField(row: EditableHashField)
fun addListMember(head: Boolean)
fun saveListMember(index: Long, value: String)
fun deleteListMember(index: Long, value: String)
fun addSetMember()
fun saveSetMember(row: EditableSetMember)
fun deleteSetMember(row: EditableSetMember)
fun addSortedSetEntry()
fun saveSortedSetEntry(row: EditableSortedSetEntry)
fun deleteSortedSetEntry(row: EditableSortedSetEntry)
fun renameKey(newName: String)
fun updateTtl(seconds: Long)        // -1 = persist
fun deleteSelectedKey()
fun createKey(draft: NewKeyDraft)
```

规则：

- 选择 Key 时取消上一次详情加载；旧结果不得覆盖新选择。
- 详情加载顺序：`TYPE` → `TTL` → `MEMORY USAGE` → `OBJECT ENCODING` → 值；彼此可并行，但用同一个 job 聚合。
- 值加载失败：`detailError` + 重试；不要清空已选 Key。
- 所有写操作成功后：
  - toast；
  - 刷新详情；
  - 更新对应缓存/TTL/大小；
  - 必要时刷新列表（删除/重命名/新建）。
- 写操作失败：保留用户输入，toast 错误，不静默回滚（除非数据已变，需明确提示）。
- 只读（连接级 `profile.readOnly` 或设置 `globalReadOnly`）：
  - 所有保存/添加/删除按钮 disabled；
  - 顶部显示 readonly 徽标；
  - state 层也要拦截（不能只靠 UI）。
- 非 UTF-8 值：`ValueCodec.decode` 返回 null 时展示 HEX，字符串编辑器进入只读；不要写回空字符串。
- 大值：超过 `settings.truncateValueKb` 时，展示前 N KB + 明确提示“已截断，仅展示前 N KB”；编辑与保存完整值需用户确认（v1 可禁用编辑截断值，提供“仍然编辑”按钮）。

---

## 4. 值面板布局

```text
Column(fillMaxSize)
├── ValueHeader
├── ValueMetaGrid
├── EditorCard (weight 1)
└── EditorFootnote
```

- 外层 `verticalScroll` 仅当内容超过视口；String 编辑器需要撑满剩余高度（`weight(1f)` + min 280dp），Hash/List 等表格自然高度。
- 内边距 20–24dp；最大宽度 1020dp，居中。
- 详情加载：保留头部骨架 + 编辑器骨架；不要整页 spinner。
- 未选择：`ValueEmptyState`，品牌图标 + “从左侧选择一个 Key 查看/编辑值”。
- 详情错误：`AppEmptyState` + 重试。

### 4.1 ValueHeader

- 类型 pill（含颜色）+ `JSON` 徽标（自动检测）+ `已置顶`（amber）+ `即将过期`（TTL <= 60s，danger）。
- Key 名：Mono 18sp/550，可换行，长 Key 不撑破；使用 `SelectionContainer` 可复制。
- 副行：`db0` · `host:port` · `OBJECT ENCODING xxx` · `更新于 …`（Mono 9.5sp）。
- 右侧动作（30dp IconButton）：
  - 置顶/取消置顶（amber star）
  - 复制 Key
  - 在控制台打开（Phase 6 完整；本阶段跳转到控制台占位）
  - 更多菜单：复制为命令、修改 TTL、重命名、删除。
- 不要放“保存”在这里；保存属于编辑器工具栏。

### 4.2 ValueMetaGrid

四格，间距 8dp，响应式：

- 宽 >= 960dp：4 列；
- 宽 < 960dp：2 列。

每格：

- label：9sp/600/大写/letterSpacing，带 10dp 图标；
- value：Mono 12sp；
- sub：Mono 8.5sp textQuaternary；
- TTL 格底部 2dp 进度条（ttl > 0 时），颜色 amber→rose，宽度按 TTL 比例（可简单用 min(100, ttl/3600*100)）。
- 字段：
  1. TTL：`永久` / `42s` / `28m 14s`；sub “无过期时间 / 剩余生存时间”。
  2. MEMORY：`1.4 KB` / `—`；sub `MEMORY USAGE`。
  3. ENCODING：`listpack` / `—`；sub `OBJECT ENCODING`。
  4. 根据类型：`VALUE SIZE`（String）、`FIELDS`（Hash）、`LENGTH`（List）、`MEMBERS`（Set/ZSet）、`ENTRIES`（Stream）；sub 为对应命令（`STRLEN` / `HLEN` / `LLEN` / `SCARD` / `ZCARD` / `XLEN`）。

### 4.3 通用编辑器卡片

- 外框 surface + border + 圆角 14dp。
- 顶部工具栏：标题 + 类型相关 badge/搜索 + 右侧操作。
- 底部状态条：命令提示 + 数量 + “已同步”状态。
- 表格统一 `CollectionTable`：固定表头、行高 34dp、hover 显示行操作、只读时隐藏操作。

---

## 5. StringEditor / CodeEditor

### 5.1 工具栏

- 标题 `Value`，带 code 图标；JSON 时显示绿色 `JSON detected`。
- 右侧：
  - 分段：`AUTO / JSON / TEXT / HEX`；
  - `格式化`（JSON 时显示）；
  - `保存` 主按钮（dirty 时可用，保存中显示 loading）。
- `HEX` 为只读展示；切换到 HEX 时隐藏保存。
- Text/JSON/Auto 可编辑；JSON 格式化失败时 toast “无效的 JSON”。

### 5.2 CodeEditor

实现一个可复用的代码编辑区：

- 背景 `codeBackground`，顶/底 padding 11dp，Mono 11sp，行高约 21sp。
- 行号 gutter：右侧 1dp border，Mono 9sp，颜色 codeLineNumber；与文本同一垂直滚动容器。
- 语法高亮：
  - 通过 `VisualTransformation` 返回 `AnnotatedString`；
  - token 颜色来自 `AppColors.jsonKey/jsonString/jsonNumber/jsonBoolean/jsonPunctuation`；
  - JSON 只在 `AUTO` 且内容合法或 `JSON` 模式启用；TEXT 不染色；
  - 复用 `design/text/JsonHighlighter.kt`。
- 编辑：
  - 使用 `BasicTextField`，`singleLine = false`；
  - 默认自动换行（设置项 Phase 9 接入），长行不强制横向滚动；
  - caret 颜色 accent；
  - 文本可选中复制；
  - 行号按逻辑行显示；自动换行时不要求视觉行号完全精确（v1 可接受，Phase 10 再优化）。
- 尺寸：String 编辑器卡片撑满可用高度（min 280dp，max 不设硬上限），代码区垂直滚动。
- 空值显示 placeholder “空字符串”；不要显示 null。

### 5.3 保存语义

- 初次加载：记录 `savedText`。
- 文本变化：`isDirty = current != savedText`。
- 保存：`gateway.setString`，成功更新 savedText，toast “已保存”；失败保留 dirty 与文本。
- 切换 Key / 关闭 tab 前若有 dirty：提示是否放弃（Phase 5 可先 toast 警告，Phase 10 完善确认）。
- 保存后刷新 TTL/memory/size（值大小可能变化）。

### 5.4 JSON 高亮工具

```kotlin
object JsonHighlighter {
    fun highlight(text: String, colors: JsonHighlightColors): AnnotatedString
}
```

- 支持对象/数组/字符串/数字/布尔/null/标点。
- 字符串中的转义引号不能截断 token。
- 非法 JSON 不抛异常：按普通文本或宽松着色。
- 单测：合法 JSON、嵌套、转义、非法输入、Unicode。

---

## 6. 集合编辑器

### 6.1 HashEditor

- 工具栏：`Hash fields` + `N fields` + 搜索（筛选 field）+ 新增字段 + 保存。
- 表格列：Field（30%）、Value、行操作。
- 行内编辑：`BasicTextField` 风格；field/value 均 Mono 11sp。
- 行操作：保存（check）、删除（trash），hover 显示。
- 逻辑：
  - 原 field 为空 = 新行；保存时 `HSET`。
  - field 改名 = `HDEL` 旧 + `HSET` 新；失败时按实际情况提示。
  - 删除 = `HDEL`。
  - 新增行：本地追加空行，不立即写 Redis；未填写时允许删除。
- 搜索只过滤显示，不修改数据。
- 空状态：“暂无字段” + 新增按钮。
- 只读：隐藏新增/保存/删除，文本不可编辑。

### 6.2 ListEditor

- 工具栏：`List members` + `N items` + 筛选 + LPUSH / RPUSH + 保存。
- 列：Index（46dp，右对齐）、Value、行操作。
- 索引变化后重新加载或本地重排；删除用 `LREM 0 value`（会删除所有相同值，需提示风险；或使用 `LSET` + `LREM` 方案）。v1 按当前行为 `LREM 0`，但 UI 提示“删除同值成员”。
- 新增：在头/尾插入空字符串；用户填写后保存。
- 保存后刷新整个列表（索引可能变化）。

### 6.3 SetEditor

- 工具栏：`Set members` + `N unique` + 筛选 + 添加成员 + 保存。
- 列：Member、行操作。
- 改名 = `SREM` 旧 + `SADD` 新。
- 删除 = `SREM`。
- 去重：重复成员提示。

### 6.4 SortedSetEditor

- 工具栏：`Sorted set` + `N members` + 筛选 + 添加成员 + 保存。
- 列：#（rank）、Member、Score（右对齐 + 小分数条）、行操作。
- score 解析为 Double；非法时提示，不提交。
- 改名 = `ZREM` 旧 + `ZADD` 新；改分数 = `ZADD`。
- 排序按 score 降序（设计如此），保存后重新加载排序。

### 6.5 StreamEditor

- v1 **只读**浏览：
  - 工具栏：`Stream entries` + `N shown` + `X groups` + 筛选。
  - 列：Entry ID、Fields（以 `field=value` 的 chip/行展示）；不假设固定字段名（设计稿的 event/actor 是演示数据，真实 Stream 字段任意）。
  - 点击条目可展开完整字段列表（可选 Dialog 或行展开）。
- 新增条目（XADD）为 Phase 10 可选；本阶段不实现，按钮禁用/不显示。
- 底部显示 “仅展示最近 N 条”。

### 6.6 通用表格行为

- 行 hover：surface2 背景；操作出现。
- 编辑未保存：行左侧 2dp amber 竖条 / 或保存按钮变 accent。
- 保存按钮 loading 时不重复提交。
- 只读模式不显示可编辑输入框，而是文本。
- 表格列宽固定，长值省略号 + tooltip/详情；双击可展开编辑（可选）。

---

## 7. Key 操作

### 7.1 更多菜单

- 复制为命令（按类型生成 `GET` / `HGETALL` / `LRANGE ... 0 -1` / `SMEMBERS` / `ZRANGE ... WITHSCORES` / `XRANGE - +`）。
- 修改 TTL。
- 重命名。
- 删除（danger）。

### 7.2 TTL Dialog

- 输入秒数；`-1` 表示永久。
- 校验整数；显示当前 TTL。
- 保存：`-1` → `persist`，否则 `expire`。
- 成功 toast；刷新详情。

### 7.3 Rename Dialog

- 输入新 Key；非空、不能等于原名；`RENAME`。
- 成功：更新列表中的 Key、选中、详情、置顶集合；失败提示（目标已存在等）。

### 7.4 Delete Dialog

- 确认文案含 Key 名；默认按钮 danger。
- 成功：从 keys/tree/置顶中移除，清空选中，toast。
- 关闭当前详情并显示空状态。

### 7.5 New Key Dialog

- 宽 560dp，滚动 body。
- 类型选择：5 个类型卡片（STRING/HASH/LIST/SET/ZSET），带类型色；Stream 新建不常用，可暂不提供。
- 字段：
  - Key 名（必填，Mono，placeholder `user:1001:profile`）；
  - TTL 秒（默认 -1）；
  - 根据类型：
    - STRING：多行值；
    - HASH：field/value 一行；
    - LIST：一个成员 + 头/尾插入；
    - SET：一个成员；
    - ZSET：member/score。
- Redis 不能创建空集合，因此集合类型必须至少一个有效条目；校验并提示。
- 创建后：`refresh()`，选中新 Key，toast。
- 只读模式禁止打开。

---

## 8. 只读 / 大值 / 非 UTF-8 状态

- 只读 banner：编辑器卡片顶部一行，“只读模式 · 写操作已禁用”，amber/shield 样式。
- 全局只读或连接只读，两者任一开启都生效。
- 大值截断提示要明确：`已截断：仅显示前 10 KB（共 2.4 MB）`；提供复制完整值（通过 gateway 获取）或提示在设置中调整阈值。
- 非 UTF-8：HEX 视图只读；复制 HEX；不显示乱码；不提供文本编辑。

---

## 9. 非目标

- 不实现 command palette 扩展（Phase 9）。
- 不实现 Stream 写入 / 消费者组管理（Phase 10 可选）。
- 不实现批量 Key 操作。
- 不实现值差异对比（diff）。
- 不实现语法高亮的完整 JSON 格式化校验 UI（错误 toast 即可）。

---

## 10. 验收

```bash
./gradlew :app:jvmTest
./gradlew ktlintCheck
./gradlew build
./gradlew :desktopApp:run
```

- [ ] 选择 String Key：JSON 高亮、Auto/JSON/Text/HEX 切换正常。
- [ ] 编辑并保存 String；重载后值正确；TTL/大小刷新。
- [ ] 选择 Hash/List/Set/ZSet：表格数据正确，新增/编辑/删除/保存成功。
- [ ] ZSet score 非法输入被阻止并提示。
- [ ] Stream 只读展示真实字段，不假设固定列。
- [ ] TTL 修改（含永久）、重命名、删除全部可用，并同步列表/置顶/标题。
- [ ] 新建 Key 对 5 种类型可用；集合类型至少一条数据；只读时不可用。
- [ ] 只读模式下所有写入口禁用，state 层也拦截。
- [ ] 非 UTF-8 值进入 HEX 只读；大值截断提示正确。
- [ ] 切换 Key 时无旧详情覆盖新详情。
- [ ] 长 Key / 长值 / 空集合 / 加载失败 都有合理状态。
- [ ] `JsonHighlighter` 单测通过。
- [ ] 所有颜色/尺寸来自 token；表格行 hover/selected 与设计一致。
- [ ] 更新 `STATUS.md`。

---

## 11. STOP

停止。Phase 6 实现控制台；不要提前写洞察/连接库/设置。
