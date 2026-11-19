package com.czh.rediscompose.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppEmptyState
import com.czh.rediscompose.design.component.AppKbd
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.key.KeyValue
import com.czh.rediscompose.feature.workspace.editor.HashEditor
import com.czh.rediscompose.feature.workspace.editor.ListEditor
import com.czh.rediscompose.feature.workspace.editor.SetEditor
import com.czh.rediscompose.feature.workspace.editor.SortedSetEditor
import com.czh.rediscompose.feature.workspace.editor.StreamEditor
import com.czh.rediscompose.feature.workspace.editor.StringEditor
import com.czh.rediscompose.util.ShortcutLabels

/** Right-hand value inspector: header, metadata and the type-specific editor. */
@Composable
internal fun ValuePane(
    state: WorkspaceState,
    onCopyKey: (String) -> Unit,
    onCopyCommand: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val selectedKey = state.selectedKey

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (selectedKey == null) {
            ValueEmptyState()
            return@Box
        }
        Column(
            modifier =
                Modifier
                    .padding(
                        start = dimens.valuePaddingSide,
                        end = dimens.valuePaddingSide,
                        top = dimens.valuePaddingTop,
                        bottom = dimens.valuePaddingBottom,
                    )
                    .widthIn(max = dimens.valueMaxWidth)
                    .fillMaxWidth()
                    .fillMaxHeight(),
        ) {
            ValueHeader(
                state = state,
                detail = state.keyDetail,
                loading = state.detailLoading,
                onCopyKey = onCopyKey,
                onCopyCommand = onCopyCommand,
            )
            state.keyDetail?.let { detail ->
                ValueMetaGrid(
                    detail = detail,
                    modifier = Modifier.padding(top = dimens.spaceLg),
                )
            }
            if (state.isReadOnly) {
                ReadOnlyBanner(modifier = Modifier.padding(top = dimens.spaceMd))
            }
            if (state.detailError != null && state.keyDetail != null) {
                ValueInlineError(
                    state = state,
                    modifier = Modifier.padding(top = dimens.spaceMd),
                )
            }

            val editorModifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = dimens.spaceLg)
            val detail = state.keyDetail
            when {
                detail != null ->
                    when (val value = detail.value) {
                        is KeyValue.StringValue -> StringEditor(state, value, editorModifier)
                        is KeyValue.HashValue -> HashEditor(state, value, editorModifier)
                        is KeyValue.ListValue -> ListEditor(state, value, editorModifier)
                        is KeyValue.SetValue -> SetEditor(state, value, editorModifier)
                        is KeyValue.SortedSetValue -> SortedSetEditor(state, value, editorModifier)
                        is KeyValue.StreamValue -> StreamEditor(state, value, editorModifier)
                    }
                state.detailError != null ->
                    Box(editorModifier, contentAlignment = Alignment.Center) {
                        AppEmptyState(
                            icon = AppIcons.ErrorCircle,
                            title = "读取值失败",
                            description = state.detailError,
                            action = {
                                AppButton(
                                    text = "重试",
                                    onClick = state::refreshSelected,
                                    variant = AppButtonVariant.Primary,
                                    size = AppButtonSize.Small,
                                    icon = AppIcons.Refresh,
                                )
                            },
                        )
                    }
                else -> ValueLoadingState(editorModifier)
            }

            EditorFootnote(
                readOnly = state.isReadOnly,
                modifier = Modifier.padding(top = dimens.spaceMd),
            )
        }
    }
}

@Composable
private fun ReadOnlyBanner(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusXs)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.warning.copy(alpha = 0.10f), shape)
                .padding(horizontal = dimens.spaceSm + 2.dp, vertical = dimens.spaceXs + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Icon(
            imageVector = AppIcons.Shield,
            contentDescription = null,
            tint = colors.warning,
            modifier = Modifier.size(dimens.iconSm),
        )
        Text(
            text = "只读模式 · 写操作已禁用",
            style = typography.caption,
            color = colors.warning,
        )
    }
}

@Composable
private fun ValueInlineError(
    state: WorkspaceState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusXs)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.danger.copy(alpha = 0.10f), shape)
                .padding(horizontal = dimens.spaceSm + 2.dp, vertical = dimens.spaceXs + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Icon(
            imageVector = AppIcons.Warning,
            contentDescription = null,
            tint = colors.danger,
            modifier = Modifier.size(dimens.iconSm),
        )
        Text(
            text = state.detailError.orEmpty(),
            style = typography.caption,
            color = colors.danger,
            modifier = Modifier.weight(1f),
        )
        AppButton(
            text = "重试",
            onClick = state::refreshSelected,
            variant = AppButtonVariant.Ghost,
            size = AppButtonSize.Small,
            icon = AppIcons.Refresh,
        )
    }
}

@Composable
private fun EditorFootnote(
    readOnly: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.editorFootnoteGap),
    ) {
        Icon(
            imageVector = AppIcons.Info,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(dimens.iconXs),
        )
        AppKbd(ShortcutLabels.commandKey("S"))
        Text(text = "保存修改", style = typography.monoMicro, color = colors.textTertiary)
        Text(text = "·", style = typography.monoMicro, color = colors.textQuaternary)
        AppKbd(ShortcutLabels.commandKey("F"))
        Text(text = "搜索 Key", style = typography.monoMicro, color = colors.textTertiary)
        Text(text = "·", style = typography.monoMicro, color = colors.textQuaternary)
        Text(
            text = if (readOnly) "只读会话：只能查看" else "写操作会立即写入 Redis",
            style = typography.monoMicro,
            color = colors.textTertiary,
            maxLines = 1,
        )
    }
}

@Preview(widthDp = 820, heightDp = 620)
@Composable
private fun ValuePanePreview() {
    AppTheme(dark = true) {
        val state = remember { previewWorkspaceState().also { it.selectKey("cache:user:1001:profile") } }
        ValuePane(
            state = state,
            onCopyKey = {},
            onCopyCommand = {},
            modifier = Modifier.background(LocalAppColors.current.background),
        )
    }
}
