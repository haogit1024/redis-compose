package com.czh.rediscompose.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens

/** Left key browser column: header, scrollable tree and SCAN footer. */
@Composable
internal fun KeyListPane(
    state: WorkspaceState,
    width: Dp,
    searchFocusRequester: FocusRequester,
    onSearchFocusChanged: (Boolean) -> Unit,
    onCopyKey: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Column(
        modifier =
            modifier
                .width(width)
                .fillMaxHeight()
                .background(colors.surface.copy(alpha = 0.40f)),
    ) {
        KeyPaneHeader(
            state = state,
            searchFocusRequester = searchFocusRequester,
            onSearchFocusChanged = onSearchFocusChanged,
        )
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        ) {
            KeyTree(state = state, onCopyKey = onCopyKey)
        }
        KeyPaneFooter(state = state)
    }
}

@Preview(widthDp = 320, heightDp = 640)
@Composable
private fun KeyListPanePreview() {
    AppTheme(dark = true) {
        val dimens = LocalAppDimens.current
        val state = remember { previewWorkspaceState() }
        KeyListPane(
            state = state,
            width = dimens.keyPaneWidth,
            searchFocusRequester = remember { FocusRequester() },
            onSearchFocusChanged = {},
            onCopyKey = {},
        )
    }
}
