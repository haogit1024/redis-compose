package com.czh.rediscompose.feature.shell

import com.czh.rediscompose.domain.connection.ConnectionEnvironment
import com.czh.rediscompose.domain.connection.ConnectionProfile

/**
 * Sidebar groups in [ConnectionEnvironment] order, each sorted by name.
 *
 * The order deliberately ignores [ConnectionProfile.lastOpenedAtEpochMillis]: opening a
 * connection (or switching tabs) must not move its row, so rows stay where the user left them.
 * The "最近使用" ordering remains available on the connections page, where it is an explicit sort.
 */
internal fun sidebarConnectionGroups(
    connections: List<ConnectionProfile>,
    filter: String,
): List<Pair<ConnectionEnvironment, List<ConnectionProfile>>> {
    val visible =
        if (filter.isBlank()) {
            connections
        } else {
            connections.filter {
                it.name.contains(filter, ignoreCase = true) || it.host.contains(filter, ignoreCase = true)
            }
        }
    return ConnectionEnvironment.entries.mapNotNull { environment ->
        val entries =
            visible
                .filter { it.environment == environment }
                .sortedBy { it.name.lowercase() }
        if (entries.isEmpty()) null else environment to entries
    }
}
