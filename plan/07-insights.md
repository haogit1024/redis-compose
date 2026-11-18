# Phase 7 — 服务器洞察

> **目标**：实现基于真实 `INFO`、`SLOWLOG`、`CLIENT LIST`、`COMMANDSTATS` 的指标页。Redis 不提供历史数据，因此图表使用**本次会话内采样**；设计稿中的 24h 数据是示意，必须诚实替换。
>
> 依赖：Phase 3  
> 下一阶段：Phase 8（连接库）

---

## 1. 开始前

- 阅读 `design/insights.html` 与 `design/assets/pages.css` 的 `.insights-*`、`.kpi*`、`.chart-*`、`.donut*`、`.bar-list`、`.health-*`、`.mini-table`。
- 阅读 `plan/reference/domain-and-data.md` §4、§8.5。
- 本阶段不引入图表库；用 Compose Canvas 自绘。
- 不实现连接库、设置页。

---

## 2. 与设计稿的有意差异

| 设计稿 | 实现 |
| --- | --- |
| 1H / 6H / 24H / 7D 历史区间 | `5M / 15M / 1H / 会话`，数据来自本次打开页面后的采样 |
| 命令 / 读取两条 24h 曲线 | 总 Ops/s 与读命令 Ops/s（本次会话采样） |
| 内存分布 donut（数据集/开销/可用） | used vs max 的 donut；未设置 maxmemory 时显示“未设置上限”状态 |
| 复制/持久化健康卡 | 从 `INFO replication` / `INFO persistence` 真实解析 |
| 慢查询 10 条 | `SLOWLOG GET 10`，按真实命令渲染 |
| 客户端分布 | `CLIENT LIST` 按 name 分组统计 |

这些差异必须在 `plan/STATUS.md` 记录，并在 UI 副标题写“自本次连接”，不伪造历史。

---

## 3. 文件清单

```text
feature/insights/
├── InsightsScreen.kt
├── InsightsState.kt
├── InsightsRange.kt
├── InsightsHeader.kt
├── KpiGrid.kt
├── ThroughputCard.kt
├── MemoryCard.kt
├── CommandDistributionCard.kt
├── KeyspaceCard.kt
├── HealthCard.kt
├── SlowLogCard.kt
├── ClientsCard.kt
└── chart/
    ├── Sparkline.kt
    ├── AreaChart.kt
    ├── DonutChart.kt
    └── ChartPrimitives.kt   # 轴、网格、图例、路径平滑
```

同步修改 `AppShell` 的 INSIGHTS 分支与 `AppState` 的 insights state 生命周期。

---

## 4. InsightsState

```kotlin
enum class InsightsRange(val seconds: Long) {
    LAST_5_MINUTES(300),
    LAST_15_MINUTES(900),
    LAST_1_HOUR(3_600),
    SESSION(Long.MAX_VALUE),
}

class InsightsState(
    private val session: RedisSession,
    private val scope: CoroutineScope,
) {
    var snapshot by mutableStateOf<ServerSnapshot?>(null)
    var commandStats by mutableStateOf<List<CommandStat>>(emptyList())
    var slowLog by mutableStateOf<List<SlowLogEntry>>(emptyList())
    var clients by mutableStateOf<List<ClientConnection>>(emptyList())
    val samples = mutableStateListOf<MetricsSample>()
    var range by mutableStateOf(InsightsRange.LAST_5_MINUTES)
    var autoRefresh by mutableStateOf(true)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun start()
    fun stop()
    fun refreshNow()
    fun setRange(range: InsightsRange)
}
```

### 4.1 采样

- `start()`：立即刷新一次，然后每 5 秒一轮；用 `while (isActive)`。
- 每轮：
  - `INFO` → `InfoParser.snapshot`；
  - `INFO commandstats` → `commandStats`；
  - 记录 `MetricsSample(at, usedMemory, opsPerSecond, hitRate, clients, totalCommands, commandCalls)`；
  - 每 3 轮（15s）刷新 `SLOWLOG` 与 `CLIENT LIST`；
