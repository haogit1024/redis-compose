package com.czh.rediscompose.feature.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.czh.rediscompose.app.RedisSession
import com.czh.rediscompose.app.SessionStatus
import com.czh.rediscompose.data.redis.ValueCodec
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.domain.command.RedisReply
import com.czh.rediscompose.domain.key.HashField
import com.czh.rediscompose.domain.key.KeyDetail
import com.czh.rediscompose.domain.key.KeyMetadata
import com.czh.rediscompose.domain.key.KeyNode
import com.czh.rediscompose.domain.key.KeySummary
import com.czh.rediscompose.domain.key.KeyTreeBuilder
import com.czh.rediscompose.domain.key.KeyType
import com.czh.rediscompose.domain.key.KeyValue
import com.czh.rediscompose.domain.key.SortedSetEntry
import com.czh.rediscompose.domain.key.StreamEntry
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.KeyView
import com.czh.rediscompose.domain.settings.ValueFormat
import com.czh.rediscompose.util.currentEpochSeconds
import com.czh.rediscompose.util.formatCount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** One editable hash row; a null [originalField] means a new, unsaved row. */
class EditableHashField(
    val originalField: String?,
    field: String,
    value: String,
    val originalValue: String = value,
) {
    var field by mutableStateOf(field)
    var value by mutableStateOf(value)

    val isNew: Boolean get() = originalField == null

    val isDirty: Boolean
        get() {
            if (isNew) return true
            return this.field != originalField || value != originalValue
        }
}

/** One list row; [index] is the Redis index at load time. */
class EditableListMember(
    val index: Long,
    value: String,
    val originalValue: String = value,
) {
    var value by mutableStateOf(value)

    val isDirty: Boolean get() = value != originalValue
}

/** One set member; a null [original] means a new member. */
class EditableSetMember(
    val original: String?,
    value: String,
) {
    var value by mutableStateOf(value)

    val isNew: Boolean get() = original == null

    val isDirty: Boolean get() = isNew || value != original
}

/** One sorted-set entry; a null [originalMember] means a new member. */
class EditableSortedSetEntry(
    val originalMember: String?,
    member: String,
    score: String,
    val originalScore: String = score,
) {
    var member by mutableStateOf(member)
    var score by mutableStateOf(score)

    val isNew: Boolean get() = originalMember == null

    val isDirty: Boolean get() = isNew || member != originalMember || score != originalScore
}

/** Draft used by the New Key dialog. Only the field for [type] is read. */
data class NewKeyDraft(
    val name: String,
    val type: KeyType,
    val ttlSeconds: Long = -1,
    val stringValue: String = "",
    val hashField: String = "",
    val hashValue: String = "",
    val listValue: String = "",
    val listHead: Boolean = false,
    val setMember: String = "",
    val zsetMember: String = "",
    val zsetScore: String = "0",
)

/**
 * State holder for the key browser and value inspector of one connection.
 *
 * All Redis access goes through [session]'s gateway and is launched from [scope] (the
 * session scope), so closing the tab or disconnecting cancels outstanding requests. The UI
 * only reads state and calls the methods below.
 */
