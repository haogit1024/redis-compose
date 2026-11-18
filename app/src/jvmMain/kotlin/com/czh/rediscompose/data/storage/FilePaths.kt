package com.czh.rediscompose.data.storage

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Files under `~/.redis-compose/`.
 *
 * The base directory is injectable so repository tests can use a temporary folder.
 */
class FilePaths(val baseDir: Path = defaultBaseDir()) {
    val settings: Path get() = baseDir.resolve("settings.json")
    val connections: Path get() = baseDir.resolve("connections.json")
    val secretKey: Path get() = baseDir.resolve("secret.key")

    companion object {
        fun defaultBaseDir(): Path = Paths.get(System.getProperty("user.home"), ".redis-compose")
    }
}

/** Expands a leading `~` and returns the path as-is otherwise. */
fun expandUserPath(path: String): Path {
    val trimmed = path.trim()
    return when {
        trimmed == "~" -> Paths.get(System.getProperty("user.home"))
        trimmed.startsWith("~/") || trimmed.startsWith("~\\") ->
            Paths.get(System.getProperty("user.home"), trimmed.substring(2))
        else -> Paths.get(trimmed)
    }
}
