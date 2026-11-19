package com.czh.rediscompose.feature.console

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.app.SessionStatus
import com.czh.rediscompose.design.component.AppTooltip
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

private val QUICK_COMMANDS =
    listOf(
        "INFO memory",
        "DBSIZE",
        "SLOWLOG GET 10",
        "SCAN 0 COUNT 100",
        "MEMORY DOCTOR",
    )

/** Command console page: terminal, quick commands, composer and history rail. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConsoleScreen(
    console: ConsoleState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val clipboard = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    var helpVisible by remember { mutableStateOf(false) }

    LaunchedEffect(console) { focusRequester.requestFocus() }
    LaunchedEffect(helpVisible) {
        if (!helpVisible) focusRequester.requestFocus()
    }
    LaunchedEffect(console.session.status, console.session.summary?.version) {
        if (console.session.status == SessionStatus.CONNECTED) console.refreshWelcome()
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background),
    ) {
        val showHistory = maxWidth >= 720.dp
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(
                            start = dimens.consolePadding,
                            end = dimens.consolePadding,
                            top = dimens.consolePaddingTop,
                        ),
            ) {
                ConsoleToolbar(console = console, onHelp = { helpVisible = true })
                TerminalSurface(
                    console = console,
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = dimens.spaceMd),
                    onCopy = {
                        clipboard.setText(AnnotatedString(console.plainTextLog()))
                        console.reportLogCopied()
                    },
                    onClear = {
                        console.clear()
                        console.reportCleared()
                    },
                )
                QuickCommands(
                    onFill = { command ->
                        console.fillCommand(command)
                        focusRequester.requestFocus()
                    },
                    modifier = Modifier.padding(top = dimens.spaceSm),
                )
                CommandComposer(
                    console = console,
                    focusRequester = focusRequester,
                    modifier = Modifier.padding(top = dimens.spaceSm, bottom = dimens.spaceMd),
                )
            }
            if (showHistory) {
                ConsoleHistoryPane(
                    console = console,
                    onFillCommand = { command ->
                        console.fillCommand(command)
                        focusRequester.requestFocus()
                    },
                )
            }
        }
    }

    ConsoleHelpDialog(
        visible = helpVisible,
        readOnly = console.isReadOnly,
        onDismiss = { helpVisible = false },
    )
    ConsoleConfirmDialog(console = console)
}

@Composable
private fun TerminalSurface(
    console: ConsoleState,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val shape = RoundedCornerShape(dimens.terminalRadius)
    Column(
        modifier =
            modifier
                .shadow(elevation = dimens.terminalShadow, shape = shape)
                .clip(shape)
                .background(colors.terminalBackground)
                .border(1.dp, colors.terminalBorder, shape),
    ) {
        TerminalBar(
            console = console,
            onCopy = onCopy,
            onClear = onClear,
        )
        ConsoleLog(
            console = console,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        )
    }
}

@Composable
private fun TerminalBar(
    console: ConsoleState,
    onCopy: () -> Unit,
    onClear: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(dimens.terminalBarHeight)
                .background(colors.terminalChrome)
                .drawBehind {
                    drawRect(
                        color = colors.terminalBorder,
                        topLeft = Offset(0f, size.height - 1.dp.toPx()),
                        size = Size(size.width, 1.dp.toPx()),
                    )
                }
                .padding(horizontal = dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm + 1.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp)) {
                TerminalLight(colors.terminalError)
                TerminalLight(colors.terminalWarning)
                TerminalLight(colors.terminalSuccess)
            }
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
            ) {
                Text(
                    text = "redis-cli — ",
                    style = typography.terminalSmall,
                    color = colors.terminalTextDim,
                    maxLines = 1,
                )
                Text(
                    text = console.session.profile.name,
                    style = typography.terminalSmall,
                    color = colors.terminalText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                TerminalDbBadge(databaseIndex = console.session.currentDatabase)
            }
        }
        TerminalIconButton(
            icon = AppIcons.Copy,
            contentDescription = "复制全部日志",
            tooltip = "复制全部",
            onClick = onCopy,
        )
        TerminalIconButton(
            icon = AppIcons.Delete,
            contentDescription = "清空会话",
            tooltip = "清空会话",
            onClick = onClear,
        )
    }
}

@Composable
private fun TerminalLight(color: Color) {
    Box(
        Modifier
            .size(LocalAppDimens.current.terminalLightSize)
            .background(color.copy(alpha = 0.75f), CircleShape),
    )
}

@Composable
private fun TerminalDbBadge(databaseIndex: Int) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusChip)
    Box(
        modifier =
            Modifier
                .height(dimens.terminalDbBadgeHeight)
                .background(colors.terminalBackground, shape)
                .border(1.dp, colors.terminalBorder, shape)
                .padding(horizontal = dimens.iconTextGap),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "db$databaseIndex",
            style = typography.monoMicro,
            color = colors.terminalTextDim,
            maxLines = 1,
        )
    }
}

@Composable
private fun TerminalIconButton(
    icon: ImageVector,
    contentDescription: String,
    tooltip: String,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    AppTooltip(tooltip) {
        Box(
            modifier =
                Modifier
                    .size(dimens.iconButtonSizeSmall)
                    .clip(RoundedCornerShape(dimens.radiusXs))
                    .background(if (hovered) Color.White.copy(alpha = 0.06f) else Color.Transparent)
                    .hoverable(interaction)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                    .pointerHoverIcon(PointerIcon.Hand),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (hovered) colors.terminalText else colors.terminalTextDim,
                modifier = Modifier.size(dimens.iconXs),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickCommands(
    onFill: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Text(
            text = "常用",
            style = LocalAppTypography.current.microLabel,
            color = colors.textQuaternary,
            modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 4.dp),
        )
        QUICK_COMMANDS.forEach { command ->
            QuickChip(
                text = command,
                onClick = { onFill(command) },
            )
        }
    }
}

@Composable
private fun QuickChip(
    text: String,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(dimens.radiusChip)
    Box(
        modifier =
            Modifier
                .height(dimens.quickChipHeight)
                .background(if (hovered) colors.surface3 else colors.surface2, shape)
                .border(1.dp, if (hovered) colors.borderStrong else colors.border, shape)
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = dimens.filterChipPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = typography.terminalSmall,
            color = if (hovered) colors.text else colors.textTertiary,
            maxLines = 1,
        )
    }
}

@Preview(widthDp = 1440, heightDp = 900)
@Composable
private fun ConsoleScreenPreview() {
    AppTheme(dark = true) {
        ConsoleScreen(console = remember { previewConsoleState() })
    }
}

@Preview(widthDp = 1440, heightDp = 900)
@Composable
private fun ConsoleScreenLightPreview() {
    AppTheme(dark = false) {
        ConsoleScreen(console = remember { previewConsoleState() })
    }
}
