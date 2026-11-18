package com.czh.rediscompose.data.redis

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Byte/text conversion for Redis values.
 *
 * Text is only used when the payload is valid UTF-8; otherwise callers show [hex] so no
 * replacement characters ever reach the UI.
 */
object ValueCodec {
    private val prettyJson = Json { prettyPrint = true }

    fun decode(bytes: ByteArray): String? =
        try {
            bytes.decodeToString(throwOnInvalidSequence = true)
        } catch (_: CharacterCodingException) {
            null
        }

    fun hex(bytes: ByteArray): String =
        bytes.joinToString(separator = " ") { byte ->
            (byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
        }

    fun isJson(text: String): Boolean {
        val trimmed = text.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return false
        return runCatching { Json.parseToJsonElement(trimmed) }.isSuccess
    }

    fun prettyJson(text: String): String? {
        val element = runCatching { Json.parseToJsonElement(text.trim()) }.getOrNull() ?: return null
        return runCatching {
            prettyJson.encodeToString(JsonElement.serializer(), element)
        }.getOrNull()
    }

    fun hexToBytes(hex: String): ByteArray? {
        val compact = hex.filterNot { it.isWhitespace() }
        if (compact.isEmpty()) return ByteArray(0)
        val normalized = compact.removePrefix("0x").removePrefix("0X")
        if (normalized.isEmpty()) return null
        if (normalized.length % 2 != 0) return null
        val result = ByteArray(normalized.length / 2)
        for (index in result.indices) {
            val high = normalized[index * 2].digitToIntOrNull(16) ?: return null
            val low = normalized[index * 2 + 1].digitToIntOrNull(16) ?: return null
            result[index] = ((high shl 4) or low).toByte()
        }
        return result
    }
}
