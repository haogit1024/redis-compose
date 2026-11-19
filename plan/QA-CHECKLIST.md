# Redis Compose — Phase 10 QA Checklist

> 本清单用于最终发布验收。Agent 已执行构建、单测、离屏渲染与启动检查；真实 Redis 手动路径需要在本机或 CI 的 Redis 环境补跑。

## 0. 环境

- macOS（本轮验证）、Windows/Linux 需在对应平台补跑打包与窗口检查。
- JDK 17+（本轮 Gradle 使用 JDK 21 执行 jpackage）。
- 可选：Docker 或本地 `redis-server`（本轮 Agent 环境没有 Docker/Redis，因此真实命令路径未执行）。

## 1. 构建与静态检查

| 命令 | 期望 | 结果 |
| --- | --- | --- |
| `./gradlew clean build` | 成功 | 通过 |
| `./gradlew ktlintCheck` | 无违规 | 通过 |
| `./gradlew :app:jvmTest` | 全部通过 | 通过（233 tests，见 STATUS） |
| `./gradlew :desktopApp:run` | 窗口启动，无异常 | 通过（进程存活 ≥ 20s） |
| `./gradlew :desktopApp:packageDistributionForCurrentOS` | 生成当前平台包 | 通过（DMG） |

## 2. 测试数据准备（建议）

```bash
docker run --rm -p 6379:6379 redis:7.4
```

```bash
redis-cli -n 0 FLUSHDB
redis-cli -n 0 SET user:1 '{"name":"Ada","age":36,"active":true}'
redis-cli -n 0 SET plain "hello redis compose"
redis-cli -n 0 SET big "$(python3 - <<'PY'
print('x' * 1500000)
PY
)"
redis-cli -n 0 HSET cache:user:1001:profile name Ada email ada@acme.io plan scale password s3cret
redis-cli -n 0 RPUSH queue:jobs job:1 job:2 job:3
redis-cli -n 0 SADD feature:flags beta checkout
redis-cli -n 0 ZADD leaderboard 42 ada 17 grace
redis-cli -n 0 XADD events:stream '*' type login user ada
redis-cli -n 0 SET ttl:short alive EX 30
# 可选：制造大量 Key
for i in $(seq 1 10000); do echo "SET bulk:key:$i $i"; done | redis-cli -n 0 >/dev/null
```

非 UTF-8 值可用应用内 HEX 展示验证；或使用 `redis-cli -x SET binary:key < file.bin`。

## 3. 手动功能走查

| 路径 | 检查点 | 状态 |
| --- | --- | --- |
| 首次启动 | 无连接空状态、命令面板可打开、无占位文案 | Agent：离屏/启动验证通过；真实点击未执行 |
| 连接库 | 新建 → 测试连接 → 保存 → 打开工作区 | 未执行（无 Redis） |
| 多连接 | 同时打开、切换 tab、关闭 tab、删除连接级联关闭 | 单测覆盖 AppState/tab 生命周期 |
| 键浏览 | SCAN 分页、搜索、类型筛选、树/平铺、置顶、TTL | 离屏 fixture 通过；真实 10k Key 未执行 |
| 值编辑器 | String JSON/文本/HEX、Hash/List/Set/ZSet 编辑、Stream 只读、非 UTF-8 集合成员不误写 | 单测 + 离屏覆盖；真实写路径未执行；集合 binary 行仍需专项修复 |
| Key 操作 | TTL、重命名、删除、新建 5 种类型 | 单测覆盖；真实写路径未执行 |
| 只读 | 全局只读拦截值编辑/控制台/删除，状态栏徽标 | 单测覆盖；离屏状态检查 |
| 控制台 | 常用命令、嵌套数组、错误、历史、补全、危险命令确认 | 单测 + 离屏；真实命令未执行 |
| 洞察 | 5 秒采样、区间、图表、慢查询、客户端、健康、离开停止 | 单测 + 离屏；真实服务端未执行 |
| 设置 | 6 个 section、主题/强调色/密度、编辑器、浏览、安全、快捷键、关于 | 离屏深/浅/紧凑 + 持久化单测 |
| 命令面板 | 页面/操作/连接/最近命令、过滤、键盘、空状态 | 离屏 + 代码检查 |
| 快捷键 | Cmd/Ctrl 组合、N/R 输入保护、Esc、窗口 1120×720 | 代码检查；macOS 未授权自动按键 |
| 重启保留 | 设置/连接/主题保留 | 临时目录持久化单测 |
| SSH/TLS | 失败路径与配置构建 | 单测；真实网络未执行 |

