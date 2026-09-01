package com.czh.redis.redis

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.ExperimentalStdlibApi

object ValueCodec {
    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }

    @OptIn(ExperimentalStdlibApi::class)
    fun decode(bytes: ByteArray): String? =
        runCatching {
            bytes.decodeToString(throwOnInvalidSequence = true)
        }.getOrNull()

    fun hex(bytes: ByteArray): String = bytes.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

    fun isJson(text: String): Boolean {
        val trimmed = text.trim()
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) return false
        return runCatching { Json.parseToJsonElement(trimmed) }.isSuccess
    }

    fun prettyJson(text: String): String {
        val element = Json.parseToJsonElement(text)
        return json.encodeToString(JsonElement.serializer(), element)
    }

    fun isBinary(bytes: ByteArray): Boolean = decode(bytes) == null

    fun format(
        bytes: ByteArray,
        mode: String,
    ): String {
        val text = decode(bytes)
        return when (mode) {
            "HEX" -> hex(bytes)
            "JSON" -> {
                if (text != null && isJson(text)) prettyJson(text) else hex(bytes)
            }
            "TEXT" -> text ?: hex(bytes)
            else -> {
                if (text == null) return hex(bytes)
                if (isJson(text)) prettyJson(text) else text
            }
        }
    }
}
