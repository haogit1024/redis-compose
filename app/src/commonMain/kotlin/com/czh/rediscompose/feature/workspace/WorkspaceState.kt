package com.czh.rediscompose.feature.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.czh.rediscompose.app.RedisSession
import com.czh.rediscompose.app.SessionStatus
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.domain.key.KeyNode
import com.czh.rediscompose.domain.key.KeyTreeBuilder
import com.czh.rediscompose.domain.key.KeyType
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.KeyView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * State holder for the key browser of one connection.
 *
 * All Redis access goes through [session]'s gateway and is launched from [scope] (the
 * session scope), so closing the tab or disconnecting cancels outstanding SCAN/TYPE/TTL
 * requests. The UI only reads state and calls the methods below.
 */
class WorkspaceState(
    val session: RedisSession,
    private val settings: () -> AppSettings,
    private val onToast: (String, ToastKind) -> Unit,
    scope: CoroutineScope,
    private val onSettingsChange: (AppSettings) -> Unit = {},
) {
    private val workspaceJob = SupervisorJob(scope.coroutineContext.job)
    private val workspaceScope = CoroutineScope(scope.coroutineContext + workspaceJob)

    val keys = mutableStateListOf<String>()
    val keyTypes = mutableStateMapOf<String, KeyType>()
    val keyTtls = mutableStateMapOf<String, Long>()

    var isLoading by mutableStateOf(false)
        private set

    var cursor by mutableStateOf(0L)
        private set

    var loadedAll by mutableStateOf(false)
        private set

    var total by mutableStateOf(0L)
        private set

    var searchInput by mutableStateOf("")

    var appliedPattern by mutableStateOf<String?>(null)
        private set

    var typeFilter by mutableStateOf<KeyType?>(null)
        private set

    var pinnedKeys by mutableStateOf<Set<String>>(emptySet())
        private set

    var selectedKey by mutableStateOf<String?>(null)
        private set

    var loadError by mutableStateOf<String?>(null)
        private set

    var tree by mutableStateOf<List<KeyNode>>(emptyList())
        private set

    var expandedDirs by mutableStateOf<Set<String>>(emptySet())
        private set

    private val loadedKeySet = mutableSetOf<String>()
    private val requestedPreviews = mutableSetOf<String>()
    private val previewSemaphore = Semaphore(PREVIEW_CONCURRENCY)
    private var scanJob: Job? = null
    private var scanGeneration = 0
    private var started = false
    private var userAdjustedExpansion = false

    val showTtl: Boolean get() = settings().showKeyTtl

    val keyView: KeyView get() = settings().keyView

    val isReadOnly: Boolean
        get() = settings().globalReadOnly || session.profile.readOnly

    val databaseIndex: Int get() = session.currentDatabase

    val separator: String get() = session.profile.keySeparator

    val hasFilters: Boolean get() = appliedPattern != null || typeFilter != null

    /** Loaded keys that match the active client-side type filter. */
    val filteredKeys: List<String>
        get() {
            val filter = typeFilter ?: return keys
            return keys.filter { key ->
                val type = keyTypes[key]
                type == null || type == filter
            }
        }

    /** Pinned keys in stable name order; pinned entries survive search filtering. */
    val pinnedKeyList: List<String>
        get() = pinnedKeys.sortedWith(compareBy({ it.lowercase() }, { it }))

    /** First load for the visible tab. Safe to call on every recomposition. */
    fun start() {
        if (started) return
        if (!session.isConnected) {
            if (session.status == SessionStatus.ERROR) {
                loadError = session.errorMessage ?: "连接不可用"
            }
            return
        }
        started = true
        refresh()
    }

    fun refresh() {
        if (!session.isConnected) {
            loadError = session.errorMessage ?: "连接不可用"
            return
        }
        resetLoadedKeys()
        launchScan(fetchTotal = true)
    }

    fun loadMore() {
        if (isLoading || loadedAll || loadError != null) return
        launchScan(fetchTotal = false)
    }

    fun retry() {
        if (isLoading) return
        loadError = null
        if (keys.isEmpty()) {
            refresh()
        } else {
            launchScan(fetchTotal = false)
        }
    }

    /** Applies a SCAN MATCH pattern; null/blank clears it and rescans the whole database. */
    fun applySearch(pattern: String?) {
        val normalized = pattern?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized == appliedPattern) return
        appliedPattern = normalized
        refresh()
    }

    fun clearSearch() {
        searchInput = ""
        applySearch(null)
    }

    fun updateTypeFilter(type: KeyType?) {
        if (type == typeFilter) return
        typeFilter = type
    }

    fun clearFilters() {
        searchInput = ""
        typeFilter = null
        applySearch(null)
    }

    fun selectKey(key: String) {
        if (key.isEmpty()) return
        selectedKey = key
    }

    fun clearSelection() {
        selectedKey = null
    }

    fun togglePinned(key: String) {
        if (key.isEmpty()) return
        pinnedKeys =
            if (key in pinnedKeys) {
                pinnedKeys - key
            } else {
                pinnedKeys + key
            }
    }

    fun isPinned(key: String): Boolean = key in pinnedKeys

    fun typeOf(key: String): KeyType? = keyTypes[key]

    fun ttlOf(key: String): Long? = keyTtls[key]

    fun toggleDirectory(path: String) {
        if (path.isEmpty()) return
        userAdjustedExpansion = true
        expandedDirs =
            if (path in expandedDirs) {
                expandedDirs - path
            } else {
                expandedDirs + path
            }
    }

    fun toggleKeyView() {
        val current = settings()
        val next = if (current.keyView == KeyView.TREE) KeyView.FLAT else KeyView.TREE
        onSettingsChange(current.copy(keyView = next))
    }

    /** Lazily loads TYPE/TTL for one visible row; at most six requests run at once. */
    fun ensurePreview(key: String) {
        if (key.isEmpty() || !session.isConnected) return
        if (!requestedPreviews.add(key)) return
        workspaceScope.launch {
            previewSemaphore.withPermit {
                val type = quietly { session.gateway.keyType(key) } ?: return@withPermit
                keyTypes[key] = type
                quietly { session.gateway.keyTtl(key) }?.let { ttl -> keyTtls[key] = ttl }
            }
        }
    }

    fun requestNewKey() {
        onToast("新建 Key 将在 Phase 5 提供", ToastKind.Info)
    }

    fun reportKeyCopied() {
        onToast("已复制 Key", ToastKind.Success)
    }

    fun dispose() {
        scanGeneration += 1
        scanJob?.cancel()
        scanJob = null
        isLoading = false
        workspaceJob.cancel()
    }

    private fun resetLoadedKeys() {
        keys.clear()
        loadedKeySet.clear()
        keyTypes.clear()
        keyTtls.clear()
        requestedPreviews.clear()
        cursor = 0
        loadedAll = false
        total = 0
        loadError = null
        rebuildTree()
    }

    private fun launchScan(fetchTotal: Boolean) {
        scanJob?.cancel()
        scanGeneration += 1
        val generation = scanGeneration
        isLoading = true
        scanJob =
            workspaceScope.launch {
                try {
                    if (fetchTotal && appliedPattern == null && total == 0L) {
                        total = quietly { session.gateway.databaseSize() } ?: 0L
                    }
                    val batchSize = settings().scanBatchSize.coerceIn(1, MAX_SCAN_BATCH)
                    val page = session.gateway.scan(cursor, appliedPattern, batchSize)
                    if (generation != scanGeneration) return@launch
                    var added = false
                    page.keys.forEach { key ->
                        if (key.isNotEmpty() && loadedKeySet.add(key)) {
                            keys.add(key)
                            added = true
                        }
                    }
                    cursor = page.cursor
                    loadedAll = page.cursor == 0L
                    loadError = null
                    if (added) rebuildTree()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    if (generation == scanGeneration) {
                        loadError = error.firstLine()
                        onToast("加载 Key 失败：${error.firstLine()}", ToastKind.Danger)
                    }
                } finally {
                    if (generation == scanGeneration) {
                        isLoading = false
                        scanJob = null
                    }
                }
            }
    }

    private fun rebuildTree() {
        tree = KeyTreeBuilder.build(keys, separator)
        if (!userAdjustedExpansion) {
            expandedDirs = tree.filter { it.isDirectory }.map { it.path }.toSet()
        }
    }

    private suspend fun <T> quietly(block: suspend () -> T): T? =
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }

    private fun Throwable.firstLine(): String =
        message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: this::class.simpleName
            ?: "未知错误"

    private companion object {
        const val PREVIEW_CONCURRENCY = 6
        const val MAX_SCAN_BATCH = 10_000
    }
}
