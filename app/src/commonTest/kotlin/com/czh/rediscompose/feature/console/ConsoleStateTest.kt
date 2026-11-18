package com.czh.rediscompose.feature.console

import com.czh.rediscompose.app.FakeRedisGateway
import com.czh.rediscompose.app.RedisSession
import com.czh.rediscompose.data.redis.RedisException
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.domain.command.RedisReply
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsoleStateTest {
    private val toasts = mutableListOf<Pair<String, ToastKind>>()
    private val recorded = mutableListOf<String>()
    private val databaseSelections = mutableListOf<Int>()
    private var settings = AppSettings()
    private lateinit var sessionRef: RedisSession

    private fun console(
        gateway: FakeRedisGateway,
        initialSettings: AppSettings = AppSettings(),
    ): ConsoleState {
        toasts.clear()
        recorded.clear()
        databaseSelections.clear()
        settings = initialSettings
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val session =
            RedisSession(
                profile = ConnectionProfile(id = "c1", name = "Local", host = "127.0.0.1"),
                gateway = gateway,
                appScope = scope,
            )
        sessionRef = session
        scope.launch { session.connect() }
        return ConsoleState(
            session = session,
            settings = { settings },
            onToast = { message, kind -> toasts += message to kind },
            onRecordCommand = { recorded += it },
            scope = scope,
            onDatabaseSelected = { databaseSelections += it },
        )
    }

    @Test
    fun `executing a command writes output history and records it`() {
        val gateway = FakeRedisGateway()
        gateway.executeReply = RedisReply.Simple("PONG")
        val console = console(gateway)

        console.execute("PING")

        val output = console.entries.last() as ConsoleEntry.Output
        assertEquals("PONG", ConsoleOutput.view(output.reply).lines.single().text)
        assertEquals(listOf("PING"), console.history)
        assertEquals("PING", console.historyItems.first().command)
        assertEquals("simple", console.historyItems.first().summary)
        assertEquals(listOf("PING"), recorded)
        assertFalse(console.isRunning)
        assertTrue(toasts.isEmpty())
    }

    @Test
    fun `redis errors become error entries without a toast`() {
        val gateway = FakeRedisGateway()
        gateway.executeReply = RedisReply.Error("ERR unknown command 'NOPE'")
        val console = console(gateway)

        console.execute("NOPE")

        val error = console.entries.last() as ConsoleEntry.Error
        assertTrue(error.message.contains("unknown command"))
        assertTrue(console.historyItems.first().failed)
        assertTrue(toasts.isEmpty())
    }

    @Test
    fun `read only mode blocks writes and unknown commands but allows reads`() {
        val gateway = FakeRedisGateway()
        gateway.executeReply = RedisReply.Integer(1)
        val console = console(gateway, AppSettings(globalReadOnly = true))

        console.execute("SET key value")
        console.execute("GET key")
        console.execute("FOOBAR key")

        assertEquals(1, gateway.executedCommands.size)
        assertEquals(listOf("GET", "key"), gateway.executedCommands.single())
        assertTrue(console.entries.any { it is ConsoleEntry.Error && it.message.contains("只读模式") })
        assertTrue(console.entries.any { it is ConsoleEntry.Error && it.message.contains("未知命令") })
        assertTrue(console.isReadOnly)
    }

    @Test
    fun `quoted arguments keep their spaces`() {
        val gateway = FakeRedisGateway()
        gateway.executeReply = RedisReply.Simple("OK")
        val console = console(gateway)

        console.execute("""SET key "hello world"""")

        assertEquals(listOf("SET", "key", "hello world"), gateway.executedCommands.single())
    }

    @Test
    fun `parse errors do not reach the gateway`() {
        val gateway = FakeRedisGateway()
        val console = console(gateway)

        console.execute("""SET key "unfinished""")

        assertTrue(gateway.executedCommands.isEmpty())
        assertTrue((console.entries.last() as ConsoleEntry.Error).message.contains("引号"))
    }

    @Test
    fun `connection errors toast and are logged`() {
        val gateway = FakeRedisGateway()
        gateway.executeError = RedisException("Connection refused")
        val console = console(gateway)

        console.execute("PING")

        assertTrue((console.entries.last() as ConsoleEntry.Error).message.contains("连接失败"))
        assertTrue(toasts.any { it.second == ToastKind.Danger })
    }

    @Test
    fun `history navigation walks from newest to oldest and clears at the end`() {
        val gateway = FakeRedisGateway()
        gateway.executeReply = RedisReply.Integer(1)
        val console = console(gateway)
        console.execute("PING")
        console.execute("DBSIZE")

        console.fillFromHistory(-1)
        assertEquals("DBSIZE", console.input)
        console.fillFromHistory(-1)
        assertEquals("PING", console.input)
        console.fillFromHistory(-1)
        assertEquals("PING", console.input)
        console.fillFromHistory(1)
        assertEquals("DBSIZE", console.input)
        console.fillFromHistory(1)
        assertEquals("", console.input)
    }

    @Test
    fun `suggestions open on prefix and fill the composer`() {
        val console = console(FakeRedisGateway())

        console.updateInput("HG")

        assertTrue(console.suggestionVisible)
        assertTrue(console.suggestions.isNotEmpty())
        val first = console.suggestions.first()
        console.acceptSuggestion(first.command)
        assertEquals("${first.command} ", console.input)
        assertFalse(console.suggestionVisible)

        console.updateInput("")
        assertFalse(console.suggestionVisible)
    }

    @Test
    fun `clear resets the log but keeps history`() {
        val gateway = FakeRedisGateway()
        gateway.executeReply = RedisReply.Simple("PONG")
        val console = console(gateway)
        console.execute("PING")

        console.clear()

        assertEquals(1, console.entries.size)
        assertTrue(console.entries.single() is ConsoleEntry.Note)
        assertEquals(listOf("PING"), console.history)
        assertEquals(1, console.historyItems.size)
    }

    @Test
    fun `fill command sets input and closes suggestions`() {
        val console = console(FakeRedisGateway())

        console.fillCommand("INFO memory")

        assertEquals("INFO memory", console.input)
        assertFalse(console.suggestionVisible)
    }

    @Test
    fun `select command syncs the session database and notifies listeners`() {
        val gateway = FakeRedisGateway()
        gateway.executeReply = RedisReply.Simple("OK")
        val console = console(gateway)

        console.execute("SELECT 2")

        assertEquals(listOf("SELECT", "2"), gateway.executedCommands.single())
        assertEquals(2, sessionRef.currentDatabase)
        assertEquals(listOf(2), databaseSelections)
    }

    @Test
    fun `plain text log includes commands output and errors`() {
        val gateway = FakeRedisGateway()
        gateway.executeReply = RedisReply.Simple("PONG")
        val console = console(gateway)
        console.execute("PING")
        gateway.executeReply = RedisReply.Error("ERR nope")
        console.execute("NOPE")

        val text = console.plainTextLog()

        assertTrue(text.contains("PING"))
        assertTrue(text.contains("PONG"))
        assertTrue(text.contains("(error) ERR nope"))
    }

    @Test
    fun `recent commands seed initial history`() {
        val console =
            console(
                FakeRedisGateway(),
                AppSettings(recentCommands = listOf("PING", "INFO memory")),
            )

        assertEquals(listOf("PING", "INFO memory"), console.history)
        assertEquals(2, console.historyItems.size)
        assertEquals("INFO memory", console.historyItems.first().command)
        assertEquals("历史", console.historyItems.first().summary)
    }
}
