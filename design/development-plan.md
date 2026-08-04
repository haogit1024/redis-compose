# RedisCompose 开发计划

> 依据 [feature-design.md](./feature-design.md) 的 P0/P1/P2 路线图与 [ui-design.md](./ui-design.md) 界面规范制定。
> 预估为单人/双人开发节奏，每个里程碑约 1~2 周，可按实际情况压缩。

---

## 1. 总体目标

构建一个跨平台（首发 Desktop JVM）的 Redis 桌面管理工具，具备：连接管理、Key 树形浏览、全类型值查看编辑、命令行控制台、服务器运维面板，海量 Key 场景下不卡顿。

## 2. 关键技术决策

| 决策项 | 结论 | 理由 |
|--------|------|------|
| Redis 客户端 | **Jedis 5.x** | API 简单、阻塞模型配合 `Dispatchers.IO` 即可；Cluster/Sentinel 支持成熟 |
| 异步模型 | `suspend` 函数 + `Dispatchers.IO` | 与现有 Compose + Coroutines 体系一致 |
| 序列化 | `kotlinx.serialization`（已使用） | 连接配置 JSON 持久化 |
| 密码存储 | 本机 Keychain 不可用时的保底：AES 加密 + 随机密钥文件 | P1 阶段实现，MVP 先明文存储（本地文件） |
| 状态管理 | `StateFlow` + `collectAsStateWithLifecycle` | 遵循 AGENTS.md 架构约定 |
| 打包分发 | Compose `packageDmg` / `packageMsi` / `packageDeb` | M6 阶段配置 |

## 3. 里程碑规划

### M0 · 项目基建（约 2 天）

- [ ] 引入依赖：Jedis、kotlinx-coroutines-swing（已有）
- [ ] 建立主题系统：`Theme.kt` 扩展为浅色/深色双主题 + 设计令牌（色值对齐 ui-design §9）
- [ ] 设置页：外观/浏览/安全/通用四分区 + 设置项本地 JSON 持久化
- [ ] 搭建 `RedisClient` 接口（commonMain）+ `JedisRedisClient` 实现（jvmMain）
- [ ] 单元测试框架就绪：`./gradlew :shared:jvmTest` 可通过

**验收**：主题切换生效且设置页可持久化；`JedisRedisClient.ping()` 连通本机 Redis 的测试用例通过。

### M1 · 连接管理完善（约 3 天）

- [ ] 连接面板：搜索框、分组树、状态圆点、颜色标签、右键菜单
- [ ] 连接生命周期：双击连接 → AUTH/SELECT → 成功后展开 DB 列表
- [ ] 连接失败处理：错误 Toast + 状态标红
- [ ] `NewConnectionDialog` 升级为分节表单（基础/SSH/SSL/高级折叠区）+ 测试连接按钮

**验收**：可增删改查连接并持久化；连接成功展示 DB 列表，失败有明确错误提示。

### M2 · Key 浏览（约 4 天）

- [ ] 工作区 Tab 栏（打开/关闭/切换）
- [ ] SCAN 分页加载（COUNT 1000，cursor 续查，滚动触底加载）
- [ ] 平铺列表视图：类型徽标、多选、右键菜单
- [ ] 树形视图：按分隔符聚合、折叠/展开、目录计数
- [ ] Key 搜索：通配符、搜索历史
- [ ] 状态栏：连接信息 + 已加载/总数

**验收**：10 万 Key 实例下滚动加载流畅不卡顿；树形/平铺切换正常；搜索生效。

### M3 · 值查看与编辑（约 5 天）

- [ ] 值面板骨架：Key 元信息头（类型/TTL/内存）+ 操作按钮
- [ ] String：多行编辑 + JSON 自动格式化
- [ ] Hash：field/value 表格 + 行内编辑 + HSCAN 分页
- [ ] List / Set / ZSet：对应表格编辑器
- [ ] 新建 Key 弹窗：String/Hash/List/Set/ZSet 五种类型的动态表单（对齐 ui-design §8.1）
- [ ] 删除/重命名/TTL 修改（危险操作二次确认）
- [ ] 值格式化器：Auto / Text / Hex

**验收**：五种基础类型全部可查看、编辑、新建、删除；大值截断展示正常。

### M4 · 命令行控制台（约 3 天）

- [ ] 控制台 Tab：命令输入、历史（↑↓）、结果输出区
- [ ] 命令自动补全（内置命令字典）
- [ ] 结果按类型着色；`KEYS` 等危险命令警告

**验收**：可执行任意命令并正确渲染数组/字符串/错误响应。

### M5 · 运维面板（约 3 天）

- [ ] 服务器信息页：概览卡片 + INFO 分区手风琴 + 5s 定时刷新
- [ ] 慢日志、客户端列表（可 KILL）
- [ ] 内存分析（SCAN 采样按前缀聚合）

**验收**：信息页实时刷新；慢日志/客户端列表可交互。

### M6 · 高级连接与发布（约 5 天）

- [ ] SSH 隧道（JSch 或 sshj）、SSL/TLS 连接
- [ ] Cluster / Sentinel 模式适配
- [ ] 连接分组持久化、只读模式
- [ ] 打包：macOS dmg（含图标/签名占位）、Windows msi
- [ ] 应用图标与窗口标题、版本号

**验收**：SSH 跳板可连通内网 Redis；dmg 双击安装可运行。

### 后续（不在本期承诺）

Stream 类型、发布订阅、Key 导入导出、多语言、快捷键自定义、RedisJSON 树形视图。

## 4. 任务依赖关系

```
M0 基建 ──► M1 连接 ──► M2 Key 浏览 ──► M3 值编辑
                              │
                              └──► M4 控制台 ──► M5 运维 ──► M6 高级/发布
```

## 5. 测试策略

| 层级 | 范围 | 工具 |
|------|------|------|
| 单元测试 | 值编解码、Key 树构建、通配符匹配、连接配置校验 | kotlin.test（jvmTest） |
| 集成测试 | Jedis 客户端对真实 Redis 的 CRUD | Testcontainers Redis 镜像 |
| 手动验证 | 海量 Key 性能、UI 交互、打包产物 | 脚本批量造 10 万 Key |

## 6. 风险与应对

| 风险 | 影响 | 应对 |
|------|------|------|
| Compose 大列表性能 | 海量 Key 卡顿 | LazyColumn + 分批加载，必要时虚拟滚动分页上限 |
| Jedis 阻塞 IO 卡 UI | 界面无响应 | 全部调用走 IO 调度器 + 超时控制 |
| SSH 隧道库选型 | M6 返工 | M1 阶段先做 SPIKE 验证 sshj |
| Cluster 模式下 RENAME 等限制 | 操作失败困惑 | 预检 slot，禁用并提示 |

## 7. 完成定义（DoD）

- 功能按里程碑验收项全部通过
- `./gradlew build` 无警告级错误，`./gradlew :shared:jvmTest` 全绿
- 遵循 AGENTS.md 编码规范（4 空格缩进、命名约定、组件带 Preview）
