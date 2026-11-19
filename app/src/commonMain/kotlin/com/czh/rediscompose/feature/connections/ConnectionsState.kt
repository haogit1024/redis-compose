package com.czh.rediscompose.feature.connections

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.czh.rediscompose.app.SessionStatus
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.domain.connection.ConnectionEnvironment
import com.czh.rediscompose.domain.connection.ConnectionField
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.ConnectionTestResult
import com.czh.rediscompose.util.currentEpochMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class ConnectionSort(val label: String) {
    RECENT("最近使用"),
    NAME("名称"),
}

enum class ConnectionView { GRID, LIST }

/** Runtime facts the card may show; everything is null/unknown until a session exists. */
data class ConnectionRuntime(
    val status: SessionStatus = SessionStatus.DISCONNECTED,
    val latencyMillis: Long? = null,
    val errorMessage: String? = null,
    val version: String? = null,
    val totalKeys: Long? = null,
    val connectedClients: Int? = null,
    val databaseCount: Int? = null,
)

class ConnectionsState(
    private val connections: () -> List<ConnectionProfile>,
    private val runtimeFor: (String) -> ConnectionRuntime,
    private val onOpen: (String) -> Unit,
    private val onSave: (ConnectionProfile) -> Unit,
    private val onDelete: (String) -> Unit,
    private val onDuplicate: (String) -> ConnectionProfile?,
    private val onTest: suspend (ConnectionProfile) -> ConnectionTestResult,
    private val onToast: (String, ToastKind) -> Unit,
    scope: CoroutineScope,
) {
    private val stateJob = SupervisorJob(scope.coroutineContext.job)
    private val stateScope = CoroutineScope(scope.coroutineContext + stateJob)
    private var testJob: Job? = null

    var search by mutableStateOf("")
        private set

    var environmentFilter by mutableStateOf<ConnectionEnvironment?>(null)
        private set

    var sort by mutableStateOf(ConnectionSort.RECENT)
        private set

    var view by mutableStateOf(ConnectionView.GRID)
        private set

    var draft by mutableStateOf<ConnectionDraft?>(null)
        private set

    var sheetTab by mutableStateOf(ConnectionSheetTab.BASIC)
        private set

    var validationErrors by mutableStateOf<Map<ConnectionField, String>>(emptyMap())
        private set

    var testResult by mutableStateOf<ConnectionTestResult?>(null)
        private set

    var isTesting by mutableStateOf(false)
        private set

    var testingConnectionId by mutableStateOf<String?>(null)
        private set

    var deleteTarget by mutableStateOf<ConnectionProfile?>(null)
        private set

    val isSheetVisible: Boolean get() = draft != null

    val onlineCount: Int
        get() = connections().count { runtimeFor(it.id).status == SessionStatus.CONNECTED }

    val totalCount: Int get() = connections().size

    val filteredConnections: List<ConnectionProfile>
        get() {
            val query = search.trim().lowercase()
            val filtered =
                connections().filter { profile ->
                    val matchesEnvironment = environmentFilter == null || profile.environment == environmentFilter
                    val matchesQuery =
                        query.isEmpty() ||
                            profile.name.lowercase().contains(query) ||
                            profile.host.lowercase().contains(query) ||
                            profile.group.lowercase().contains(query)
                    matchesEnvironment && matchesQuery
                }
            return when (sort) {
                ConnectionSort.RECENT ->
                    filtered.sortedWith(
                        compareByDescending<ConnectionProfile> { it.lastOpenedAtEpochMillis ?: Long.MIN_VALUE }
                            .thenBy { it.name.lowercase() },
                    )
                ConnectionSort.NAME -> filtered.sortedBy { it.name.lowercase() }
            }
        }

    val recentConnections: List<ConnectionProfile>
        get() =
            connections()
                .filter { it.lastOpenedAtEpochMillis != null }
                .sortedByDescending { it.lastOpenedAtEpochMillis }
                .take(6)

    val hasActiveFilters: Boolean
        get() = search.isNotBlank() || environmentFilter != null

    fun updateSearch(value: String) {
        search = value
    }

    fun selectEnvironment(value: ConnectionEnvironment?) {
        environmentFilter = value
    }

    fun chooseSort(value: ConnectionSort) {
        sort = value
    }

    fun chooseView(value: ConnectionView) {
        view = value
    }

    fun clearFilters() {
        search = ""
        environmentFilter = null
    }

    fun runtimeFor(connectionId: String): ConnectionRuntime = runtimeFor.invoke(connectionId)

    fun open(connectionId: String) {
        onOpen(connectionId)
    }

    fun openNew() {
        testJob?.cancel()
        draft = ConnectionDraft.new()
        sheetTab = ConnectionSheetTab.BASIC
        validationErrors = emptyMap()
        testResult = null
    }

    fun openEdit(connectionId: String) {
        val profile = connections().firstOrNull { it.id == connectionId } ?: return
        testJob?.cancel()
        draft = ConnectionDraft.fromProfile(profile, ConnectionFormMode.EDIT)
        sheetTab = ConnectionSheetTab.BASIC
        validationErrors = emptyMap()
        testResult = null
    }

    fun openDuplicate(connectionId: String) {
        val duplicate = onDuplicate(connectionId) ?: return
        testJob?.cancel()
        draft = ConnectionDraft.fromProfile(duplicate, ConnectionFormMode.DUPLICATE)
        sheetTab = ConnectionSheetTab.BASIC
        validationErrors = emptyMap()
        testResult = null
    }

    fun closeSheet() {
        testJob?.cancel()
        testJob = null
        isTesting = false
        draft = null
        validationErrors = emptyMap()
        testResult = null
    }

    fun selectSheetTab(value: ConnectionSheetTab) {
        sheetTab = value
    }

    fun updateDraft(transform: (ConnectionDraft) -> ConnectionDraft) {
        draft = draft?.let(transform)
        if (validationErrors.isNotEmpty()) validationErrors = emptyMap()
        testResult = null
    }

    fun save() {
        val current = draft ?: return
        val now = currentEpochMillis()
        val profile = current.toProfile(now, current.id ?: newConnectionId(now))
        val errors = current.validate(now)
        if (errors.isNotEmpty()) {
            validationErrors = errors
            return
        }
        onSave(profile)
        onToast(if (current.isEdit) "连接已保存" else "连接已创建", ToastKind.Success)
        closeSheet()
    }

    fun testDraft() {
        val current = draft ?: return
        if (isTesting) return
        val now = currentEpochMillis()
        val profile = current.toProfile(now, current.id ?: "draft-test")
        val errors = current.validate(now)
        if (errors.isNotEmpty()) {
            validationErrors = errors
            return
        }
        testJob?.cancel()
        isTesting = true
        testResult = null
        testJob =
            stateScope.launch {
                try {
                    testResult = onTest(profile)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    testResult = ConnectionTestResult(success = false, errorMessage = error.firstLine())
                } finally {
                    isTesting = false
                    testJob = null
                }
            }
    }

    fun testExisting(connectionId: String) {
        if (testingConnectionId != null) return
        val profile = connections().firstOrNull { it.id == connectionId } ?: return
        testingConnectionId = connectionId
        stateScope.launch {
            val result =
                try {
                    onTest(profile)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    ConnectionTestResult(success = false, errorMessage = error.firstLine())
                } finally {
                    testingConnectionId = null
                }
            if (result.success) {
                onToast(testSuccessText(result), ToastKind.Success)
            } else {
                onToast("连接失败：${result.errorMessage ?: "未知错误"}", ToastKind.Danger)
            }
        }
    }

    fun requestDelete(connectionId: String) {
        deleteTarget = connections().firstOrNull { it.id == connectionId }
    }

    fun dismissDelete() {
        deleteTarget = null
    }

    fun confirmDelete() {
        val target = deleteTarget ?: return
        onDelete(target.id)
        deleteTarget = null
        onToast("已删除连接", ToastKind.Info)
    }

    fun dispose() {
        testJob?.cancel()
        testJob = null
        stateJob.cancel()
    }

    private fun newConnectionId(now: Long): String = "conn-$now-${Random.nextInt(1_000, 10_000)}"

    private fun Throwable.firstLine(): String =
        message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: this::class.simpleName
            ?: "未知错误"

    companion object {
        fun testSuccessText(result: ConnectionTestResult): String =
            buildString {
                append("连接成功")
                result.latencyMillis?.let { append(" · $it ms") }
                result.version?.takeIf { it.isNotBlank() }?.let { append(" · Redis $it") }
            }
    }
}

/** Human label for a recent-connection timestamp. */
fun relativeConnectionTime(
    atEpochMillis: Long?,
    nowEpochMillis: Long = currentEpochMillis(),
): String {
    if (atEpochMillis == null) return "—"
    val seconds = ((nowEpochMillis - atEpochMillis).coerceAtLeast(0L)) / 1_000L
    return when {
        seconds < 60L -> "刚刚使用"
        seconds < 3_600L -> "${seconds / 60L} 分钟前"
        seconds < 86_400L -> "${seconds / 3_600L} 小时前"
        else -> "${seconds / 86_400L} 天前"
    }
}
