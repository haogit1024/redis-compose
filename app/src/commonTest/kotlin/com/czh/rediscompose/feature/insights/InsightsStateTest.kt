package com.czh.rediscompose.feature.insights

import com.czh.rediscompose.app.FakeRedisGateway
import com.czh.rediscompose.app.RedisSession
import com.czh.rediscompose.data.redis.RedisException
import com.czh.rediscompose.domain.command.ClientConnection
import com.czh.rediscompose.domain.command.CommandStat
import com.czh.rediscompose.domain.command.SlowLogEntry
import com.czh.rediscompose.domain.connection.ConnectionProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InsightsStateTest {
    private fun state(gateway: FakeRedisGateway): InsightsState {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val session =
            RedisSession(
                profile = ConnectionProfile(id = "c1", name = "Local", host = "127.0.0.1"),
                gateway = gateway,
                appScope = scope,
            )
        scope.launch { session.connect() }
        return InsightsState(session = session, scope = scope)
    }

    @Test
    fun `refresh collects info commandstats slowlog and clients into one sample`() {
        val gateway = FakeRedisGateway()
        gateway.commandStatValues = listOf(CommandStat("get", calls = 100, totalUsec = 1_000, usecPerCall = 10.0))
        gateway.slowLogEntries =
            listOf(
                SlowLogEntry(
                    id = 1,
                    timestampEpochSeconds = 1_700_000_000,
                    durationMicros = 15_000,
                    command = listOf("GET", "cache:1"),
                    clientAddress = "10.0.0.1",
                    clientName = "worker-1",
                ),
            )
        gateway.clientConnections =
            listOf(
                ClientConnection(id = "1", address = "10.0.0.1", name = "worker-1", database = 0, idleSeconds = 2),
            )
        val insights = state(gateway)

        insights.refreshNow()

        assertEquals(1, insights.samples.size)
        assertEquals("7.4.2", insights.snapshot?.version)
        assertEquals(1, insights.commandStats.size)
        assertEquals(1, insights.slowLog.size)
        assertEquals(1, insights.clients.size)
        assertNull(insights.error)
        assertNotNull(insights.lastUpdatedEpochMillis)
        insights.dispose()
    }

    @Test
    fun `start samples immediately and stop ends polling while keeping samples`() {
        val gateway = FakeRedisGateway()
        val insights = state(gateway)

        insights.start()
        assertTrue(insights.isPolling)
        assertEquals(1, insights.samples.size)

        insights.stop()
        assertFalse(insights.isPolling)
        assertEquals(1, insights.samples.size)
        insights.dispose()
    }

    @Test
    fun `manual refresh works while auto refresh is paused`() {
        val gateway = FakeRedisGateway()
        val insights = state(gateway)
        insights.updateAutoRefresh(false)

        insights.start()
        assertEquals(1, insights.samples.size)
        insights.refreshNow()

        assertEquals(2, insights.samples.size)
        assertFalse(insights.autoRefresh)
        insights.dispose()
    }

    @Test
    fun `failed refresh keeps old data and exposes an error without clearing samples`() {
        val gateway = FakeRedisGateway()
        val insights = state(gateway)
        insights.refreshNow()
        val firstSnapshot = insights.snapshot

        gateway.infoError = RedisException("Connection refused")
        insights.refreshNow()

        assertEquals(1, insights.samples.size)
        assertEquals(firstSnapshot, insights.snapshot)
        assertTrue(insights.error.orEmpty().contains("Connection refused"))
        insights.dispose()
    }

    @Test
    fun `command distribution starts sampling then uses deltas between samples`() {
        val gateway = FakeRedisGateway()
        gateway.commandStatValues = listOf(CommandStat("get", calls = 100, totalUsec = 1_000, usecPerCall = 10.0))
        val insights = state(gateway)

        insights.refreshNow()
        val first = insights.dashboardData()
        assertTrue(first.commandsSampling)
        assertTrue(first.commands.isEmpty())
        assertEquals(4, first.kpis.size)

        gateway.commandStatValues = listOf(CommandStat("get", calls = 160, totalUsec = 1_600, usecPerCall = 10.0))
        insights.refreshNow()
        val second = insights.dashboardData()

        assertFalse(second.commandsSampling)
        assertEquals("GET", second.commands.single().name)
        assertEquals(60L, second.commands.single().calls)
        insights.dispose()
    }

    @Test
    fun `range changes only affect derived windows not network calls`() {
        val gateway = FakeRedisGateway()
        val insights = state(gateway)
        insights.refreshNow()
        val calls = gateway.pingCount
        val infoCalls = 1

        insights.selectRange(InsightsRange.LAST_1_HOUR)

        assertEquals(InsightsRange.LAST_1_HOUR, insights.dashboardData().header.range)
        assertEquals(calls, gateway.pingCount)
        assertEquals(infoCalls, insights.samples.size)
        insights.dispose()
    }
}
