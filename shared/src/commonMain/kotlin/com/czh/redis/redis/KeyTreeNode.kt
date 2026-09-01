package com.czh.redis.redis

data class KeyTreeNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val key: String? = null,
    val children: MutableList<KeyTreeNode> = mutableListOf(),
) {
    val count: Int
        get() = if (isDirectory) children.sumOf { it.count } else 1

    val leafKeys: List<String>
        get() = if (key != null) listOf(key) else children.flatMap { it.leafKeys }
}

fun buildKeyTree(
    keys: List<String>,
    separator: String,
): List<KeyTreeNode> {
    if (separator.isEmpty()) {
        return keys.distinct().map { KeyTreeNode(name = it, path = it, isDirectory = false, key = it) }
    }
    val roots = mutableListOf<KeyTreeNode>()

    fun getOrCreateDir(
        chain: List<KeyTreeNode>,
        name: String,
        path: String,
    ): KeyTreeNode {
        val container = chain.lastOrNull()?.children ?: roots
        return container.firstOrNull { it.name == name && it.isDirectory }
            ?: KeyTreeNode(name = name, path = path, isDirectory = true).also { container.add(it) }
    }

    for (key in keys) {
        val parts = key.split(separator)
        val pathParts = mutableListOf<String>()
        val chain = mutableListOf<KeyTreeNode>()
        for ((index, part) in parts.withIndex()) {
            pathParts.add(part)
            val path = pathParts.joinToString(separator)
            if (index == parts.lastIndex) {
                val leaf = KeyTreeNode(name = part, path = key, isDirectory = false, key = key)
                if (chain.isEmpty()) {
                    if (roots.none { it.key == key }) roots.add(leaf)
                } else {
                    if (chain.last().children.none { it.key == key }) chain.last().children.add(leaf)
                }
            } else {
                chain.add(getOrCreateDir(chain, part, path))
            }
        }
    }
    return roots
}
