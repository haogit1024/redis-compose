# redis-compose AGENTS.md

## 项目概述

本项目是一个基于 **Kotlin Multiplatform Compose** 的桌面应用开发框架，旨在通过共享代码实现跨平台 UI 开发。

### 核心目标

- 利用 Kotlin Multiplatform 特性实现代码复用
- 使用 Compose Multiplatform 构建现代化桌面 UI
- 支持热重载开发，提升开发效率
- 为未来扩展到 iOS/Android/Web 平台奠定基础

### 技术栈

| 分类 | 技术 | 版本             |
|------|------|----------------|
| 语言 | Kotlin | 2.4.0          |
| UI 框架 | Compose Multiplatform | 1.11.1         |
| 设计系统 | Material3 | 1.11.0-alpha07 |
| 异步 | Kotlinx Coroutines | 1.11.0         |
| 生命周期 | AndroidX Lifecycle | 2.11.0-beta01  |
| 构建工具 | Gradle (Kotlin DSL) | 9.6.0          |
| 目标平台 | Desktop (JVM) | -              |

---

## 模块结构

```
redis-compose/
├── desktopApp/           # 桌面应用入口模块
│   └── src/main/kotlin/com/czh/redis/
│       └── main.kt       # 桌面应用主函数
├── shared/               # 共享代码模块
│   └── src/
│       ├── commonMain/   # 跨平台共享代码
│       │   ├── kotlin/   # Kotlin 源代码
│       │   └── composeResources/  # 资源文件
│       ├── commonTest/   # 跨平台测试代码
│       ├── jvmMain/      # JVM 平台特定代码
│       └── jvmTest/      # JVM 平台测试代码
├── gradle/               # Gradle 配置
│   ├── wrapper/          # Gradle Wrapper
│   └── libs.versions.toml # 依赖版本目录
└── settings.gradle.kts   # 项目模块配置
```

### 模块职责

| 模块 | 职责 | 说明 |
|------|------|------|
| `desktopApp` | 桌面应用入口 | 包含主函数、桌面特定配置、打包配置 |
| `shared` | 共享 UI 和逻辑 | 包含 Compose 组件、业务逻辑、跨平台资源 |

---

## 开发指南

### 构建命令

```bash
# 编译项目
./gradlew build

# 清理构建
./gradlew clean
```

### 运行方式

```bash
# 热重载运行（推荐开发使用）
./gradlew :desktopApp:hotRun --auto

# 标准运行
./gradlew :desktopApp:run
```

### 测试命令

```bash
# 运行所有测试
./gradlew test

# 仅运行 JVM 测试
./gradlew :shared:jvmTest
```

### IDE 配置

推荐使用 **IntelliJ IDEA** 进行开发，支持以下功能：
- Kotlin Multiplatform 插件
- Compose Preview 支持
- Gradle 集成

---

## 依赖管理

项目使用 Gradle Version Catalog 管理依赖，配置文件为 [gradle/libs.versions.toml](file:///Users/chenzhihao/git_clones/my/redis-compose/gradle/libs.versions.toml)。

### 主要依赖

| 依赖 | 坐标 | 用途 |
|------|------|------|
| Compose Runtime | `org.jetbrains.compose.runtime:runtime` | Compose 运行时 |
| Compose Foundation | `org.jetbrains.compose.foundation:foundation` | 基础 UI 组件 |
| Compose Material3 | `org.jetbrains.compose.material3:material3` | Material Design 3 组件 |
| Compose UI | `org.jetbrains.compose.ui:ui` | UI 核心功能 |
| Compose Resources | `org.jetbrains.compose.components:components-resources` | 资源管理 |
| Compose UI Tooling Preview | `org.jetbrains.compose.ui:ui-tooling-preview` | 预览支持 |
| AndroidX Lifecycle ViewModel Compose | `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` | ViewModel 集成 |
| AndroidX Lifecycle Runtime Compose | `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose` | 生命周期感知 |
| Kotlinx Coroutines Swing | `org.jetbrains.kotlinx:kotlinx-coroutines-swing` | Swing 协程调度 |

### 插件

| 插件 | ID | 用途 |
|------|-----|------|
| Compose Multiplatform | `org.jetbrains.compose` | Compose 多平台支持 |
| Compose Compiler | `org.jetbrains.kotlin.plugin.compose` | Compose 编译器插件 |
| Kotlin JVM | `org.jetbrains.kotlin.jvm` | JVM 目标编译 |
| Kotlin Multiplatform | `org.jetbrains.kotlin.multiplatform` | KMP 支持 |

---

## 编码约定

### Kotlin 代码规范

- 遵循 [Kotlin 官方编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- 使用 4 空格缩进
- 文件编码为 UTF-8
- 类名使用 PascalCase
- 函数名和变量名使用 camelCase
- 常量使用 UPPER_CASE_WITH_UNDERSCORES

### Compose 组件编写规范

```kotlin
@Composable
fun MyComponent(
    param1: String,
    param2: Int,
    modifier: Modifier = Modifier,
) {
    // 组件实现
}

@Composable
@Preview
fun MyComponentPreview() {
    MaterialTheme {
        MyComponent(
            param1 = "示例",
            param2 = 0,
        )
    }
}
```

- 组件函数使用 `@Composable` 注解
- `modifier` 参数作为最后一个参数，默认值为 `Modifier`
- 为每个组件提供 `@Preview` 预览函数
- 使用 `MaterialTheme` 包裹预览内容

### 文件命名规则

- Composable 组件：`PascalCase.kt`（如 `Greeting.kt`）
- ViewModel：`PascalCaseViewModel.kt`
- 工具类：`PascalCaseUtil.kt`

### 资源管理

- 资源文件放在 `shared/src/commonMain/composeResources/`
- 使用 `Res` 对象访问资源（如 `Res.drawable.compose_multiplatform`）

---

## 扩展建议

### 平台扩展

当前项目仅支持 Desktop (JVM)，未来可扩展以下平台：

| 平台 | 目录 | 说明 |
|------|------|------|
| Android | `shared/src/androidMain/` | Android 平台特定代码 |
| iOS | `shared/src/iosMain/` | iOS 平台特定代码 |
| Web | `shared/src/jsMain/` | Web 平台特定代码 |

### 架构演进

建议采用以下架构模式：

1. **分层架构**：UI层 → 业务逻辑层 → 数据层
2. **状态管理**：使用 `StateFlow` + `collectAsStateWithLifecycle`
3. **依赖注入**：考虑使用 Koin 或 Hilt
4. **导航**：使用 Compose Navigation 组件

---

## 参考资源

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform Documentation](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
