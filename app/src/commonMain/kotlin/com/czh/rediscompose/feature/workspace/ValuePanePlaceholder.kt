package com.czh.rediscompose.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppLoading
import com.czh.rediscompose.design.component.AppTypeChip
import com.czh.rediscompose.design.icon.BrandMark
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

/**
 * Right pane placeholder for Phase 4.
 *
 * It intentionally looks unfinished: the value inspector and editors arrive in Phase 5,
 * so only the selected key name, type and a loading placeholder are shown.
 */
@Composable
internal fun ValuePanePlaceholder(
    state: WorkspaceState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val selectedKey = state.selectedKey
    LaunchedEffect(selectedKey) {
        if (selectedKey != null) state.ensurePreview(selectedKey)
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background),
    ) {
        if (selectedKey == null) {
            EmptyValuePane(modifier = Modifier.align(Alignment.Center))
        } else {
            SelectedKeyPlaceholder(
                state = state,
                selectedKey = selectedKey,
            )
        }
    }
}

@Composable
private fun EmptyValuePane(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Column(
        modifier = modifier.padding(dimens.space2Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandMark(size = dimens.brandMarkLarge)
        Text(
            text = "从左侧选择一个 Key 查看值",
            style = typography.bodyStrong,
            color = colors.text,
            modifier = Modifier.padding(top = dimens.spaceLg),
        )
        Text(
            text = "Key 列表来自真实 SCAN；值编辑器将在 Phase 5 实现。",
            style = typography.caption,
            color = colors.textTertiary,
            modifier = Modifier.padding(top = dimens.spaceXs),
        )
    }
}

@Composable
private fun SelectedKeyPlaceholder(
    state: WorkspaceState,
    selectedKey: String,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(dimens.space2Xl),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        ) {
            Text(
                text = selectedKey,
                style = typography.keyTitle,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            val type = state.typeOf(selectedKey)
            AppTypeChip(
                text = type?.displayLabel()?.uppercase() ?: "…",
                color = type?.displayColor(colors) ?: colors.typeNone,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spaceMd)
                    .height(1.dp)
                    .background(colors.border),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppLoading(label = "正在读取值…", inline = true)
            Text(
                text = "占位：值编辑器将在 Phase 5 实现",
                style = typography.caption,
                color = colors.textQuaternary,
                modifier = Modifier.padding(top = dimens.spaceSm),
            )
        }
    }
}

@Preview(widthDp = 720, heightDp = 640)
@Composable
private fun ValuePanePlaceholderPreview() {
    AppTheme(dark = true) {
        val state = remember { previewWorkspaceState().also { it.clearSelection() } }
        ValuePanePlaceholder(state = state)
    }
}

@Preview(widthDp = 720, heightDp = 640)
@Composable
private fun ValuePanePlaceholderSelectedPreview() {
    AppTheme(dark = false) {
        val state =
            remember {
                previewWorkspaceState().also { it.selectKey("cache:user:1001:profile") }
            }
        ValuePanePlaceholder(state = state)
    }
}
