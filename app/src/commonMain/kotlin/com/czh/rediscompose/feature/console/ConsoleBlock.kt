package com.czh.rediscompose.feature.console

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.command.RedisReply

/** Renders one command, note, error or recursive reply block in the terminal. */
@Composable
internal fun ConsoleEntryBlock(
    entry: ConsoleEntry,
    promptUser: String,
    promptHost: String,
    modifier: Modifier = Modifier,
) {
    when (entry) {
        is ConsoleEntry.Command ->
            ConsoleCommandBlock(
                entry = entry,
                promptUser = promptUser,
                promptHost = promptHost,
                modifier = modifier,
            )
        is ConsoleEntry.Note ->
            Text(
                text = entry.text,
                style = LocalAppTypography.current.terminalSmall,
                color = LocalAppColors.current.terminalTextDim,
                modifier = modifier.fillMaxWidth(),
            )
        is ConsoleEntry.Error ->
            SelectionContainer {
                Text(
                    text = "(error) ${entry.message}",
                    style = LocalAppTypography.current.terminal,
                    color = LocalAppColors.current.terminalError,
                    modifier = modifier.padding(start = LocalAppDimens.current.spaceMd),
                )
            }
        is ConsoleEntry.Output ->
            ConsoleOutputBlock(entry = entry, modifier = modifier)
    }
}

@Composable
private fun ConsoleCommandBlock(
    entry: ConsoleEntry.Command,
    promptUser: String,
    promptHost: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    SelectionContainer {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 2.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = entry.at,
                style = typography.terminalSmall,
                color = colors.terminalTextDim,
            )
            Row {
                Text(
                    text = promptUser,
                    style = typography.terminal,
                    color = colors.terminalPrompt,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = dimens.consolePromptMaxWidth),
                )
                Text(
                    text = entry.promptHost.ifEmpty { promptHost },
                    style = typography.terminal,
                    color = colors.terminalTextDim,
                )
            }
            Text(
                text = entry.text,
                style = typography.terminal,
                color = colors.terminalText,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ConsoleOutputBlock(
    entry: ConsoleEntry.Output,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val view = remember(entry) { ConsoleOutput.view(entry.reply) }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = view.headline,
                style = typography.terminalSmall,
                color = colors.terminalTextDim,
            )
            Text(
                text = "· ${durationLabel(entry.durationMillis)}",
                style = typography.terminalSmall,
                color = colors.terminalTextDim,
            )
        }
        SelectionContainer {
            Column {
                view.lines.forEach { line ->
                    Text(
                        text = line.text.ifEmpty { " " },
                        style = typography.terminal,
                        color = toneColor(line.tone, colors),
                        modifier =
                            Modifier.padding(
                                start = dimens.spaceLg * line.indent,
                            ),
                    )
                }
            }
        }
    }
}

private fun toneColor(
    tone: ConsoleOutputTone,
    colors: com.czh.rediscompose.design.theme.AppColors,
): Color =
    when (tone) {
        ConsoleOutputTone.Normal -> colors.terminalTextSecondary
        ConsoleOutputTone.Success -> colors.terminalSuccess
        ConsoleOutputTone.Dim -> colors.terminalTextDim
        ConsoleOutputTone.Error -> colors.terminalError
    }

private fun durationLabel(millis: Long): String = if (millis <= 0L) "<1 ms" else "$millis ms"

@Preview(widthDp = 760, heightDp = 420)
@Composable
private fun ConsoleEntryBlockPreview() {
    AppTheme(dark = true) {
        val dimensions = LocalAppDimens.current
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(dimensions.spaceLg),
            verticalArrangement = Arrangement.spacedBy(dimensions.spaceSm),
        ) {
            ConsoleEntryBlock(
                entry = ConsoleEntry.Command("07:41:12", "HGETALL cache:user:1001:profile"),
                promptUser = "redis@Local",
                promptHost = ":6379[0]>",
            )
            ConsoleEntryBlock(
                entry = ConsoleEntry.Note("Redis Compose shell · connected to 127.0.0.1:6379 (8.6.2)"),
                promptUser = "",
                promptHost = "",
            )
            ConsoleEntryBlock(
                entry = ConsoleEntry.Output(RedisReply.Integer(8), 0),
                promptUser = "",
                promptHost = "",
            )
            ConsoleEntryBlock(
                entry = ConsoleEntry.Error("ERR unknown command 'FOO'"),
                promptUser = "",
                promptHost = "",
            )
        }
    }
}
