package com.czh.rediscompose.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.app.SessionStatus
import com.czh.rediscompose.design.component.horizontalResizeCursor
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import kotlinx.coroutines.delay

/**
 * Workspace key browser: resizable key column plus the Phase 4 value placeholder.
 *
 * All Redis work lives in [WorkspaceState]; this screen only handles layout, focus and
 * keyboard shortcuts (⌘F, Esc, R, N).
 */
@Composable
fun WorkspaceScreen(
    workspace: WorkspaceState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val rootFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    var searchFocused by remember { mutableStateOf(false) }
    var paneWidth by rememberSaveable(workspace.session.profile.id) {
        mutableFloatStateOf(dimens.keyPaneWidth.value)
    }

    LaunchedEffect(workspace) { rootFocusRequester.requestFocus() }

    LaunchedEffect(workspace, workspace.session.status) {
        if (workspace.session.status == SessionStatus.CONNECTED) workspace.start()
    }

    LaunchedEffect(workspace, workspace.searchInput) {
        val query = workspace.searchInput
        delay(SEARCH_DEBOUNCE_MILLIS)
        workspace.applySearch(query)
    }

    val onCopyKey: (String) -> Unit = { key ->
        clipboardManager.setText(AnnotatedString(key))
        workspace.reportKeyCopied()
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background)
                .focusRequester(rootFocusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val command = event.isMetaPressed || event.isCtrlPressed
                    val plain = !command && !event.isAltPressed && !event.isShiftPressed
                    when {
                        event.key == Key.F && command -> {
                            searchFocusRequester.requestFocus()
                            true
                        }
                        event.key == Key.Escape -> {
                            when {
                                workspace.searchInput.isNotEmpty() -> {
                                    workspace.clearSearch()
                                    true
                                }
                                workspace.selectedKey != null -> {
                                    workspace.clearSelection()
                                    true
                                }
                                searchFocused -> {
                                    focusManager.clearFocus()
                                    true
                                }
                                else -> false
                            }
                        }
                        event.key == Key.R && plain && !searchFocused -> {
                            workspace.refresh()
                            true
                        }
                        event.key == Key.N && plain && !searchFocused -> {
                            workspace.requestNewKey()
                            true
                        }
                        else -> false
                    }
                },
    ) {
        val maxForPane =
            (maxWidth - dimens.keyPaneValueMin - dimens.splitterWidth)
                .coerceAtLeast(dimens.keyPaneMin)
        val paneMax = minOf(dimens.keyPaneMax, maxForPane)
        val clampedWidth = paneWidth.coerceIn(dimens.keyPaneMin.value, paneMax.value).dp

        Row(Modifier.fillMaxSize()) {
            KeyListPane(
                state = workspace,
                width = clampedWidth,
                searchFocusRequester = searchFocusRequester,
                onSearchFocusChanged = { searchFocused = it },
                onCopyKey = onCopyKey,
            )
            PaneSplitter(
                onDrag = { deltaPx ->
                    paneWidth =
                        (paneWidth + deltaPx / density.density)
                            .coerceIn(dimens.keyPaneMin.value, paneMax.value)
                },
            )
            ValuePanePlaceholder(
                state = workspace,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun PaneSplitter(onDrag: (Float) -> Unit) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier =
            Modifier
                .width(dimens.splitterWidth)
                .fillMaxHeight()
                .hoverable(interaction)
                .pointerHoverIcon(horizontalResizeCursor())
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                }
                .drawBehind {
                    drawRect(
                        color = if (hovered) colors.borderStrong else colors.border,
                        topLeft = Offset((size.width - 1.dp.toPx()) / 2f, 0f),
                        size = Size(1.dp.toPx(), size.height),
                    )
                },
    )
}

private const val SEARCH_DEBOUNCE_MILLIS = 300L

@Preview(widthDp = 1120, heightDp = 720)
@Composable
private fun WorkspaceScreenPreview() {
    AppTheme(dark = true) {
        val workspace = remember { previewWorkspaceState() }
        WorkspaceScreen(workspace = workspace)
    }
}

@Preview(widthDp = 1120, heightDp = 720)
@Composable
private fun WorkspaceScreenLightPreview() {
    AppTheme(dark = false) {
        val workspace = remember { previewWorkspaceState() }
        WorkspaceScreen(workspace = workspace)
    }
}
