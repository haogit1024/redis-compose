package com.czh.redis.redis

fun parseInfoSections(info: String): List<Pair<String, List<Pair<String, String>>>> {
    val sections = mutableListOf<Pair<String, List<Pair<String, String>>>>()
    var currentSection = ""
    val currentLines = mutableListOf<Pair<String, String>>()
    for (line in info.lines()) {
        when {
            line.startsWith("#") -> {
                if (currentLines.isNotEmpty()) {
                    sections.add(currentSection to currentLines.toList())
                }
                currentLines.clear()
                currentSection = line.removePrefix("#").trim()
            }
            line.isNotBlank() && !line.startsWith(" ") -> {
                val idx = line.indexOf(':')
                if (idx > 0) currentLines.add(line.substring(0, idx) to line.substring(idx + 1))
            }
        }
    }
    if (currentLines.isNotEmpty()) sections.add(currentSection to currentLines.toList())
    return sections
}
