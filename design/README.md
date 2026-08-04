# RedisCompose 设计稿

一个基于 **Kotlin Multiplatform + Compose Desktop** 的 Redis 桌面管理工具设计文档。

> 参考项目：
> - [AnotherRedisDesktopManager](https://github.com/qishibo/AnotherRedisDesktopManager)（功能特性与交互参考）
> - [RedisDesktopManager / RESP.app](https://github.com/redis/RedisDesktopManager)（界面布局参考）

## 文档索引

| 文档 | 说明 |
|------|------|
| [feature-design.md](./feature-design.md) | 功能设计：模块划分、核心功能、分期路线图 |
| [ui-design.md](./ui-design.md) | 界面设计：整体布局、页面结构、组件与主题规范 |
| [development-plan.md](./development-plan.md) | 开发计划：里程碑、任务拆解、验收标准、风险应对 |
| [html/index.html](./html/index.html) | 交互式 HTML 原型预览入口（浏览器直接打开） |

## 设计稿图片

| 图片 | 说明 |
|------|------|
| [assets/main-screen-light.png](./assets/main-screen-light.png) | 主界面（浅色主题）：连接列表 + Key 树 + 值查看器 |
| [assets/main-screen-dark.png](./assets/main-screen-dark.png) | 主界面（深色主题） |
| [assets/new-connection-dialog.png](./assets/new-connection-dialog.png) | 新建连接弹窗 |

## 核心设计理念

1. **海量 Key 不卡顿**：使用 `SCAN` 增量加载，分页渲染 Key 列表，绝不一次性 `KEYS *`。
2. **多标签工作区**：Key 详情、命令行控制台、服务器信息均以 Tab 形式打开，互不干扰。
3. **树形浏览**：按分隔符（默认 `:`）将 Key 组织为目录树，支持折叠/展开。
4. **类型全覆盖**：String / Hash / List / Set / ZSet / Stream（及 RedisJSON），值支持多种格式化展示。
5. **连接灵活**：支持 SSH 隧道、SSL/TLS、Sentinel、Cluster，连接可分组、可打颜色标签。
