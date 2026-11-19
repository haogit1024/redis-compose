package com.czh.rediscompose.feature.console

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

/** Scrollable terminal log; auto-scroll pauses while the user reads older entries. */
@Composable
internal fun ConsoleLog(
    console: ConsoleState,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    val colors = LocalAppColors.current
    val listState = rememberLazyListState()
    var stickToBottom by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.distinctUntilChanged().collect { scrolling ->
            if (scrolling) {
                val info = listState.layoutInfo
                val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                stickToBottom = info.totalItemsCount == 0 || last >= info.totalItemsCount - 2
            }
        }
    }

    val lastEntry = console.entries.lastOrNull()
    LaunchedEffect(console.entries.size, lastEntry) {
        if (stickToBottom && console.entries.isNotEmpty()) {
            listState.scrollToItem(console.entries.lastIndex)
        }
    }

    LazyColumn(
        modifier =
            modifier
                .background(colors.terminalBackground),
        state = listState,
        contentPadding =
            PaddingValues(
                start = dimens.terminalBodyPaddingH,
                end = dimens.terminalBodyPaddingH,
                top = dimens.terminalBodyPaddingV,
                bottom = dimens.terminalBodyPaddingV + dimens.spaceSm,
            ),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        items(
            count = console.entries.size,
            key = { index -> index },
        ) { index ->
            SelectionContainer {
                ConsoleEntryBlock(
                    entry = console.entries[index],
                    promptUser = console.promptUser,
                    promptHost = console.promptHost,
                )
            }
        }
    }
}

@Preview(widthDp = 760, heightDp = 360)
@Composable
private fun ConsoleLogPreview() {
    AppTheme(dark = true) {
        val console = remember { previewConsoleState() }
        ConsoleLog(
            console = console,
            modifier = Modifier.background(LocalAppColors.current.terminalBackground),
        )
    }
}
