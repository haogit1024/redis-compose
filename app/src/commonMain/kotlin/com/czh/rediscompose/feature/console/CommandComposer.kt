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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.design.theme.trackTextInputFocus
import com.czh.rediscompose.util.RedisCommandSuggestion
import com.czh.rediscompose.util.ShortcutLabels

/** Composer with prompt, run button, history keys and the autocomplete popup. */
@Composable
internal fun CommandComposer(
    console: ConsoleState,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(dimens.terminalComposerRadius)
    val suggestions = console.suggestions
    val showSuggestions = console.suggestionVisible && suggestions.isNotEmpty()

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val panelWidth = minOf(maxWidth, 560.dp)
        Box(Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = dimens.consoleComposerHeight)
                        .drawBehind {
                            if (focused) {
                                val glow = dimens.fieldGlow.toPx()
                                drawRoundRect(
                                    color = colors.accentSofter,
                                    topLeft = Offset(-glow, -glow),
                                    size = Size(size.width + glow * 2, size.height + glow * 2),
                                    cornerRadius = CornerRadius(dimens.terminalComposerRadius.toPx() + glow),
                                )
                            }
                        }
                        .clip(shape)
                        .background(colors.surface)
                        .border(1.dp, if (focused) colors.accentLine else colors.borderStrong, shape)
                        .padding(horizontal = dimens.fieldPadding, vertical = dimens.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            ) {
                Row {
                    Text(
                        text = console.promptUser,
                        style = typography.monoBody,
                        color = colors.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = dimens.consolePromptMaxWidth),
                    )
                    Text(
                        text = console.promptHost,
                        style = typography.monoBody,
                        color = colors.textQuaternary,
                        maxLines = 1,
                    )
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (console.input.isEmpty()) {
                        Text(
                            text = "输入 Redis 命令，Enter 执行，↑↓ 浏览历史",
                            style = typography.monoBody,
                            color = colors.textQuaternary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = console.input,
                        onValueChange = console::updateInput,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .trackTextInputFocus()
                                .onFocusChanged { state ->
                                    focused = state.isFocused
                                    if (state.isFocused) {
                                        console.refreshSuggestions()
                                    } else {
                                        console.dismissSuggestions()
                                    }
                                }
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    val command = event.isMetaPressed || event.isCtrlPressed
                                    when {
                                        command && (event.key == Key.Enter || event.key == Key.NumPadEnter) -> {
                                            console.execute(console.input)
                                            true
                                        }
                                        event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                                            console.execute(console.input)
                                            true
                                        }
                                        event.key == Key.Tab -> {
                                            val first = suggestions.firstOrNull()
                                            if (first != null) {
                                                console.acceptSuggestion(first.command)
                                                focusRequester.requestFocus()
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                        event.key == Key.DirectionUp && !showSuggestions -> {
                                            console.fillFromHistory(-1)
                                            true
                                        }
                                        event.key == Key.DirectionDown && !showSuggestions -> {
                                            console.fillFromHistory(1)
                                            true
                                        }
                                        event.key == Key.Escape && showSuggestions -> {
                                            console.dismissSuggestions()
                                            true
                                        }
                                        else -> false
                                    }
                                },
                        singleLine = true,
                        textStyle = typography.monoBody.copy(color = colors.text),
                        cursorBrush = SolidColor(colors.accent),
                    )
                }
                Text(
                    text = "${ShortcutLabels.commandEnter()} 执行",
                    style = typography.terminalSmall,
                    color = colors.textQuaternary,
                    maxLines = 1,
                )
                AppButton(
                    text = "运行",
                    onClick = { console.execute(console.input) },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Small,
                    icon = AppIcons.Run,
                    enabled = console.input.isNotBlank() && !console.isRunning,
                    loading = console.isRunning,
                )
            }
            if (showSuggestions) {
                SuggestionPopup(
                    console = console,
                    suggestions = suggestions,
                    width = panelWidth,
                    onPick = {
                        console.acceptSuggestion(it.command)
                        focusRequester.requestFocus()
                    },
                    onDismiss = console::dismissSuggestions,
                )
            }
        }
    }
}

@Composable
private fun SuggestionPopup(
    console: ConsoleState,
    suggestions: List<RedisCommandSuggestion>,
    width: Dp,
    onPick: (RedisCommandSuggestion) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val density = LocalDensity.current
    val gap = with(density) { 6.dp.roundToPx() }
    val margin = with(density) { 8.dp.roundToPx() }
    val shape = RoundedCornerShape(dimens.radiusMd)

    Popup(
        popupPositionProvider =
            remember(gap, margin) {
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize,
                    ): IntOffset {
                        val x =
                            anchorBounds.left
                                .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                        val y = (anchorBounds.top - popupContentSize.height - gap).coerceAtLeast(margin)
                        return IntOffset(x, y)
                    }
                }
            },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(min = 320.dp, max = width)
                    .shadow(elevation = 12.dp, shape = shape)
                    .background(colors.surface3, shape)
                    .border(1.dp, colors.borderStrong, shape)
                    .padding(dimens.menuPadding),
        ) {
            Text(
                text = "命令建议",
                style = typography.microLabel,
                color = colors.textQuaternary,
                modifier = Modifier.padding(horizontal = dimens.spaceSm, vertical = dimens.spaceXs),
            )
            suggestions.forEach { suggestion ->
                SuggestionRow(
                    suggestion = suggestion,
                    onPick = {
                        onPick(suggestion)
                    },
                )
            }
            Text(
                text = "Tab 补全 · Enter 执行 · 模式：${if (console.isReadOnly) "只读" else "可写"}",
                style = typography.monoMicro,
                color = colors.textQuaternary,
                modifier = Modifier.padding(horizontal = dimens.spaceSm, vertical = dimens.spaceXs),
            )
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: RedisCommandSuggestion,
    onPick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = dimens.menuItemHeight)
                .clip(RoundedCornerShape(dimens.radiusXs))
                .background(if (hovered) colors.accentSoft else androidx.compose.ui.graphics.Color.Transparent)
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onPick,
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Text(
            text = suggestion.command,
            style = typography.monoValue,
            color = if (hovered) colors.text else colors.textSecondary,
            maxLines = 1,
        )
        Text(
            text = suggestion.syntax,
            style = typography.monoMicro,
            color = colors.textQuaternary,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(widthDp = 760, heightDp = 320)
@Composable
private fun CommandComposerPreview() {
    AppTheme(dark = true) {
        val console =
            remember {
                previewConsoleState().also { it.updateInput("HG") }
            }
        CommandComposer(
            console = console,
            focusRequester = remember { FocusRequester() },
            modifier = Modifier.padding(LocalAppDimens.current.spaceLg),
        )
    }
}
