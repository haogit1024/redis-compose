package com.czh.rediscompose.data.storage

import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

internal val storageJson: Json =
    Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

/**
 * Writes a temporary file and moves it over the target so a crash never leaves a
 * half-written settings or connections file.
 */
internal fun writeAtomically(
    target: Path,
    content: String,
) {
    val parent = target.parent
    if (parent != null) Files.createDirectories(parent)
    val temporary = target.resolveSibling("${target.fileName}.tmp")
    Files.writeString(temporary, content, StandardCharsets.UTF_8)
    try {
        Files.move(
            temporary,
            target,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    } catch (_: AtomicMoveNotSupportedException) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
    }
}
