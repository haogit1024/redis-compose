# 编码规范与 Compose 约定

> 适用于 `app` 与 `desktopApp` 的所有新代码。与现有代码冲突时，以本文为准；现有代码会在 Phase 0 被替换。

---

## 1. Kotlin

- 4 空格缩进；UTF-8；行宽建议 120；文件末尾换行。
- 使用 trailing comma；ktlint 必须通过。
- 只使用显式 import，禁止 `*` import（ktlint 默认）。
- 命名：
  - 包：全小写，按职责分层。
  - 类/对象/接口：`PascalCase`。
  - 函数/属性：`camelCase`；布尔用 `is` / `has` / `can` 前缀。
  - 常量：`UPPER_SNAKE_CASE`，只放 `const val`；普通只读值用 `PascalCase` 对象属性。
  - 枚举项：`UPPER_SNAKE_CASE`。
  - 文件：一个文件一个主类型；文件名与主类型同名。屏幕文件 `XxxScreen.kt`，状态文件 `XxxState.kt`。
- 避免缩写：`db` 在领域术语中可以保留，但代码字段用 `databaseIndex`；`cfg`/`mgr`/`svc` 禁止。
- 函数保持短小；超过 ~80 行考虑拆分。
- 文件超过 ~500 行考虑拆分（`@Preview` 多的设计系统文件可以例外）。

---

## 2. Compose API

- 所有公共 composable 参数：
  - 必填参数在前；
  - 可选参数有默认值；
  - `modifier: Modifier = Modifier` 必须是最后一个普通参数；
  - slot 参数（`content: @Composable () -> Unit`）放在最后，可带默认空实现。
- 不把 `Modifier` 传给内部结构；只在根部使用传入的 modifier。
- 不在 composable 中创建长期对象（repository / gateway / scope）；通过参数或 `remember`。
- 状态提升：
  - 设计系统组件尽量无状态；
  - feature 屏幕从 `XxxState` 读取，事件回调回 state；
  - 可复用局部 UI 状态（展开、hover）允许 `remember`。
- 副作用：
  - 初始化加载使用 `LaunchedEffect(Unit)`；
  - 监听状态变化用 `LaunchedEffect(key)`；
  - 不要在 composition 中发网络请求。
- 列表一律 `LazyColumn` / `LazyRow`；不要用 `Column + verticalScroll` 渲染长 Key 列表或表格。
- 使用 `key = { ... }` 提供稳定 key（Key 名、条目 id）。
- 动画只在有反馈意义时使用；时长见设计 token。
- 所有颜色/字号/圆角/间距使用 `LocalAppColors`、`AppTypography`、`AppDimens`。禁止 `Color(0xFF...)`、`12.sp`、`8.dp` 散落在 feature 中；确需一次性尺寸（如 `calc`）时使用 `AppDimens` 增加 token。
- 所有图标使用 `AppIcons.*`；禁止直接写 `Icons.Outlined.X` 到 feature。
- 所有卡片/输入框优先使用 `design/component` 里的组件，不要重复造按钮。

### 2.1 Preview

- 每个组件和屏幕提供 `@Preview`：
  - 组件：深色 + 浅色两个预览（至少一个）。
  - 屏幕：用 fake state 或 `remember` 的演示数据；不要连接真实 Redis。
- 预览必须包在 `AppTheme(dark = true/false, accent = Accent.IRIS)` 中。
- 预览命名：`AppButtonPreview`、`WorkspaceScreenPreview`。
- 预览文件放在同文件底部；如果文件过大，可拆 `XxxPreviews.kt`（不参与生产逻辑）。

---

## 3. 状态与协程

- State holder 不依赖 Compose UI；只用 `mutableStateOf` / `mutableStateListOf` / `mutableStateMapOf`。
- State holder 的公开修改方法必须：
  1. 校验参数；
  2. 取消冲突的旧 job；
  3. `scope.launch`；
  4. 在 IO 返回后更新状态；
  5. 捕获异常并调用 `onToast` 或设置 `errorMessage`。