- 最多保留 1 小时样本（5s 间隔 = 720 条）；裁剪旧样本。
- `stop()` 取消 job；切换 tab 离开页面时调用（`DisposableEffect`）。
- 重新进入页面保留已有 samples，并立即刷新一次。
- `autoRefresh = false` 时停止轮询但保留数据，手动刷新可用。
- 错误：设置 `error`，保留旧数据；不要每 5 秒弹一次 toast。

### 4.2 派生指标

- 总 Ops/s：优先 `ServerSnapshot.instantaneousOpsPerSecond`；缺失时用相邻样本 `totalCommands` 差值 / 秒。
- 读 Ops/s：对读命令集合（GET/HGETALL/LRANGE/SMEMBERS/ZRANGE/SCAN 等）的 `commandstats` calls 差值 / 秒。
- 命中率：`keyspace_hits / (hits + misses)`；分母 0 时显示 `—`。
- 内存使用率：`used / maxmemory`；maxmemory 为 0/空 → 无上限。
- 命令分布：相对上一采样的 calls 差值；首次采样为 0 时显示“正在采样”，不要用累计值假装近期分布。

### 4.3 区间过滤

- 按 `range.seconds` 过滤 samples；SESSION 显示全部。
- 样本 < 2：图表显示“正在收集数据…”的空状态，不要画假曲线。
- 区间切换只影响图表与 KPI 的“均值”口径；瞬时 KPI 使用最新样本。

---

## 5. 界面

### 5.1 InsightsScreen

- 可滚动页面，最大宽度 1220dp，内边距 22–24dp。
- 页头：
  - 眉标 `OBSERVABILITY`；
  - 标题“服务器洞察”+ `LIVE` 徽标（mint 点，2.4s 呼吸）；
  - 副标题：`host:port` · Redis 版本 · 运行时长 · “自本次连接” · 最后更新。
  - 右侧：区间 segmented（5M/15M/1H/会话）+ 自动刷新开关/暂停 + 刷新按钮。
- KPI 四列（窄窗 2 列）：
  1. Used memory：`1.24 GB / 2 GB`，无上限时 `1.24 GB`，delta 通过样本差值；
  2. Ops/s：均值 + 当前；
  3. Cache hit rate：`98.7%`；
  4. Connected clients：当前 / max（如果 INFO 有 maxclients）。
  - 每张卡带 Sparkline（由 samples 生成；样本不足时不画）。
- 主网格：吞吐量（1.65fr）+ 内存（1fr）。
- 底部三列：命令分布、键空间表、健康检查。
- 再下一行：慢查询 + 客户端（两列）。
- 页面底部一行数据来源说明。

### 5.2 吞吐量卡

- 标题“吞吐量”，副标题 `自本次连接 · 5s 采样`。
- 图例：命令（accent 面积/线）、读取（mint 线）。
- AreaChart：
  - 网格 3 条水平虚线；Y 轴 0 / 中间 / 最大值，自动取整；
  - X 轴时间标签 4–6 个；
  - 面积渐变 accent 34% → 0；
  - 线条 2dp，圆角连接；
  - 绘制动画 900ms。
- 无数据：空状态 + “保持页面打开以收集样本”。

### 5.3 内存卡

- 有 maxmemory：donut used/free，中心百分比；图例 used、peak、fragmentation；底部峰值、淘汰数。
- 无 maxmemory：不画 donut；显示 used / peak 与“未设置 maxmemory”提示，fragmentation ratio。
- 指标来源 `INFO memory`；字段缺失显示 `—`。

### 5.4 命令分布

- 横向条形列表：命令名、条形（类型色/统一色）、百分比。
- 数据来自会话内 calls 差值；首次无差值时显示“正在采样…”。
- 最多 6 条 + “其他”。

### 5.5 键空间

