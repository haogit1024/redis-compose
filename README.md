# redis-compose

A Kotlin Multiplatform / Compose Desktop workspace for Redis, built with a custom design system.

这是 Redis Compose 的重写版本。项目按分阶段计划推进，Phase 0（结构重置）、Phase 1（设计系统）与 Phase 2（领域与数据层）已完成；当前应用启动到临时组件画廊，下一步是 Phase 3（AppState 与外壳）。

## 模块

| 模块 | 说明 |
| --- | --- |
| `desktopApp` | 桌面应用入口（`com.czh.rediscompose.desktop.MainKt`） |
| `app` | 共享 UI 与逻辑（包名 `com.czh.rediscompose`） |

`design/` 是 HTML 设计参考，`plan/` 是分阶段实施计划与验收记录（`plan/STATUS.md`）。两者都不参与打包。

## 命令

```bash
./gradlew build
./gradlew ktlintCheck
./gradlew :app:jvmTest
./gradlew :desktopApp:run
./gradlew :desktopApp:hotRun --auto
```

## 参考

- 计划入口：`plan/README.md`
- 设计总览：`design/index.html`
