# Phase 8 — 连接库与连接表单

> **目标**：实现连接库页面、连接卡、筛选/搜索/视图切换、最近连接、新建/编辑侧滑表单、测试连接、删除/复制，以及 TLS/SSH 的实际连接能力。
>
> 依赖：Phase 3（外壳与 AppState）、Phase 2（数据层）  
> 下一阶段：Phase 9（设置 + 命令面板扩展）
>
> 如果一次上下文不足以完成 UI + SSH 隧道，可拆成 8A（连接库与表单）与 8B（TLS/SSH 运行时），但**两批都完成才算 Phase 8 验收**。

---

## 1. 开始前

- 阅读 `design/connections.html` 与 `design/assets/pages.css` 的 `.connections-*`、`.conn-*`、`.sheet-*`、`.field*`。
- 参考 `design/assets/shots/connections.png`。
- 阅读 `plan/reference/domain-and-data.md` §2、§6、§7。
- 不实现设置页、命令面板扩展。

---

## 2. 文件清单

```text
feature/connections/
├── ConnectionsScreen.kt
├── ConnectionsState.kt           # 搜索/环境筛选/视图/排序/最近连接
├── ConnectionsHeader.kt
├── ConnectionCard.kt
├── AddConnectionRow.kt
├── RecentConnectionsTable.kt
├── ConnectionSheet.kt
├── sheet/
│   ├── ConnectionBasicTab.kt
│   ├── ConnectionTlsTab.kt
│   ├── ConnectionSshTab.kt
│   └── ConnectionAdvancedTab.kt
├── ConnectionDraft.kt
└── ConnectionCardMenu.kt
```

数据层补充（8B）：

```text
data/jedis/
├── JedisGatewayFactory.kt        # 根据 profile 选择直连 / TLS / SSH 隧道
├── SshTunnel.kt                  # JSch 端口转发
└── TlsContextFactory.kt          # 可选：自定义 CA/客户端证书
```

同步修改：

- `AppState`：`saveConnection` / `deleteConnection` / `duplicateConnection` / `testConnection` / 最近连接时间。
- `ConnectionRepository` 持久化；密码加密。
- `AppShell` 的 CONNECTIONS 分支渲染完整页面。

---

## 3. 连接库页面

### 3.1 布局

- 页面可滚动，最大宽 1160dp，内边距 26–30dp。
- 页头：
  - 眉标 `CONNECTION LIBRARY`；
  - 标题“连接库”+ mint 徽标 “N 在线”；
  - 描述；
  - 右侧：“导入配置”（v1 不实现，隐藏或禁用并 tooltip“暂不支持”）、“新建连接”主按钮。
  - 设计中的“导入配置”可在 v1 省略；不要做假功能。
- 工具栏：
  - 搜索（名称/Host/分组）；
  - 环境 tabs：全部 / Production / Staging / Local / Analytics（Other）；
  - 右侧连接计数 + 网格/列表切换 + 排序（最近使用/名称）。
- 网格：
  - 3 列（< 1160dp 2 列；< 860dp 1 列）；
  - 卡片；新增连接行跨整行（与设计一致）。
- 下方最近连接表格：最近使用时间、连接、环境、状态、操作。
  - 不使用“审计活动”假数据；用 `lastOpenedAtEpochMillis` 真实最近连接。
- 无连接：大空状态 + 新建按钮。

### 3.2 连接卡

- 顶部 2dp 渐变线，颜色 = `ConnectionColor`。
- 36dp 色块缩写（2 个字母）+ 名称 + env badge + host:port Mono。
- 右上状态：
  - 已连接：mint 点 + 延迟（`12 ms`）；
  - 连接中：warning 点 + “连接中”；
  - 错误：danger 点 + “超时/失败”；
  - 未连接：灰点 + “未连接”。
- 标签：TLS / SSH 隧道 / 只读 / Redis 版本 / DB 数 / 预发环境；只显示真实可用的字段。
- 统计行：keys（session summary 或 “—”）· clients · 最近使用。
- 更多菜单：打开工作区、编辑、复制连接、测试连接、删除（danger）。
- 点击卡片：`openConnection` → 切到 Workspace。
- 错误连接也允许点击重试；不要因一个连接失败阻塞页面。

### 3.3 排序与筛选

- 搜索实时过滤（本地）。
- 环境 tabs 过滤 `ConnectionEnvironment`。
- 排序：名称（A→Z，默认，打开连接不会改变顺序）、最近使用（lastOpenedAt 降序）。
- 网格/列表切换只改变布局；列表模式下卡片横向排列，新增行仍跨整行。
- 当前筛选结果为空：空状态“没有匹配的连接”+ 清除筛选。

### 3.4 最近连接

- 表格列：连接（名称 + host）、环境、最后使用、状态/延迟。
- 最多 6 行；没有最近记录时显示“还没有连接记录”。
- 点击行打开连接。

---

## 4. 连接表单（侧滑）

### 4.1 结构

- 触发：新建 / 编辑 / 复制。
- `AppSheet` 宽 470dp，全高；头部：色块 + 标题 + 副标题 + 关闭。
- Tabs：基础 / TLS / SSH 隧道 / 高级。
- Body 可滚动；footer 固定：测试结果文本 + 取消 + 测试连接 + 保存连接。
- 打开时焦点在“连接名称”；Esc / 遮罩关闭。
- 未保存修改关闭时确认（可选，Phase 10）。

### 4.2 基础 tab

- 连接名称*、环境、Host*、Port*、用户名、密码、默认 DB、Key 分隔符、颜色标签、分组、只读模式。
- 颜色：6 个圆点（IRIS/MINT/AMBER/SKY/ROSE/LILAC），选中带白边。
- 表单校验：字段下方红字；顶部汇总错误。
- 密码字段可显示/隐藏（眼睛图标）。
- 默认 DB 输入 0..255。