- 不把整个 `AppState` 传给 feature；只传需要的回调/数据（见 domain-and-data）。
- 不让 composable 持有 `CoroutineScope` 做业务请求；scope 属于 state。
- 所有长任务可取消；关闭 tab/session 时取消。
- 连接类异常要保留首行为用户文案；详细堆栈只用于日志/详情。
- 不要在 `LaunchedEffect` 中 `while(true)` 轮询而不响应生命周期；Insights 的轮询用 `while (isActive)` + delay，并在 tab 关闭时取消。

---

## 4. 数据层

- UI/feature 不得 import `redis.clients.jedis.*`。
- Gateway 返回领域类型；ByteArray 只在 data 层和值编解码边界出现。
- 值编解码统一走 `ValueCodec`：
  - 合法 UTF-8 → 文本；
  - 非法 UTF-8 → HEX 展示；
  - 编辑 HEX 时按字节解析。
- Redis 命令必须参数化，禁止字符串拼接命令。
- 阻塞 IO 不能出现在主线程。
- 每个 gateway 实例一个 Mutex；`close()` 幂等。
- 日志使用 `println` 时加前缀 `[redis-compose]`；不要打印密码。

---

## 5. 文件与包依赖

允许的依赖方向：

```
desktopApp -> app
app/app -> feature, design, domain, data, util
feature -> design, domain, util, app/AppState (只读 + 回调)
design -> domain (只读类型，如 KeyType) / util
data -> domain
domain -> 无（纯 Kotlin）
util -> 无 UI
```

- `design` 不依赖 `data`。
- `feature` 不依赖 `data`。
- 跨 feature 复用逻辑放到 `util` 或 `domain`，不要在 A feature 里 import B feature 的 State。
- 平台代码放 `jvmMain`，common 通过接口注入，不用 expect/actual 除非确有必要。

---

## 6. 文案

- UI 中文；Redis 命令、字段名、类型名、协议术语保留英文。
- 同一文案不要散落多个字面量；导航、状态、空状态等重复项放到 `uiText` 常量对象（可选 `util/AppStrings.kt`）。
- 错误文案：
  - 连接失败：`连接失败：<首行>`
  - 保存失败：`保存失败：<首行>`
  - 删除确认：`确定删除 Key「<key>」？`
  - 只读拦截：`只读模式已开启，写操作被拦截`
- 不使用“成功!”等夸张语气；使用“已保存”“已连接”。

---

## 7. 可访问性与桌面习惯

- 图标按钮必须有 `contentDescription` 或 tooltip；Tooltip 文案与动作一致。
- 所有可点击区域至少 28dp 高；IconButton 视觉 30dp。
- 键盘：
  - Tab 可遍历主要操作；
  - Enter 执行主操作；
  - Esc 关闭顶层弹层/对话框/命令面板；
  - 焦点环使用 accent 2dp。
- 文本选择：Key、值、日志、错误详情要可选中复制。
- 空状态给出下一步操作（新建连接 / 新建 Key / 刷新）。
- 只读模式必须在值面板、控制台输入、删除/保存处有一致的拦截与提示。

---

## 8. 测试

- 纯函数必须有单元测试（见 `domain-and-data.md` §10）。
- 测试命名：`method_condition_expected` 或反引号中文描述；全项目统一一种，建议英文反引号：
  `fun \`build groups keys by separator\`()`
- 不引入 UI 测试框架，除非 Phase 10 明确需要。
- Fake gateway 放 `commonTest`，实现 `RedisGateway` 的内存版本。
- 测试不依赖网络、不依赖本机 Redis。

---

## 9. 提交流程

每个阶段：

```bash
./gradlew ktlintFormat   # 可选；确保不产生大量无关改动
./gradlew ktlintCheck
./gradlew :app:jvmTest
./gradlew :desktopApp:run
```

- 不提交 `build/`、`.gradle/`、临时截图。
- 不在同一提交里混入下一阶段的文件。
- 不删除 `design/`、`plan/`。
