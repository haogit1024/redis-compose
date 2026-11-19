import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ktlint)
}

ktlint {
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}

dependencies {
    implementation(projects.app)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.czh.rediscompose.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "RedisCompose"
            // jpackage on macOS rejects versions whose first component is 0, so the bundle
            // version is 1.0.0 while the in-app product version remains 0.1.0.
            packageVersion = "1.0.0"
            description = "Redis Compose — a local-first Redis workspace"
            vendor = "Redis Compose"
            copyright = "© 2026 Redis Compose contributors"

            macOS {
                bundleID = "com.czh.rediscompose"
                dockName = "Redis Compose"
                iconFile.set(project.file("icons/app-icon.icns"))
            }
            windows {
                menuGroup = "Redis Compose"
                iconFile.set(project.file("icons/app-icon.ico"))
            }
            linux {
                menuGroup = "Development"
                appCategory = "Development"
                iconFile.set(project.file("icons/app-icon-linux.png"))
            }
        }
    }
}
