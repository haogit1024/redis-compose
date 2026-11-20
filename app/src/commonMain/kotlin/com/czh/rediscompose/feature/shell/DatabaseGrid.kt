package com.czh.rediscompose.feature.shell

import com.czh.rediscompose.domain.key.DatabaseInfo

/**
 * Splits the sidebar database list into column-major chunks so a 16-database server renders
 * db0..db7 down the left column and db8..db15 down the right, keeping all of them visible.
 */
internal fun databaseColumns(
    databases: List<DatabaseInfo>,
    columnCount: Int = 2,
): List<List<DatabaseInfo>> {
    if (databases.isEmpty() || columnCount <= 1) return listOf(databases)
    val perColumn = (databases.size + columnCount - 1) / columnCount
    return databases.chunked(perColumn)
}
