package com.czh.rediscompose.feature.shell

import com.czh.rediscompose.domain.connection.ConnectionEnvironment
import com.czh.rediscompose.domain.connection.ConnectionProfile
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectionOrderingTest {
    private fun profile(
        id: String,
        name: String,
        environment: ConnectionEnvironment = ConnectionEnvironment.LOCAL,
        host: String = "127.0.0.1",
        lastOpened: Long? = null,
    ) = ConnectionProfile(
        id = id,
        name = name,
        environment = environment,
        host = host,
        lastOpenedAtEpochMillis = lastOpened,
    )

    @Test
    fun `recently opened connections keep their place`() {
        val connections =
            listOf(
                profile("a", "Alpha", lastOpened = 1_000L),
                profile("b", "Bravo", lastOpened = 3_000L),
                profile("c", "Charlie", lastOpened = 2_000L),
            )

        val before = sidebarConnectionGroups(connections, filter = "").flatMap { it.second }.map { it.id }
        val after =
            sidebarConnectionGroups(
                connections.map {
                    if (it.id == "a") it.copy(lastOpenedAtEpochMillis = 9_000L) else it
                },
                filter = "",
            ).flatMap { it.second }.map { it.id }

        assertEquals(listOf("a", "b", "c"), before)
        assertEquals(before, after)
    }

    @Test
    fun `groups follow environment order and skip empty environments`() {
        val connections =
            listOf(
                profile("local", "Local", ConnectionEnvironment.LOCAL),
                profile("prod-b", "Beta", ConnectionEnvironment.PRODUCTION),
                profile("prod-a", "Alpha", ConnectionEnvironment.PRODUCTION),
            )

        val groups = sidebarConnectionGroups(connections, filter = "")

        assertEquals(listOf(ConnectionEnvironment.PRODUCTION, ConnectionEnvironment.LOCAL), groups.map { it.first })
        assertEquals(listOf("prod-a", "prod-b"), groups.first().second.map { it.id })
    }

    @Test
    fun `filter matches name or host case-insensitively`() {
        val connections =
            listOf(
                profile("a", "Prod Cache", host = "redis-01.acme.io"),
                profile("b", "Local Sandbox", host = "127.0.0.1"),
            )

        assertEquals(
            listOf("a"),
            sidebarConnectionGroups(connections, filter = "cache").flatMap { it.second }.map { it.id },
        )
        assertEquals(
            listOf("b"),
            sidebarConnectionGroups(connections, filter = "127.0").flatMap { it.second }.map { it.id },
        )
        assertEquals(2, sidebarConnectionGroups(connections, filter = " ").sumOf { it.second.size })
    }
}
