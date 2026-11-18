package com.czh.rediscompose.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppIconButton
import com.czh.rediscompose.design.component.AppIconButtonSize
import com.czh.rediscompose.design.component.AppTooltip
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.settings.KeyView
import com.czh.rediscompose.util.formatCount

/** Title, key count and pane actions above the search field. */
@Composable
internal fun KeyPaneHeader(
    state: WorkspaceState,
    searchFocusRequester: FocusRequester,
    onSearchFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val count = if (state.total > 0) state.total else state.keys.size.toLong()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = dimens.spaceMd,
                    end = dimens.spaceMd,
                    top = dimens.spaceMd,
                    bottom = dimens.spaceSm,
                ),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.spaceXs - 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm - 1.dp),
        ) {
            Text(
                text = "Keys",
                style = typography.cardTitle,
                color = colors.text,
            )
            Text(
                text = formatCount(count),
                style = typography.monoMeta,
                color = colors.textQuaternary,
                maxLines = 1,
            )
            if (state.isReadOnly) {
                AppTooltip("只读模式：写操作被拦截") {
                    Box(
                        modifier = Modifier.size(dimens.iconButtonSizeSmall),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = AppIcons.Lock,
                            contentDescription = "只读模式",
                            tint = colors.warning,
                            modifier = Modifier.size(dimens.iconXs),
                        )
                    }
                }
            }
            Box(Modifier.weight(1f))
            AppTooltip(if (state.canWrite) "新建 Key" else "只读模式，无法新建 Key") {
                AppIconButton(
                    icon = AppIcons.Add,
                    contentDescription = "新建 Key",
                    onClick = state::requestNewKey,
                    size = AppIconButtonSize.Small,
                    enabled = state.canWrite,
                )
            }
            AppTooltip("刷新 Keyspace（R）") {
                AppIconButton(
                    icon = AppIcons.Refresh,
                    contentDescription = "刷新 Keyspace",
                    onClick = state::refresh,
                    size = AppIconButtonSize.Small,
                    enabled = !state.isLoading,
                )
            }
            AppTooltip(if (state.keyView == KeyView.TREE) "切换为平铺视图" else "切换为树形视图") {
                AppIconButton(
                    icon = if (state.keyView == KeyView.TREE) AppIcons.List else AppIcons.Grid,
                    contentDescription = "切换 Key 视图",
                    onClick = state::toggleKeyView,
                    size = AppIconButtonSize.Small,
                )
            }
        }
        KeySearchFilter(
            state = state,
            focusRequester = searchFocusRequester,
            onFocusChanged = onSearchFocusChanged,
        )
    }
}

@Preview(widthDp = 320)
@Composable
private fun KeyPaneHeaderPreview() {
    AppTheme(dark = true) {
        val state = remember { previewWorkspaceState() }
        KeyPaneHeader(
            state = state,
            searchFocusRequester = remember { FocusRequester() },
            onSearchFocusChanged = {},
        )
    }
}

@Preview(widthDp = 320)
@Composable
private fun KeyPaneHeaderLightPreview() {
    AppTheme(dark = false) {
        val state = remember { previewWorkspaceState(readOnly = true) }
        KeyPaneHeader(
            state = state,
            searchFocusRequester = remember { FocusRequester() },
            onSearchFocusChanged = {},
        )
    }
}
