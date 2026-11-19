package com.czh.rediscompose.feature.console

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.czh.rediscompose.app.RedisSession
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.domain.command.RedisReply
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.util.CommandSafety
import com.czh.rediscompose.util.DangerousCommands
import com.czh.rediscompose.util.RedisCommandCatalog
import com.czh.rediscompose.util.RedisCommandParser
import com.czh.rediscompose.util.RedisCommandSuggestion
import com.czh.rediscompose.util.WriteCommandGuard
import com.czh.rediscompose.util.currentEpochSeconds
import com.czh.rediscompose.util.formatEpochSeconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/**
 * One command waiting for the dangerous-command confirmation dialog.
 */
data class PendingDangerousCommand(
    val command: String,
    val tokens: List<String>,
    val at: String,
)

/**
 * State holder for one command console tab.
 *
 * The console keeps every entry in memory (capped), records history commands through
 * [onRecordCommand], and enforces read-only mode before anything reaches the gateway.
 * Dangerous commands are parked in [pendingDangerous] until the user confirms.
 */
class ConsoleState(
    val session: RedisSession,
    private val settings: () -> AppSettings,
    private val onToast: (String, ToastKind) -> Unit,
    private val onRecordCommand: (String) -> Unit,
    scope: CoroutineScope,
    private val onDatabaseSelected: (Int) -> Unit = {},
) {
    private val consoleJob = SupervisorJob(scope.coroutineContext.job)
    private val consoleScope = CoroutineScope(scope.coroutineContext + consoleJob)

    val entries = mutableStateListOf<ConsoleEntry>()
    val history = mutableStateListOf<String>()
    val historyItems = mutableStateListOf<ConsoleHistoryItem>()

    var input by mutableStateOf("")
        private set

    var isRunning by mutableStateOf(false)
        private set

    var suggestionVisible by mutableStateOf(false)
        private set

    var historyCursor by mutableStateOf(-1)
        private set

    var pendingDangerous by mutableStateOf<PendingDangerousCommand?>(null)
        private set

    private var commandJob: Job? = null

    val promptUser: String get() = "redis@${session.profile.name}"
    val promptHost: String get() = ":${session.profile.port}[${session.currentDatabase}]>"
    val prompt: String get() = promptUser + promptHost

    val suggestions: List<RedisCommandSuggestion>
        get() = RedisCommandCatalog.suggest(input, MAX_SUGGESTIONS)

    val isReadOnly: Boolean
        get() = settings().globalReadOnly || session.profile.readOnly

    val canExecute: Boolean get() = input.isNotBlank() && !isRunning

    init {
        val recent = settings().recentCommands.takeLast(MAX_HISTORY)
        history.addAll(recent)
        recent.asReversed().forEach { command ->
            historyItems +=
                ConsoleHistoryItem(
                    command = command,
                    at = "--:--",
                    durationMillis = null,
                    summary = "历史",
                )
        }
        entries += ConsoleEntry.Note(welcomeText())
    }

    fun updateInput(value: String) {
        input = value
        historyCursor = -1
        suggestionVisible =
            value.isNotBlank() &&
            !value.endsWith(" ") &&
            RedisCommandCatalog.suggest(value, MAX_SUGGESTIONS).isNotEmpty()
    }

    fun refreshSuggestions() {
        suggestionVisible =
            input.isNotBlank() &&
            !input.endsWith(" ") &&
            suggestions.isNotEmpty()
    }

    fun dismissSuggestions() {
        suggestionVisible = false
    }

    fun acceptSuggestion(command: String) {
        input = if (command.endsWith(" ")) command else "$command "
        suggestionVisible = false
        historyCursor = -1
    }

    fun execute(raw: String) {
        if (isRunning) {
            onToast("上一条命令仍在执行", ToastKind.Info)
            return
        }
        if (pendingDangerous != null) {
            onToast("请先确认或取消待执行的危险命令", ToastKind.Warning)
            return
        }
        val command = raw.trim()
        if (command.isEmpty()) return
        val tokens = RedisCommandParser.split(command)
        if (tokens == null) {
            appendError("命令解析失败：引号未闭合")
            return
        }
        if (tokens.isEmpty()) return

        val at = now()
        input = ""
        suggestionVisible = false
        historyCursor = -1

        val safety = WriteCommandGuard.classify(command)
        if (isReadOnly && safety != CommandSafety.READ) {
            val first = tokens.first().uppercase()
            val message =
                if (safety == CommandSafety.UNKNOWN) {
                    "未知命令在只读模式下被拦截：$first"
                } else {
                    "只读模式已开启，写命令被拦截：$first"
                }
            appendError(message)
            addHistoryItem(
                ConsoleHistoryItem(
                    command = command,
                    at = at,
                    durationMillis = null,
                    summary = "blocked",
                    failed = true,
                ),
            )
            return
        }
        if (isReadOnly && tokens.first().equals("PUBLISH", ignoreCase = true)) {
            append(ConsoleEntry.Note("只读模式允许 PUBLISH，但消息仍会发布到对应频道。"))
        }
        if (settings().confirmDangerousCommands && DangerousCommands.isDangerous(tokens)) {
            pendingDangerous = PendingDangerousCommand(command = command, tokens = tokens, at = at)
            return
        }
        runCommand(command = command, tokens = tokens, at = at)
    }

    /** Confirms the parked dangerous command and sends it to Redis. */
    fun confirmDangerous() {
        val pending = pendingDangerous ?: return
        pendingDangerous = null
        if (isReadOnly) {
            appendError("只读模式已开启，写命令被拦截：${pending.tokens.first().uppercase()}")
            addHistoryItem(
                ConsoleHistoryItem(
                    command = pending.command,
                    at = pending.at,
                    durationMillis = null,
                    summary = "blocked",
                    failed = true,
                ),
            )
            return
        }
        runCommand(command = pending.command, tokens = pending.tokens, at = pending.at)
    }

    fun dismissDangerous() {
        pendingDangerous?.let { input = it.command }
        pendingDangerous = null
    }

    private fun runCommand(
        command: String,
        tokens: List<String>,
        at: String,
    ) {
        append(ConsoleEntry.Command(at = at, text = command, promptHost = promptHost))
        addHistory(command)
        isRunning = true
        val started = TimeSource.Monotonic.markNow()
        commandJob =
            consoleScope.launch {
                try {
                    val reply = session.gateway.execute(tokens)
                    val duration = started.elapsedNow().inWholeMilliseconds
                    if (reply is RedisReply.Error) {
                        appendError(reply.message)
                    } else {
                        append(ConsoleEntry.Output(reply = reply, durationMillis = duration))
                        applySelectedDatabase(tokens)
                    }
                    addHistoryItem(
                        ConsoleHistoryItem(
                            command = command,
                            at = at,
                            durationMillis = duration,
                            summary = if (reply is RedisReply.Error) "error" else ConsoleOutput.summary(reply),
                            failed = reply is RedisReply.Error,
                        ),
                    )
                    onRecordCommand(command)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    val duration = started.elapsedNow().inWholeMilliseconds
                    val message = error.firstLine()
                    appendError("连接失败：$message")
                    addHistoryItem(
                        ConsoleHistoryItem(
                            command = command,
                            at = at,
                            durationMillis = duration,
                            summary = "连接失败",
                            failed = true,
                        ),
                    )
                    onToast("命令执行失败：$message", ToastKind.Danger)
                } finally {
                    isRunning = false
                    commandJob = null
                }
            }
    }

    fun fillFromHistory(direction: Int) {
        if (history.isEmpty()) return
        val next =
            when {
                direction < 0 ->
                    if (historyCursor == -1) {
                        history.lastIndex
                    } else {
                        (historyCursor - 1).coerceAtLeast(0)
                    }
                direction > 0 ->
                    if (historyCursor == -1) {
                        -1
                    } else {
                        historyCursor + 1
                    }
                else -> historyCursor
            }
        if (next >= history.size || next < 0) {
            historyCursor = -1
            input = ""
        } else {
            historyCursor = next
            input = history[next]
        }
        suggestionVisible = false
    }

    fun fillCommand(command: String) {
        input = command
        historyCursor = -1
        suggestionVisible = false
    }

    fun clear() {
        entries.clear()
        entries += ConsoleEntry.Note(welcomeText())
    }

    /** Danger-zone action: drops persisted/in-memory command history but keeps the session log. */
    fun clearCache() {
        history.clear()
        historyItems.clear()
        historyCursor = -1
    }

    fun refreshWelcome() {
        val welcome = ConsoleEntry.Note(welcomeText())
        if (entries.firstOrNull() is ConsoleEntry.Note) {
            entries[0] = welcome
        } else {
            entries.add(0, welcome)
        }
    }

    /** Full log as plain text for the copy button. */
    fun plainTextLog(): String =
        entries.joinToString("\n") { entry ->
            when (entry) {
                is ConsoleEntry.Command -> "${entry.at} $promptUser${entry.promptHost.ifEmpty { promptHost }} ${entry.text}"
                is ConsoleEntry.Note -> entry.text
                is ConsoleEntry.Error -> "(error) ${entry.message}"
                is ConsoleEntry.Output -> ConsoleOutput.plainText(entry.reply)
            }
        }

    fun reportLogCopied() {
        onToast("已复制日志", ToastKind.Success)
    }

    fun reportCleared() {
        onToast("会话已清空", ToastKind.Info)
    }

    fun dispose() {
        commandJob?.cancel()
        commandJob = null
        pendingDangerous = null
        isRunning = false
        consoleJob.cancel()
    }

    private fun applySelectedDatabase(tokens: List<String>) {
        if (!tokens.first().equals("SELECT", ignoreCase = true)) return
        val index = tokens.getOrNull(1)?.toIntOrNull() ?: return
        if (index in 0 until RedisSession.DATABASE_COUNT) {
            session.recordDatabaseSelection(index)
            onDatabaseSelected(index)
        }
    }

    private fun addHistory(command: String) {
        if (history.lastOrNull() == command) return
        history += command
        while (history.size > MAX_HISTORY) history.removeAt(0)
    }

    private fun addHistoryItem(item: ConsoleHistoryItem) {
        historyItems.add(0, item)
        while (historyItems.size > MAX_HISTORY) historyItems.removeAt(historyItems.lastIndex)
    }

    private fun append(entry: ConsoleEntry) {
        entries += entry
        while (entries.size > MAX_ENTRIES) {
            entries.removeAt(0)
        }
    }

    private fun appendError(message: String) {
        append(ConsoleEntry.Error(message.trim().ifEmpty { "未知错误" }))
    }

    private fun welcomeText(): String {
        val version = session.summary?.version
        return if (session.isConnected) {
            "Redis Compose shell · connected to ${session.profile.host}:${session.profile.port}" +
                (version?.let { " ($it)" } ?: "") + " · type HELP for commands"
        } else {
            "Redis Compose shell · waiting for ${session.profile.host}:${session.profile.port}"
        }
    }

    private fun now(): String = formatEpochSeconds(currentEpochSeconds())

    private fun Throwable.firstLine(): String =
        message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: this::class.simpleName
            ?: "未知错误"

    private companion object {
        const val MAX_SUGGESTIONS = 6
        const val MAX_HISTORY = 200
        const val MAX_ENTRIES = 500
    }
}
