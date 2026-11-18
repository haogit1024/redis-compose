package com.czh.rediscompose.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.czh.rediscompose.data.redis.InfoParser
import com.czh.rediscompose.data.redis.RedisGateway
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.key.DatabaseInfo
import com.czh.rediscompose.domain.server.ServerSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlin.time.TimeSource

enum class SessionStatus { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

/**
 * Runtime state for one open connection.
 *
 * The gateway is owned here; feature states (Phase 4+) share this session. All state
 * writes happen on the Compose main thread because callers launch from the app scope.
 */
class RedisSession(
    val profile: ConnectionProfile,
    val gateway: RedisGateway,
    private val appScope: CoroutineScope,
) {
    var status by mutableStateOf(SessionStatus.DISCONNECTED)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var latencyMillis by mutableStateOf<Long?>(null)
        private set

    var databases by mutableStateOf<List<DatabaseInfo>>(emptyList())
        private set

    var currentDatabase by mutableStateOf(profile.defaultDatabase)
        private set

    var summary by mutableStateOf<ServerSnapshot?>(null)
        private set

    private var sessionJob: Job = SupervisorJob(appScope.coroutineContext.job)

    /** Scope for feature managers that must stop when the session disconnects. */
    val scope: CoroutineScope
        get() = CoroutineScope(appScope.coroutineContext + sessionJob)

    val isConnected: Boolean get() = status == SessionStatus.CONNECTED

    suspend fun connect() {
        if (status == SessionStatus.CONNECTING || status == SessionStatus.CONNECTED) return
        if (sessionJob.isCancelled) {
            sessionJob = SupervisorJob(appScope.coroutineContext.job)
        }
        status = SessionStatus.CONNECTING
        errorMessage = null
        latencyMillis = null
        val started = TimeSource.Monotonic.markNow()
        try {
            gateway.ping()
            latencyMillis = started.elapsedNow().inWholeMilliseconds
            gateway.selectDatabase(profile.defaultDatabase)
            currentDatabase = profile.defaultDatabase
            refreshDatabases()
            refreshSummary()
            status = SessionStatus.CONNECTED
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            status = SessionStatus.ERROR
            errorMessage = error.firstLine()
            println("[redis-compose] connection '${profile.id}' failed: ${error.firstLine()}")
        }
    }

    suspend fun refreshSummary() {
        summary = InfoParser.snapshot(gateway.info())
    }

    suspend fun refreshDatabases() {
        val raw = gateway.info("keyspace")
        val section = InfoParser.parse(raw)["Keyspace"].orEmpty()
        val counts = InfoParser.parseKeyspace(section).associate { it.index to it.keyCount }
        databases =
            (0 until DATABASE_COUNT).map { index ->
                DatabaseInfo(index = index, keyCount = counts[index] ?: 0L)
            }
    }

    suspend fun switchDatabase(index: Int) {
        require(index in 0 until DATABASE_COUNT) { "Invalid database index: $index" }
        gateway.selectDatabase(index)
        currentDatabase = index
    }

    /**
     * Keeps [currentDatabase] in sync when a command (such as console `SELECT`) changed the
     * database on the shared connection.
     */
    fun recordDatabaseSelection(index: Int) {
        if (index in 0 until DATABASE_COUNT) currentDatabase = index
    }

    fun disconnect() {
        runCatching { gateway.close() }
        sessionJob.cancel()
        status = SessionStatus.DISCONNECTED
        errorMessage = null
        latencyMillis = null
        summary = null
        databases = emptyList()
    }

    private fun Throwable.firstLine(): String =
        message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: "连接失败"

    companion object {
        const val DATABASE_COUNT = 16
    }
}
