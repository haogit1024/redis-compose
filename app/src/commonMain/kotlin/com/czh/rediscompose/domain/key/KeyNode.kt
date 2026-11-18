package com.czh.rediscompose.domain.key

data class KeyNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val count: Int,
    val key: KeySummary?,
    val children: List<KeyNode>,
)
