package com.czh.rediscompose.domain.key

/**
 * Redis value types as presented by the UI.
 */
enum class KeyType {
    STRING,
    HASH,
    LIST,
    SET,
    ZSET,
    STREAM,
    NONE,
}

data class ScanPage(
    val cursor: Long,
    val keys: List<String>,
)

data class DatabaseInfo(
    val index: Int,
    val keyCount: Long,
)

data class KeySummary(
    val name: String,
    val type: KeyType,
    val ttlSeconds: Long = -1,
)

data class KeyMetadata(
    val ttlSeconds: Long = -1,
    val memoryBytes: Long? = null,
    val encoding: String? = null,
    val elementCount: Long? = null,
)

data class HashField(val field: String, val value: String)

data class SortedSetEntry(val member: String, val score: Double)

data class StreamEntry(
    val id: String,
    val fields: List<Pair<String, String>>,
)

/**
 * Decoded value of a selected key. Binary-safe decoding happens in [KeyDetail]'s source
 * gateway; this model only carries displayable strings.
 */
sealed interface KeyValue {
    data class StringValue(val text: String, val isJson: Boolean) : KeyValue

    data class HashValue(val fields: List<HashField>) : KeyValue

    data class ListValue(val items: List<String>) : KeyValue

    data class SetValue(val members: List<String>) : KeyValue

    data class SortedSetValue(val entries: List<SortedSetEntry>) : KeyValue

    data class StreamValue(val entries: List<StreamEntry>, val groupCount: Int?) : KeyValue
}

data class KeyDetail(
    val summary: KeySummary,
    val metadata: KeyMetadata,
    val value: KeyValue,
)