class WorkspaceState(
    val session: RedisSession,
    private val settings: () -> AppSettings,
    private val onToast: (String, ToastKind) -> Unit,
    scope: CoroutineScope,
    private val onSettingsChange: (AppSettings) -> Unit = {},
    private val onOpenConsole: () -> Unit = {},
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

    var keyDetail by mutableStateOf<KeyDetail?>(null)
        private set

    var detailLoading by mutableStateOf(false)
        private set

    var detailError by mutableStateOf<String?>(null)
        private set

    var detailLoadedAtEpochSeconds by mutableStateOf(0L)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var hasUnsavedEdits by mutableStateOf(false)

    var ttlDialogKey by mutableStateOf<String?>(null)
        private set

    var renameDialogKey by mutableStateOf<String?>(null)
        private set

    var deleteDialogKey by mutableStateOf<String?>(null)
        private set

    var newKeyDialogVisible by mutableStateOf(false)
        private set

    var saveRequestId by mutableStateOf(0)
        private set

    var searchFocusRequestId by mutableStateOf(0)
        private set

    private var handledSaveRequest = 0
    private val loadedKeySet = mutableSetOf<String>()
    private val requestedPreviews = mutableSetOf<String>()
    private val previewSemaphore = Semaphore(PREVIEW_CONCURRENCY)
    private var scanJob: Job? = null
    private var scanGeneration = 0
    private var detailJob: Job? = null
    private var detailGeneration = 0
    private var started = false

    val showTtl: Boolean get() = settings().showKeyTtl

    val keyView: KeyView get() = settings().keyView

    val isReadOnly: Boolean
        get() = settings().globalReadOnly || session.profile.readOnly

    val canWrite: Boolean get() = !isReadOnly && session.isConnected

    val truncateValueKb: Int get() = settings().truncateValueKb

    val editorFontSize: Int get() = settings().editorFontSize

    val editorWordWrap: Boolean get() = settings().wordWrap

    val showLineNumbers: Boolean get() = settings().showLineNumbers

    val defaultValueFormat: ValueFormat get() = settings().defaultValueFormat

    val maskSensitiveValues: Boolean get() = settings().maskSensitiveValues

    val confirmDeleteKeys: Boolean get() = settings().confirmDeleteKeys

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

    fun loadMore(notify: Boolean = false) {
        if (isLoading || loadedAll) return
        // A failed page leaves the load-more row on screen; clicking it must retry instead
        // of silently doing nothing.
        if (loadError != null) {
            retry()
            return
        }
        launchScan(fetchTotal = false, announce = notify)
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
        if (key != selectedKey && hasUnsavedEdits) {
            onToast("上一个 Key 有未保存的修改", ToastKind.Warning)
        }
        hasUnsavedEdits = false
        selectedKey = key
        loadDetail(key, keepCurrent = false)
    }

    fun clearSelection() {
        selectedKey = null
        detailJob?.cancel()
        detailGeneration += 1
        keyDetail = null
        detailError = null
        detailLoading = false
        hasUnsavedEdits = false
        ttlDialogKey = null
        renameDialogKey = null
        deleteDialogKey = null
    }

    fun refreshSelected() {
        val key = selectedKey ?: return
        loadDetail(key, keepCurrent = true)
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
        openNewKeyDialog()
    }

    fun requestSave() {
        saveRequestId += 1
    }

    /** Global ⌘F asks the screen to focus the SCAN search field. */
    fun requestSearchFocus() {
        searchFocusRequestId += 1
    }

    fun consumeSaveRequest(): Boolean {
        if (saveRequestId == handledSaveRequest) return false
        handledSaveRequest = saveRequestId
        return true
    }

    fun reportKeyCopied() {
        onToast("已复制 Key", ToastKind.Success)
    }

    fun reportCommandCopied() {
        onToast("已复制 Redis 命令", ToastKind.Success)
    }

    fun reportInvalidJson() {
        onToast("无效的 JSON", ToastKind.Warning)
    }

    fun reportValueCopied() {
        onToast("已复制完整值", ToastKind.Success)
    }

    fun openInConsole() {
        onOpenConsole()
    }

    fun openNewKeyDialog() {
        if (!canWrite) {
            denyWrite()
            return
        }
        newKeyDialogVisible = true
    }

    fun dismissNewKeyDialog() {
        newKeyDialogVisible = false
    }

    fun openTtlDialog() {
        if (selectedKey == null) return
        if (!canWrite) {
            denyWrite()
            return
        }
        ttlDialogKey = selectedKey
    }

    fun dismissTtlDialog() {
        ttlDialogKey = null
    }

    fun openRenameDialog() {
        if (selectedKey == null) return
        if (!canWrite) {
            denyWrite()
            return
        }
        renameDialogKey = selectedKey
    }

    fun dismissRenameDialog() {
        renameDialogKey = null
    }

    fun openDeleteDialog() {
        if (selectedKey == null) return
        if (!canWrite) {
            denyWrite()
            return
        }
        if (!confirmDeleteKeys) {
            deleteSelectedKey()
            return
        }
        deleteDialogKey = selectedKey
    }

    fun dismissDeleteDialog() {
        deleteDialogKey = null
    }

    fun saveString(text: String) {
        val key = selectedKey ?: return
        if ((keyDetail?.value as? KeyValue.StringValue)?.isBinary == true) {
            onToast("二进制值只读，无法保存为文本", ToastKind.Warning)
            return
        }
        launchWrite(
            successMessage = "已保存",
            onSuccess = {
                hasUnsavedEdits = false
                refreshSelected()
            },
        ) {
            session.gateway.setString(key, text.encodeToByteArray())
        }
    }

    fun saveHashField(row: EditableHashField) {
        saveHashFields(listOf(row))
    }

    fun saveHashFields(rows: List<EditableHashField>) {
        val key = selectedKey ?: return
        val pending = rows.filter { it.isDirty }
        if (pending.isEmpty()) {
            onToast("没有需要保存的修改", ToastKind.Info)
            return
        }
        if (pending.any { it.field.isBlank() }) {
            onToast("Hash field 不能为空", ToastKind.Warning)
            return
        }
        launchWrite(
            successMessage = "已保存 Hash 字段",
            onSuccess = { refreshSelected() },
        ) {
            pending.forEach { row ->
                session.gateway.hashSet(key, row.field, row.value.encodeToByteArray())
                val original = row.originalField
                if (original != null && original != row.field) {
                    session.gateway.hashDelete(key, original)
                }
            }
        }
    }

    fun deleteHashField(row: EditableHashField) {
        val key = selectedKey ?: return
        val original = row.originalField ?: return
        launchWrite(
            successMessage = "已删除字段",
            onSuccess = { refreshSelected() },
        ) {
            session.gateway.hashDelete(key, original)
        }
    }

    fun saveListMember(
        index: Long,
        value: String,
    ) {
        val key = selectedKey ?: return
        launchWrite(
            successMessage = "已保存列表成员",
            onSuccess = { refreshSelected() },
        ) {
            session.gateway.listSet(key, index, value.encodeToByteArray())
        }
    }

    fun saveListMembers(rows: List<EditableListMember>) {
        val key = selectedKey ?: return
        val pending = rows.filter { it.isDirty }
        if (pending.isEmpty()) {
            onToast("没有需要保存的修改", ToastKind.Info)
            return
        }
        launchWrite(
            successMessage = "已保存列表成员",
            onSuccess = { refreshSelected() },
        ) {
            pending.forEach { row ->
                session.gateway.listSet(key, row.index, row.value.encodeToByteArray())
            }
        }
    }

    fun addListMember(
        value: String,
        head: Boolean,
    ) {
        val key = selectedKey ?: return
        launchWrite(
            successMessage = if (head) "已在头部插入成员" else "已在尾部插入成员",
            onSuccess = { refreshSelected() },
        ) {
            session.gateway.listPush(key, value.encodeToByteArray(), head)
        }
    }

    fun deleteListMember(
        index: Long,
        value: String,
    ) {
        val key = selectedKey ?: return
        launchWrite(
            successMessage = "已删除列表成员",
            onSuccess = { refreshSelected() },
        ) {
            session.gateway.listRemove(key, value.encodeToByteArray())
        }
    }

    fun saveSetMember(row: EditableSetMember) {
        saveSetMembers(listOf(row))
    }

    fun saveSetMembers(rows: List<EditableSetMember>) {
        val key = selectedKey ?: return
        val pending = rows.filter { it.isDirty }
        if (pending.isEmpty()) {
            onToast("没有需要保存的修改", ToastKind.Info)
            return
        }
        val existingMembers = (keyDetail?.value as? KeyValue.SetValue)?.members.orEmpty()
        val targets = mutableSetOf<String>()
        for (row in pending) {
            if (!targets.add(row.value)) {
                onToast("成员重复：${row.value}", ToastKind.Warning)
                return
            }
            if (existingMembers.any { member -> member == row.value && member != row.original }) {
                onToast("成员已存在：${row.value}", ToastKind.Warning)
                return
            }
        }
        launchWrite(
            successMessage = "已保存 Set 成员",
            onSuccess = { refreshSelected() },
        ) {
            pending.forEach { row ->
                session.gateway.setAdd(key, row.value.encodeToByteArray())
                val original = row.original
                if (original != null && original != row.value) {
                    session.gateway.setRemove(key, original.encodeToByteArray())
                }
            }
        }
    }

    fun deleteSetMember(row: EditableSetMember) {
        val key = selectedKey ?: return
        val original = row.original ?: return
        launchWrite(
            successMessage = "已删除 Set 成员",
            onSuccess = { refreshSelected() },
        ) {
            session.gateway.setRemove(key, original.encodeToByteArray())
        }
    }

    fun saveSortedSetEntry(row: EditableSortedSetEntry) {
        saveSortedSetEntries(listOf(row))
    }

    fun saveSortedSetEntries(rows: List<EditableSortedSetEntry>) {
        val key = selectedKey ?: return
        val pending = rows.filter { it.isDirty }
        if (pending.isEmpty()) {
            onToast("没有需要保存的修改", ToastKind.Info)
            return
        }
        val parsed = mutableListOf<Pair<EditableSortedSetEntry, Double>>()
        for (row in pending) {
            val score = row.score.toDoubleOrNull()
            if (score == null || !score.isFinite()) {
                onToast("Score 必须是有限数字", ToastKind.Warning)
                return
            }
            parsed += row to score
        }
        if (parsed.any { (row, _) -> row.member.isBlank() }) {
            onToast("Member 不能为空", ToastKind.Warning)
            return
        }
        launchWrite(
            successMessage = "已保存 Sorted Set 成员",
            onSuccess = { refreshSelected() },
        ) {
            parsed.forEach { (row, score) ->
                session.gateway.sortedSetAdd(key, row.member.encodeToByteArray(), score)
                val original = row.originalMember
                if (original != null && original != row.member) {
                    session.gateway.sortedSetRemove(key, original.encodeToByteArray())
                }
            }
        }
    }

    fun deleteSortedSetEntry(row: EditableSortedSetEntry) {
        val key = selectedKey ?: return
        val original = row.originalMember ?: return
        launchWrite(
            successMessage = "已删除 Sorted Set 成员",
            onSuccess = { refreshSelected() },
        ) {
            session.gateway.sortedSetRemove(key, original.encodeToByteArray())
        }
    }

    fun updateTtl(seconds: Long) {
        val key = selectedKey ?: return
        if (seconds == 0L || seconds < -1) {
            onToast("TTL 必须为 -1 或正整数", ToastKind.Warning)
            return
        }
        launchWrite(
            successMessage = if (seconds == -1L) "已设为永久" else "已更新 TTL",
            onSuccess = {
                ttlDialogKey = null
                refreshSelected()
            },
        ) {
            if (seconds == -1L) {
                session.gateway.persist(key)
            } else {
                session.gateway.expire(key, seconds)
            }
        }
    }

    fun renameKey(newName: String) {
        val oldName = selectedKey ?: return
        val target = newName.trim()
        if (target.isEmpty()) {
            onToast("Key 名不能为空", ToastKind.Warning)
            return
        }
        if (target == oldName) {
            onToast("新 Key 名与原名相同", ToastKind.Warning)
            return
        }
        launchWrite(
            successMessage = "已重命名 Key",
            onSuccess = {
                renameDialogKey = null
                applyRename(oldName, target)
                loadDetail(target, keepCurrent = false)
            },
        ) {
            val destinationType = session.gateway.keyType(target)
            if (destinationType != KeyType.NONE) {
                throw IllegalStateException("目标 Key 已存在")
            }
            session.gateway.renameKey(oldName, target)
        }
    }

    fun deleteSelectedKey() {
        val key = selectedKey ?: return
        launchWrite(
            successMessage = "已删除 Key",
            onSuccess = {
                removeKeyLocally(key)
                deleteDialogKey = null
            },
        ) {
            session.gateway.deleteKeys(listOf(key))
        }
    }

    fun createKey(draft: NewKeyDraft) {
        if (!canWrite) {
            denyWrite()
            return
        }
        if (isSaving) return
        val name = draft.name.trim()
        val validationError = validateNewKey(draft, name)
        if (validationError != null) {
            onToast(validationError, ToastKind.Warning)
            return
        }
        val score = draft.zsetScore.toDoubleOrNull() ?: 0.0
        isSaving = true
        workspaceScope.launch {
            try {
                if (session.gateway.keyType(name) != KeyType.NONE) {
                    throw IllegalStateException("Key 已存在")
                }
                when (draft.type) {
                    KeyType.STRING -> session.gateway.setString(name, draft.stringValue.encodeToByteArray())
                    KeyType.HASH ->
                        session.gateway.hashSet(name, draft.hashField, draft.hashValue.encodeToByteArray())
                    KeyType.LIST -> session.gateway.listPush(name, draft.listValue.encodeToByteArray(), draft.listHead)
                    KeyType.SET -> session.gateway.setAdd(name, draft.setMember.encodeToByteArray())
                    KeyType.ZSET -> session.gateway.sortedSetAdd(name, draft.zsetMember.encodeToByteArray(), score)
                    else -> throw IllegalArgumentException("不支持新建该类型")
                }
                if (draft.ttlSeconds > 0) {
                    session.gateway.expire(name, draft.ttlSeconds)
                }
                newKeyDialogVisible = false
                onToast("已创建 Key", ToastKind.Success)
                refresh()
                selectKey(name)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onToast("创建 Key 失败：${error.firstLine()}", ToastKind.Danger)
            } finally {
                isSaving = false
            }
        }
    }

    fun dispose() {
        scanGeneration += 1
        detailGeneration += 1
        scanJob?.cancel()
        detailJob?.cancel()
        scanJob = null
        detailJob = null
        isLoading = false
        detailLoading = false
        workspaceJob.cancel()
    }

    private fun validateNewKey(
        draft: NewKeyDraft,
        name: String,
    ): String? =
        when {
            name.isEmpty() -> "Key 名不能为空"
            draft.ttlSeconds != -1L && draft.ttlSeconds <= 0 -> "TTL 必须为 -1 或正整数"
            draft.type == KeyType.STRING -> null
            draft.type == KeyType.HASH && draft.hashField.isBlank() -> "Hash field 不能为空"
            draft.type == KeyType.LIST && draft.listValue.isEmpty() -> "List 至少需要一个成员"
            draft.type == KeyType.SET && draft.setMember.isEmpty() -> "Set 至少需要一个成员"
            draft.type == KeyType.ZSET && draft.zsetMember.isEmpty() -> "Sorted Set 至少需要一个成员"
            draft.type == KeyType.ZSET && draft.zsetScore.toDoubleOrNull()?.isFinite() != true ->
                "Score 必须是有限数字"
            draft.type !in CREATABLE_TYPES -> "不支持新建该类型"
            else -> null
        }

    private fun applyRename(
        oldName: String,
        newName: String,
    ) {
        val index = keys.indexOf(oldName)
        if (index >= 0) {
            keys[index] = newName
        } else if (!loadedKeySet.contains(newName)) {
            keys.add(newName)
        }
        loadedKeySet.remove(oldName)
        loadedKeySet.add(newName)
        if (oldName in pinnedKeys) {
            pinnedKeys = pinnedKeys - oldName + newName
        }
        keyTypes.remove(oldName)?.let { keyTypes[newName] = it }
        keyTtls.remove(oldName)?.let { keyTtls[newName] = it }
        selectedKey = newName
        rebuildTree()
    }

    private fun removeKeyLocally(key: String) {
        keys.remove(key)
        loadedKeySet.remove(key)
        pinnedKeys = pinnedKeys - key
        keyTypes.remove(key)
        keyTtls.remove(key)
        requestedPreviews.remove(key)
        selectedKey = null
        keyDetail = null
        detailError = null
        detailLoading = false
        hasUnsavedEdits = false
        total = (total - 1).coerceAtLeast(0)
        detailJob?.cancel()
        detailGeneration += 1
        rebuildTree()
    }

    private fun denyWrite() {
        onToast(
            if (isReadOnly) "只读模式已开启，写操作被拦截" else "连接不可用，无法写入",
            ToastKind.Warning,
        )
    }

    private fun launchWrite(
        successMessage: String,
        onSuccess: suspend () -> Unit,
        block: suspend () -> Unit,
    ) {
        if (!canWrite) {
            denyWrite()
            return
        }
        if (isSaving) return
        isSaving = true
        workspaceScope.launch {
            try {
                block()
                onSuccess()
                onToast(successMessage, ToastKind.Success)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onToast("操作失败：${error.firstLine()}", ToastKind.Danger)
            } finally {
                isSaving = false
            }
        }
    }

    private fun loadDetail(
        key: String,
        keepCurrent: Boolean,
    ) {
        detailJob?.cancel()
        detailGeneration += 1
        val generation = detailGeneration
        detailLoading = true
        detailError = null
        if (!keepCurrent) keyDetail = null
        detailJob =
            workspaceScope.launch {
                try {
                    val type = session.gateway.keyType(key)
                    if (type == KeyType.NONE) {
                        if (generation == detailGeneration) {
                            keyDetail = null
                            detailError = "Key 不存在或已过期"
                        }
                        return@launch
                    }
                    val ttl = session.gateway.keyTtl(key)
                    val memory = session.gateway.keyMemoryUsage(key)
                    val encoding = session.gateway.keyEncoding(key)
                    val value = loadValue(key, type)
                    val elementCount = elementCountOf(key, type, value)
                    if (generation != detailGeneration) return@launch
                    keyDetail =
                        KeyDetail(
                            summary = KeySummary(name = key, type = type, ttlSeconds = ttl),
                            metadata =
                                KeyMetadata(
                                    ttlSeconds = ttl,
                                    memoryBytes = memory,
                                    encoding = encoding,
                                    elementCount = elementCount,
                                ),
                            value = value,
                        )
                    keyTypes[key] = type
                    keyTtls[key] = ttl
                    detailLoadedAtEpochSeconds = currentEpochSeconds()
                    detailError = null
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    if (generation == detailGeneration) {
                        detailError = error.firstLine()
                        onToast("读取值失败：${error.firstLine()}", ToastKind.Danger)
                    }
                } finally {
                    if (generation == detailGeneration) {
                        detailLoading = false
                        detailJob = null
                    }
                }
            }
    }

    private suspend fun loadValue(
        key: String,
        type: KeyType,
    ): KeyValue =
        when (type) {
            KeyType.STRING -> {
                val bytes = session.gateway.getString(key) ?: ByteArray(0)
                val decoded = ValueCodec.decode(bytes)
                KeyValue.StringValue(
                    text = decoded ?: ValueCodec.hex(bytes),
                    isJson = decoded != null && ValueCodec.isJson(decoded),
                    isBinary = decoded == null,
                    byteLength = bytes.size.toLong(),
                )
            }
            KeyType.HASH ->
                KeyValue.HashValue(
                    session.gateway.getHash(key).map { field ->
                        HashField(field = field.field, value = decodeBytes(field.value))
                    },
                )
            KeyType.LIST ->
                KeyValue.ListValue(session.gateway.getList(key).map(::decodeBytes))
            KeyType.SET ->
                KeyValue.SetValue(session.gateway.getSet(key).map(::decodeBytes))
            KeyType.ZSET ->
                KeyValue.SortedSetValue(
                    session.gateway.getSortedSet(key)
                        .map { entry -> SortedSetEntry(member = decodeBytes(entry.member), score = entry.score) }
                        .sortedByDescending { it.score },
                )
            KeyType.STREAM -> {
                val entries =
                    session.gateway.getStream(key, STREAM_PREVIEW_COUNT).map { entry ->
                        StreamEntry(
                            id = entry.id,
                            fields = entry.fields.map { (field, value) -> field to decodeBytes(value) },
                        )
                    }
                KeyValue.StreamValue(entries = entries, groupCount = quietly { session.gateway.getStreamGroupCount(key) })
            }
            KeyType.NONE -> throw IllegalStateException("Key 不存在或已过期")
        }

    private suspend fun elementCountOf(
        key: String,
        type: KeyType,
        value: KeyValue,
    ): Long? =
        when (type) {
            KeyType.STRING -> (value as? KeyValue.StringValue)?.byteLength
            KeyType.HASH -> (value as? KeyValue.HashValue)?.fields?.size?.toLong()
            KeyType.LIST -> (value as? KeyValue.ListValue)?.items?.size?.toLong()
            KeyType.SET -> (value as? KeyValue.SetValue)?.members?.size?.toLong()
            KeyType.ZSET -> (value as? KeyValue.SortedSetValue)?.entries?.size?.toLong()
            KeyType.STREAM -> streamLength(key) ?: (value as? KeyValue.StreamValue)?.entries?.size?.toLong()
            KeyType.NONE -> null
        }

    private suspend fun streamLength(key: String): Long? =
        try {
            val reply = session.gateway.execute(listOf("XLEN", key))
            (reply as? RedisReply.Integer)?.value
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }

    private fun decodeBytes(bytes: ByteArray): String = ValueCodec.decode(bytes) ?: ValueCodec.hex(bytes)

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

    private fun launchScan(
        fetchTotal: Boolean,
        announce: Boolean = false,
    ) {
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
                    var added = 0
                    var emptyPages = 0
                    // SCAN may return a page whose keys are already loaded, and MATCH can
                    // filter out a whole page. Keep scanning so a load-more click visibly
                    // adds keys instead of silently advancing the cursor.
                    while (true) {
                        val page = session.gateway.scan(cursor, appliedPattern, batchSize)
                        if (generation != scanGeneration) return@launch
                        page.keys.forEach { key ->
                            if (key.isNotEmpty() && loadedKeySet.add(key)) {
                                keys.add(key)
                                added += 1
                            }
                        }
                        cursor = page.cursor
                        loadedAll = page.cursor == 0L
                        loadError = null
                        if (added > 0 || loadedAll || ++emptyPages >= MAX_EMPTY_SCAN_PAGES) break
                    }
                    if (added > 0) rebuildTree()
                    if (announce) announceScanResult(added)
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

    private fun announceScanResult(added: Int) {
        val message =
            when {
                added > 0 ->
                    "本次加载 ${formatCount(added.toLong())} 个 Key（已加载 ${formatCount(keys.size.toLong())}）"
                loadedAll -> "已加载全部 ${formatCount(keys.size.toLong())} 个 Key"
                else -> "本次没有扫描到新的 Key，可继续加载或调整匹配模式"
            }
        onToast(message, ToastKind.Info)
    }

    private fun rebuildTree() {
        tree = KeyTreeBuilder.build(keys, separator)
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
        const val MAX_EMPTY_SCAN_PAGES = 5
        const val STREAM_PREVIEW_COUNT = 50
        val CREATABLE_TYPES = setOf(KeyType.STRING, KeyType.HASH, KeyType.LIST, KeyType.SET, KeyType.ZSET)
    }
}
