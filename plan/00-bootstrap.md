# Phase 0 — 重置脚手架

> **目标**：把工程从旧结构切换到新命名、新包结构的空壳应用。此阶段允许破坏旧功能；不要保留旧 UI。
>
> 依赖：无  
> 下一阶段：Phase 1（设计系统）

---

## 1. 开始前

- 确认用户已同意“完全抛弃旧代码”。
- 先建分支（如 `feat/rewrite-ui`），不要直接在 `main` 上做破坏性提交。
- 阅读 `plan/README.md` 的架构决策与目标目录。
- **不要**开始实现主题、组件或页面；本阶段只做结构重置与可启动空壳。

---

## 2. 任务

### 2.1 重命名模块

1. 将 Gradle 模块目录 `shared/` 重命名为 `app/`：
   ```bash
   git mv shared app
   ```
2. 修改 `settings.gradle.kts`：
   - `include(":shared")` → `include(":app")`
3. 修改 `desktopApp/build.gradle.kts`：
   - `implementation(projects.shared)` → `implementation(projects.app)`
   - `mainClass = "com.czh.rediscompose.desktop.MainKt"`
   - `packageName = "com.czh.rediscompose"`
4. 修改 `app/build.gradle.kts`：
   - 模块名不写在 build 文件里；保持现有插件。
   - 暂不改依赖；字体/图标依赖在 Phase 1 添加。
5. 搜索并清理 `shared` 的引用（`settings.gradle.kts`、README、AGENTS、IDE 配置不必强行修改 IDE 配置）。

### 2.2 包结构重置

1. 删除旧包目录（保留 `composeResources` 目录并在 Phase 1 重建）：
   ```text
   app/src/commonMain/kotlin/com/czh/redis/            ← 删除整个目录
   app/src/jvmMain/kotlin/com/czh/redis/               ← 删除整个目录
   app/src/commonTest/kotlin/com/czh/redis/            ← 删除
   app/src/jvmTest/kotlin/com/czh/redis/               ← 删除
   ```
2. 创建新包：
   ```text
   app/src/commonMain/kotlin/com/czh/rediscompose/app/
   app/src/commonMain/kotlin/com/czh/rediscompose/design/
   app/src/commonMain/kotlin/com/czh/rediscompose/domain/
   app/src/commonMain/kotlin/com/czh/rediscompose/data/
   app/src/commonMain/kotlin/com/czh/rediscompose/feature/
   app/src/commonMain/kotlin/com/czh/rediscompose/util/
   app/src/jvmMain/kotlin/com/czh/rediscompose/
   app/src/commonTest/kotlin/com/czh/rediscompose/
   app/src/jvmTest/kotlin/com/czh/rediscompose/
   ```
   空目录不会被 git 记录，后续阶段按需创建文件；可以放 `.gitkeep`，但不要留下无意义占位类。
3. 删除旧 `composeResources/drawable/compose-multiplatform.xml`（Phase 1 会创建字体/品牌资源）。
4. 删除旧 `shared/src/.../Platform.kt`、`Platform.jvm.kt` 与所有 expect/actual（Phase 2 用 container 注入替代）。

### 2.3 新入口与空壳

创建 `app/src/commonMain/kotlin/com/czh/rediscompose/app/RedisComposeApp.kt`：

```kotlin
package com.czh.rediscompose.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RedisComposeApp() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF07090D)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("R", color = Color(0xFF918DFF), fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Redis Compose", color = Color(0xFFEEF1F8), fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text("UI rewrite in progress", color = Color(0xFF6C7890), fontSize = 12.sp)
        }
    }
}
```

> 这是唯一允许硬编码颜色的地方；Phase 1 会用 `AppTheme` 替换。

创建 `desktopApp/src/main/kotlin/com/czh/rediscompose/desktop/Main.kt`：

```kotlin
package com.czh.rediscompose.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.czh.rediscompose.app.RedisComposeApp
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Redis Compose",
        state = WindowState(size = DpSize(1440.dp, 900.dp)),
    ) {
        window.minimumSize = Dimension(1120, 720)
        RedisComposeApp()
    }
}
```

### 2.4 清理构建与文档

- 更新 `AGENTS.md`：
  - 模块结构 `shared` → `app`；
  - 包名/入口路径；
  - 测试命令 `./gradlew :app:jvmTest`。
  其余编码约定可在 Phase 10 再精修。
- 更新根 `README.md` 中的模块路径（如果有）。
- 确认 `.gitignore` 仍忽略 `build/`、`.gradle/`。
- 删除旧测试（已随目录删除）；暂时不加新测试。

---

## 3. 非目标

- 不实现主题、字体、图标、组件。
- 不实现 Redis 连接、数据模型、持久化。
- 不实现任何页面布局。
- 不删除 `design/`、`plan/`。
- 不升级 Gradle/Kotlin/Compose 版本。

---

## 4. 验收

```bash
./gradlew build
./gradlew ktlintCheck
./gradlew :desktopApp:run
```

- [ ] 工程只有一个应用模块 `app` + 入口模块 `desktopApp`。
- [ ] `settings.gradle.kts` 不再出现 `:shared`。
- [ ] `rg "com\.czh\.redis\b" app desktopApp` 无结果（旧包是 `com.czh.redis`，新包 `com.czh.rediscompose` 不会被这个边界匹配到）。
- [ ] 应用以 1440×900 打开，最小尺寸 1120×720；标题为 “Redis Compose”。
- [ ] 窗口内容为深色空壳，无旧 UI 残留。
- [ ] `build/` 中没有旧的 shared 产物被提交。
- [ ] `plan/STATUS.md` 更新 Phase 0 为“已验收”。

---

## 5. STOP

完成验收后停止。Phase 1 需要基于本阶段的新包结构开始设计系统。