## 4. 视觉检查（Agent 离屏）

| 页面 | 尺寸/主题 | 结果 |
| --- | --- | --- |
| Key 浏览器 | 1074×770 深/浅、802×590 深、超长 Key | 无溢出；长 Key 3 行省略 |
| 控制台 | 1074×770 深/浅、802×590 浅 | 历史栏/终端/composer 正常；窄宽隐藏策略符合实现 |
| 连接库 | 1380×816 深/浅、1060×636 浅、超长名称/Host | 3/2 列切换，长文本省略 |
| 洞察 | 1074×770 深、802×590 浅 | 卡片/图表布局正常；单帧采样为空态 |
| 设置 | 1380×816 深、1060×636 浅 | section/导航/行高正常；滚动条可见 |
| 外壳 | 1120×720 深空状态、1440×900 浅连接库 | 顶栏/rail/侧栏/状态栏无溢出 |
| 命令面板 | 1000×700 深/浅 | 分组、连接状态点、最近命令、底部提示正常 |

允许的差异与已知限制见 `README.md` 与 `plan/STATUS.md`。

## 5. 性能与稳定性代码检查

- [x] Key 列表 `LazyColumn`；TYPE/TTL 只对可见行懒加载，并发 6。
- [x] SCAN 分页与搜索 debounce；切换 Key 取消旧详情请求。
- [x] 1MB+ String 截断提示，不截断原始数据；保存 loading 防重复。
- [x] List/Set/ZSet/Stream 使用 `LazyColumn`。
- [x] 控制台/输出/数组/行数/entry 上限。
- [x] 洞察仅保留 1 小时/720 样本，离开 tab 停止轮询。
- [x] 关闭 tab/session 取消 scope 与 gateway；`AppState.close()` 清理 state 与轮询。
- [x] 设置 300ms debounce，退出时 flush 待写快照。
- [x] 除启动时读取小文件（`createDesktopAppContainer`，Plan §8.1 允许）外，无主线程网络/文件 IO。
- [x] 窗口失焦时暂停自动摘要刷新。

## 6. 可访问性检查

- [x] 图标按钮强制 `contentDescription`；Rail/工具栏有 tooltip。
- [x] 按钮/图标按钮增加焦点边框；文本字段已有焦点环。
- [x] Esc 关闭顶层弹层；命令面板/对话框/Sheet 各自处理。
- [x] Value 标题、Key、控制台日志可选中复制；错误以文本 + 颜色表达。
- [x] 深浅主题正文/次级文本对比度检查（离屏）。
- [ ] Tab 遍历顺序的全量键盘回放：未自动化（macOS 未授权 osascript）。
- [ ] 屏幕阅读器语义树检查：未执行（需要人工/专用工具）。

## 7. 打包

- [x] `packageName = RedisCompose`，`bundleID = com.czh.rediscompose`。
- [x] macOS DMG 生成成功：`desktopApp/build/compose/binaries/main/dmg/RedisCompose-1.0.0.dmg`。
- [x] 应用图标已提供：`desktopApp/icons/app-icon.icns|ico|png`，DMG/App 内 `CFBundleIconFile` 指向 `RedisCompose.icns`。
- [x] Windows MSI / Linux DEB 目标已配置（本轮未在对应平台执行）。
- [ ] 代码签名/公证：未配置（需要开发者证书）。

## 8. 结论

Agent 环境可执行的构建、单测、静态检查、离屏视觉、启动与 macOS 打包均通过。真实 Redis 手动路径、Windows/Linux 打包、代码签名、屏幕阅读器与自动键盘回放需要在发布前由人工/CI 补跑。
