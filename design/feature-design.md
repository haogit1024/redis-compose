# 功能设计文档

> 参考：AnotherRedisDesktopManager（功能广度）、RESP.app（交互体验）
> 技术栈：Kotlin 2.4 + Compose Multiplatform 1.11 + Kotlinx Coroutines，目标平台 Desktop (JVM)

---

## 1. 模块总览

```
┌─────────────────────────────────────────────────────┐
│                    RedisCompose                     │
├──────────────┬──────────────┬───────────────────────┤
│  连接管理     │  数据浏览     │  运维工具              │
│  Connection  │  Browser     │  Ops                  │
├──────────────┼──────────────┼───────────────────────┤
│ · 连接 CRUD   │ · Key 树浏览  │ · 命令行控制台          │
│ · SSH 隧道    │ · 值查看/编辑 │ · 服务器信息/监控        │
│ · SSL/ACL    │ · 搜索过滤    │ · 慢日志               │
│ · Cluster    │ · 批量操作    │ · 客户端列表            │
│ · Sentinel   │ · TTL 管理    │ · 内存分析             │
│ · 分组/标签   │ · 导入导出    │ · 发布订阅             │
└──────────────┴──────────────┴───────────────────────┘
```

---

## 2. 连接管理

### 2.1 连接配置项

| 分组 | 字段 | 说明 |
|------|------|------|
| 基础 | 名称、Host、Port、用户名、密码 | 用户名用于 Redis 6+ ACL |
| 基础 | 默认 DB、Key 分隔符、连接颜色标签 | 分隔符默认 `:` |
| SSH | SSH Host/Port、用户名、密码/私钥(+passphrase)、超时 | SSH 隧道转发 |
| SSL | 启用开关、CA 证书、客户端证书、私钥 | TLS 连接 |
| 模式 | Standalone / Cluster / Sentinel | Sentinel 需 master name |
| 高级 | 只读模式、连接超时、读写超时、自动重连 | 只读模式拦截写命令 |

### 2.2 连接操作

- **新建 / 编辑 / 删除 / 复制连接**（弹窗表单，已有 `NewConnectionDialog` 基础）
- **连接分组**：文件夹式分组，支持拖拽排序（参考 ARDM 2026-07 特性）
- **连接搜索**：顶部过滤框，实时筛选连接名
- **双击连接** → 建立连接并进入 Key 浏览
- **右键菜单**：编辑、复制、删除、新窗口打开
- **连接状态指示**：绿点（已连接）/ 灰点（未连接）/ 红点（连接失败）
- **持久化**：连接列表本地加密存储（已有 `ConnectionStorage`），密码字段加密保存

### 2.3 连接生命周期

```
双击连接 → 建立通道(SSH?) → 握手(AUTH/SELECT) → 拉取 INFO 校验
   │                                              │
   ├─ 失败 → Toast 提示错误详情，连接标红            ├─ 成功 → 打开工作区 Tab
   └─ 断开 → 自动重连(可配置次数/间隔)               └─ DB 列表(db0~db15) 展示在左侧
```

---

## 3. Key 浏览与编辑（核心功能）

### 3.1 Key 列表加载

- **SCAN 增量加载**：每批 `COUNT 1000`，滚动到底部自动加载下一批（cursor 续查）
- **禁止 `KEYS *`**：超大实例保护
- **大 Key 保护**：值加载超时告警，可取消
- 显示已加载 Key 数 / 估算总数（`DBSIZE`）

### 3.2 树形视图

- 按分隔符（默认 `:`）把 Key 切分为目录树，如 `user:1001:profile` → `user/1001/profile`
- 目录节点显示子 Key 数量，支持折叠/展开/一键展开
- 可切换 **树形视图 / 平铺列表视图**
- 右键目录：批量删除、导出

### 3.3 搜索

- 顶部搜索框，支持 `*pattern*` 通配符
- **精确搜索**与**模糊搜索**切换
- 搜索历史记录（下拉建议）
- 大集合内部搜索：Hash / Set / ZSet / List / Stream 成员内搜索（HSCAN/SSCAN/ZSCAN）

### 3.4 值查看与格式化

