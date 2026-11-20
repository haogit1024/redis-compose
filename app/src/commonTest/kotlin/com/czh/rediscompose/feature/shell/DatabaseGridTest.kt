package com.czh.rediscompose.feature.shell

import com.czh.rediscompose.domain.key.DatabaseInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseGridTest {
    private fun databases(count: Int): List<DatabaseInfo> = (0 until count).map { DatabaseInfo(index = it, keyCount = it.toLong()) }

    @Test
    fun `sixteen databases split into two columns of eight`() {
        val columns = databaseColumns(databases(16))

        assertEquals(2, columns.size)
        assertEquals((0..7).toList(), columns[0].map { it.index })
        assertEquals((8..15).toList(), columns[1].map { it.index })
    }

    @Test
    fun `odd database count fills the left column first`() {
        val columns = databaseColumns(databases(15))

        assertEquals((0..7).toList(), columns[0].map { it.index })
        assertEquals((8..14).toList(), columns[1].map { it.index })
    }

    @Test
    fun `empty list stays a single empty column`() {
        assertEquals(listOf(emptyList()), databaseColumns(emptyList()))
    }
}