- 表格：DB、Keys、Expires、TTL 均值。
- 数据 `INFO keyspace`；空 DB 显示 0；没有 keyspace section 时显示空状态。
- 最多显示 16 行，超出滚动。

### 5.6 健康检查

- 行：主从复制（role、replicas）、RDB（最近保存时间）、AOF（enabled/大小）、内存水位（used/max）。
- 状态颜色：正常 mint，关注 amber，异常 danger；每个状态都有文字，不只靠颜色。
- 缺字段显示“未知”，不要默认“正常”。

### 5.7 慢查询

- `SLOWLOG GET 10` 表格：命令（截断）、客户端、耗时（ms，>10ms amber）、时间。
- 空状态：“没有慢查询记录”。
- 行点击可复制完整命令。
- 详见 `design/insights.html` 的表格样式。

### 5.8 客户端

- 顶部四个 mini stat：应用连接、管理会话、空闲、最大连接。
- 下方按 `name` 前缀分组（worker-* 等）的条形列表；空 CLIENT LIST 时显示连接数 + 空状态。
- 不展示客户端 IP 列表（隐私），除非用户展开。

---

## 6. 图表实现

### 6.1 通用

- 全部用 `Canvas` + `drawWithCache`；不引入图表库。
- 尺寸自适应：`BoxWithConstraints`，宽度变化重新计算路径。
- 颜色只来自 `AppColors`；网格线用 border；文字用 textQuaternary。
- 坐标轴标签 7.5–8sp Mono；不要为了“高级感”去掉刻度，密度页需要可读。

### 6.2 路径平滑

- 用三次贝塞尔的中点平滑（与设计 HTML 的 JS 相同思路）：
  对每对点 `(x0,y0)-(x1,y1)`，控制点 `(mx,y0)` 和 `(mx,y1)`，`mx=(x0+x1)/2`。
- 面积路径：线路径 + 底边闭合。
- 数据为空或 1 点：不画路径。

### 6.3 动画

- 绘制进度 `animateFloatAsState(0f→1f, 900ms)`；路径按进度裁剪（简单可用 alpha 渐入，不强制）。
- 离开页面停止动画无关资源。

---

## 7. 性能 / 生命周期

- 页面不可见时停止轮询；`InsightsScreen` 用 `DisposableEffect(state) { state.start(); onDispose { state.stop() } }`。
- 切换区间不重新请求服务端，只重绘。
- 样本列表变化导致重组时，图表用 `remember(samples, range, size)` 缓存路径。
- 刷新失败不覆盖已有数据；错误条在页头下方。
- 只读不影响指标页。

---

## 8. 非目标

- 不实现跨会话历史存储/数据库。
- 不实现告警规则、阈值配置、导出报告。
- 不实现 Redis Cluster 节点拓扑图。
- 不实现 Pub/Sub、Monitor。
- 不引入监控后端。

---

## 9. 验收

```bash
./gradlew :app:jvmTest
./gradlew ktlintCheck
./gradlew build
./gradlew :desktopApp:run
```

- [ ] 页面打开后 5–10 秒内 KPI 有真实值，副标题显示“自本次连接”。
- [ ] 吞吐量与内存图随采样增长；样本不足时显示空状态而非假数据。
- [ ] 区间切换正确过滤；会话区间显示全部。
- [ ] 命令分布使用会话内差值；首次显示采样中。
- [ ] 慢查询来自真实 SLOWLOG；空时正确显示。
- [ ] CLIENT LIST 分组统计正确。
- [ ] 无 maxmemory 时内存卡不误报百分比。
- [ ] 页面离开后轮询停止（日志/测试可验证），返回后恢复并保留样本。
- [ ] 1120×720 下 KPI 2 列、图表不溢出。
- [ ] 深/浅主题下图表与文字对比可读。
- [ ] 不使用外部图表库；不硬编码假指标。
- [ ] 更新 `STATUS.md`（记录与设计稿的区间差异）。

---

## 10. STOP

停止。Phase 8 实现连接库与连接表单。