| 数据类型 | 查看方式 | 编辑能力 |
|----------|----------|----------|
| String | 文本编辑器，自动识别 JSON 并格式化 | 直接编辑保存 |
| Hash | 表格（field / value），支持分页 | 增删改 field |
| List | 列表表格，头尾插入 | 按索引修改/删除 |
| Set | 表格，支持成员搜索 | 增删成员 |
| ZSet | 表格（member / score），按 score 排序 | 增删改成员 |
| Stream | 时间线表格，展示 Group/Consumer | 追加消息 |
| JSON | 树形 JSON 视图（可折叠） | JSON 编辑 |

值格式化器：**Auto / Text / JSON / Hex / Binary**（不可见字符以 Hex 展示）。

### 3.5 Key 操作

- **新建 Key**：选择类型 → 填写内容（每种类型专属表单）
- **删除**：单个/多选批量删除，二次确认；目录批量删除
- **重命名**（RENAME，Cluster 下跨 slot 禁用并提示）
- **TTL**：显示剩余 TTL，支持修改/清除（-1 永久）
- **复制**：复制 Key 名、复制值
- **内存占用**：`MEMORY USAGE` 显示单个 Key 内存

### 3.6 导入导出

- 导出 Key 为文件（文本/JSON）
- 从文件导入批量命令并执行

---

## 4. 命令行控制台

- 内嵌 RESP 命令行 Tab，连接共享同一通道
- **命令自动补全**与参数提示（内置命令字典）
- 历史记录（上下方向键）
- 命令结果格式化输出（表格/数组/原始）
- 只读模式下拦截写命令并提示

---

## 5. 服务器运维

| 功能 | 说明 |
|------|------|
| 服务器信息 | INFO 全量展示：版本、内存、命中率、连接数、运行时长 |
| 实时监控 | MONITOR 实时命令流（带警告：生产慎用） |
| 慢日志 | SLOWLOG 列表，可清空 |
| 客户端列表 | CLIENT LIST，可 KILL 指定客户端 |
| 内存分析 | 按前缀聚合统计 Key 数量/内存占用（SCAN 采样） |
| 发布订阅 | PUBSUB 频道订阅，实时消息流展示 |
| DB 管理 | FLUSHDB 危险操作需输入确认文本 |

---

## 6. 全局设置

- 主题：**浅色 / 深色 / 跟随系统**
- 字体：自定义字体族与字号（等宽字体用于值展示）
- Key 列表每批加载数量（默认 1000）
- 值展示截断阈值（超长值只展示前 N KB，点击"加载更多"）
- 语言：中文 / 英文
- 快捷键：`Cmd/Ctrl+F` 搜索、`Cmd/Ctrl+N` 新建连接、`Cmd/Ctrl+W` 关闭 Tab、`Cmd/Ctrl+D` 删除 Key

---

## 7. 分期路线图

### P0 —— MVP（核心可用）

- [x] 连接 CRUD + 本地持久化（已有基础）
- [ ] Standalone 连接（AUTH、SELECT DB）
- [ ] SCAN 分页加载 Key 列表 + 平铺视图
- [ ] String / Hash / List / Set / ZSet 查看与编辑
- [ ] Key 增删改、TTL 管理
- [ ] Key 搜索（通配符）
- [ ] 深浅色主题

### P1 —— 体验增强

- [ ] 树形视图 + 折叠
- [ ] 命令行控制台（补全 + 历史）
- [ ] Stream 类型支持
- [ ] 服务器信息面板 + 慢日志
- [ ] 多选批量删除、Key 导入导出
- [ ] 值格式化器（JSON 自动美化 / Hex）

### P2 —— 高级能力

- [ ] SSH 隧道、SSL/TLS
- [ ] Cluster / Sentinel 模式
- [ ] 连接分组与颜色标签
- [ ] 内存分析、客户端管理、发布订阅
- [ ] 只读模式
- [ ] RedisJSON 树形视图

---

## 8. 架构建议

```
UI 层 (Compose)          → ViewModel (StateFlow + collectAsStateWithLifecycle)
    │
领域层                    → ConnectionService / KeyService / ValueCodec
    │
数据层 (expect/actual)    → jvmMain: Jedis/Lettuce 封装 (RedisClient)
                          → ConnectionStorage (已有, JSON 文件持久化)
```

- 所有 Redis 调用走 `suspend` 函数 + `Dispatchers.IO`，UI 不阻塞
- `RedisClient` 接口定义在 commonMain，JVM 实现放 jvmMain，为后续平台扩展留口
- 值编解码（Text/JSON/Hex）为纯函数，可单元测试
