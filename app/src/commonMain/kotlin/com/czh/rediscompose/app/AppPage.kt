package com.czh.rediscompose.app

enum class AppPage { WORKSPACE, CONNECTIONS, SETTINGS }

enum class WorkspaceTabKind { KEYS, CONSOLE, INSIGHTS }

data class WorkspaceTab(
    val id: String,
    val connectionId: String,
    val kind: WorkspaceTabKind,
    val title: String,
) {
    companion object {
        fun idFor(
            connectionId: String,
            kind: WorkspaceTabKind,
        ): String = "$connectionId:${kind.name.lowercase()}"
    }
}
