package com.czh.redis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.ui.components.AppButton
import com.czh.redis.ui.components.AppTextField
import com.czh.redis.ui.components.ButtonSize
import com.czh.redis.ui.components.ButtonVariant
import kotlinx.coroutines.delay

@Composable
fun ConsolePage(
    state: ConsoleState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.lines.size) {
        if (state.lines.isNotEmpty()) {
            listState.animateScrollToItem(state.lines.lastIndex)
        }
    }
    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    Column(modifier = modifier.fillMaxSize().background(colors.panel)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.panelAlt)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${state.session.connection.name} · db${state.session.currentDb}",
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.weight(1f))
            AppButton(
                text = "清空",
                variant = ButtonVariant.GHOST,
                size = ButtonSize.SMALL,
                onClick = { state.clear() },
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            items(state.lines.size) { index ->
                val line = state.lines[index]
                val lineColor =
                    when (line.kind) {
                        ConsoleLineKind.PROMPT -> colors.primary
                        ConsoleLineKind.INPUT -> colors.text
                        ConsoleLineKind.OUTPUT -> colors.textSecondary
                        ConsoleLineKind.RESULT -> colors.success
                        ConsoleLineKind.WARNING -> colors.warn
                        ConsoleLineKind.ERROR -> colors.error
                    }
                Text(
                    text = line.text,
                    color = lineColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.panelAlt)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${state.session.connection.host}:${state.session.connection.port}>",
                color = colors.primary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.width(8.dp))
            AppTextField(
                value = state.input,
                onValueChange = { state.input = it },
                placeholder = "输入命令，Enter 执行，↑↓ 查看历史",
                mono = true,
                height = 30.dp,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                onPreviewKeyEvent = { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.Enter -> {
                                state.execute()
                                true
                            }
                            Key.DirectionUp -> {
                                state.historyUp()
                                true
                            }
                            Key.DirectionDown -> {
                                state.historyDown()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
            )
        }
    }
}