### 4.3 TLS tab

- 启用 TLS 开关。
- CA 路径、客户端证书、客户端私钥、校验模式（严格 / 允许自签名 / 不校验）。
- 路径支持 `~`；文件选择器可选（桌面可加“浏览”按钮，使用 JFileChooser）。
- 非严格校验时显示 amber 警告文案。
- v1 若未实现自定义证书加载，不能假装成功：显示“当前版本仅支持系统信任链”或抛错；优先实现加载。

### 4.4 SSH tab

- 启用 SSH 隧道开关。
- SSH Host/Port、用户名、认证方式（私钥/密码）、私钥路径/密码。
- 当启用但字段缺失时阻止保存。
- 说明文案：连接会先建立 SSH 隧道再访问 Redis。

### 4.5 高级 tab

- 连接超时、读取超时、Key 分隔符、客户端名称（如果 gateway 支持 `CLIENT SETNAME`，可选）、Sentinel/Cluster（暂不实现，禁用并说明）。
- v1 不支持 Sentinel / Cluster；不要放一个假开关。可以显示“即将支持”的禁用行。

### 4.6 测试连接

- 按钮 loading → 用 draft 构造临时 `ConnectionProfile`（临时 id）→ `gatewayFactory.create(profile)` → `ping()` → `close()`。
- 成功：“✓ 连接成功 · 12 ms · Redis 7.4.2”（如果 ping 无法获取版本，则不写版本）。
- 失败：红色错误首行；不关闭 sheet。
- 测试使用当前未保存的表单值；测试后必须关闭临时 gateway/SSH 隧道。

### 4.7 保存

- 校验通过后：
  - 新建：生成 id、createdAt/updatedAt；保存并更新 AppState；
  - 编辑：保留 id/createdAt，更新 updatedAt；若已有 session，断开旧连接（或提示重连）；
  - 复制：name 加“副本”，新 id，清空 lastOpened。
- 成功后关闭 sheet、toast；编辑后刷新卡片。
- 失败：toast，保持 sheet 打开。

### 4.8 删除 / 复制

- 删除：确认对话框；关闭 session、相关 tabs；从列表移除；如果是 activeConnection，选择剩余第一个或 null。
- 复制：在卡片菜单触发，打开 sheet 的复制草稿。

---

## 5. SSH / TLS 运行时（8B）

### 5.1 SSH 隧道

- 添加依赖（需在提交说明中记录）：`com.github.mwiede:jsch`（选当前稳定 0.2.x）。
- `SshTunnel`：
  - 建立 JSch session；
  - 使用 `setPortForwardingL(0, targetHost, targetPort)` 获取本地端口；
  - 返回 `localPort`；Jedis 连接 `127.0.0.1:localPort`；
  - `close()` 断开转发与 session；
  - 连接失败给出可读错误（认证失败、主机不可达、私钥格式）。
- SSH 密码/私钥路径来自已解密的 `ConnectionProfile`；不打印敏感信息。
- 主机密钥校验：v1 可先用 `StrictHostKeyChecking=no` 并在 UI 标注“v1 不校验 known_hosts”；Phase 10 可改进。**不能**在无提示下静默忽略安全设置。
- 测试连接与正式连接共用同一隧道工厂；确保关闭时释放本地端口。

### 5.2 TLS

- 系统信任链：`SSLContext.getDefault()` 或 `SSLContext.getInstance("TLS")`。
- 自定义 CA / 客户端证书：
  - 解析 PEM → KeyStore → TrustManager/KeyManager → SSLContext；
  - 失败给出明确错误，不降级到明文。
- 校验模式为 NONE 时仅用于开发环境，UI 必须显示危险提示。

### 5.3 边界

- Sentinel / Cluster 不在本阶段。
- SSH agent 认证不在本阶段（仅私钥文件/密码）。
- Windows 路径与 `~` 展开要处理。

---

## 6. 非目标

- 不实现导入/导出配置。
- 不实现 Sentinel / Cluster / SSH agent。
- 不实现连接分组管理对话框（分组字段可输入）。
- 不实现活动审计日志（用最近连接替代）。

---

## 7. 验收

```bash
./gradlew :app:jvmTest
./gradlew ktlintCheck
./gradlew build
./gradlew :desktopApp:run
```

- [ ] 连接库在 1440×900 下 3 列，窄窗降为 2/1 列；新增行跨整行。
- [ ] 搜索、环境筛选、排序、网格/列表切换均工作。
- [ ] 新建连接 → 测试 → 保存 → 卡片出现 → 点击进入工作区并连接。
- [ ] 编辑连接后数据持久化；重启应用仍在。
- [ ] 删除连接会关闭 session 与 tabs；activeConnection 回退正确。
- [ ] 复制连接生成新 id，名称带副本。
- [ ] 测试连接失败时 sheet 不关闭，错误可读。
- [ ] TLS 开关行为真实；不支持的自定义证书场景有明确错误。
- [ ] SSH 隧道真实可用（本地 SSH 环境）；关闭后端口释放。
- [ ] 最近连接表来自 lastOpenedAt；无记录时为空状态。
- [ ] 连接卡状态、延迟、版本、Key 数与真实状态一致。
- [ ] 密码不出现在日志/UI 可见处；保存文件为加密值。
- [ ] 更新 `STATUS.md`（记录 SSH 主机密钥校验限制）。

---

## 8. STOP

停止。Phase 9 实现设置页、主题/强调色持久化与命令面板扩展。
