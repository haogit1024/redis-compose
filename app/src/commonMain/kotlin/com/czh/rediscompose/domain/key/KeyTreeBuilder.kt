package com.czh.rediscompose.domain.key

/**
 * Builds the key tree used by the workspace pane.
 *
 * Directories sort before keys, both by case-insensitive name so refreshes keep a stable
 * order. Keys are de-duplicated and blank keys are ignored.
 */
object KeyTreeBuilder {
    fun build(
        keys: List<String>,
        separator: String,
    ): List<KeyNode> {
        val distinctKeys = keys.filter { it.isNotEmpty() }.distinct()
        if (distinctKeys.isEmpty()) return emptyList()

        if (separator.isEmpty()) {
            return distinctKeys
                .sortedWith(NAME_ORDER)
                .map { key ->
                    KeyNode(
                        name = key,
                        path = key,
                        isDirectory = false,
                        count = 1,
                        key = KeySummary(name = key, type = KeyType.NONE),
                        children = emptyList(),
                    )
                }
        }

        val root = Directory(name = "", path = "")
        distinctKeys.forEach { key ->
            val segments = key.split(separator).filter { it.isNotEmpty() }
            if (segments.isEmpty()) {
                root.leaves.add(Leaf(name = key, path = key))
            } else {
                root.insert(segments, fullPath = key, separator = separator, index = 0)
            }
        }
        return root.toNodes()
    }

    private fun Directory.insert(
        segments: List<String>,
        fullPath: String,
        separator: String,
        index: Int,
    ) {
        val segment = segments[index]
        if (index == segments.lastIndex) {
            leaves.add(Leaf(name = segment, path = fullPath))
            return
        }
        val childPath = if (path.isEmpty()) segment else "$path$separator$segment"
        val child = directories.getOrPut(segment) { Directory(name = segment, path = childPath) }
        child.insert(segments, fullPath, separator, index + 1)
    }

    private fun Directory.toNodes(): List<KeyNode> {
        val directoryNodes =
            directories.values
                .sortedWith(compareBy({ it.name.lowercase() }, { it.name }))
                .map { directory ->
                    KeyNode(
                        name = directory.name,
                        path = directory.path,
                        isDirectory = true,
                        count = directory.totalCount(),
                        key = null,
                        children = directory.toNodes(),
                    )
                }
        val leafNodes =
            leaves
                .sortedWith(compareBy({ it.name.lowercase() }, { it.name }))
                .map { leaf ->
                    KeyNode(
                        name = leaf.name,
                        path = leaf.path,
                        isDirectory = false,
                        count = 1,
                        key = KeySummary(name = leaf.path, type = KeyType.NONE),
                        children = emptyList(),
                    )
                }
        return directoryNodes + leafNodes
    }

    private fun Directory.totalCount(): Int = leaves.size + directories.values.sumOf { it.totalCount() }

    private val NAME_ORDER = compareBy<String>({ it.lowercase() }, { it })

    private class Directory(val name: String, val path: String) {
        val directories = linkedMapOf<String, Directory>()
        val leaves = mutableListOf<Leaf>()
    }

    private data class Leaf(val name: String, val path: String)
}
