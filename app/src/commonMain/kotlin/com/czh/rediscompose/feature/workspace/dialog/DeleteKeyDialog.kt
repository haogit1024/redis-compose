package com.czh.rediscompose.feature.workspace.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppDialog
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.feature.workspace.WorkspaceState
import com.czh.rediscompose.feature.workspace.previewWorkspaceState

/** Confirm deletion of the selected key; the confirm button uses the danger variant. */
@Composable
internal fun DeleteKeyDialog(state: WorkspaceState) {
    val key = state.deleteDialogKey ?: return
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusXs)

    AppDialog(
        visible = true,
        onDismissRequest = state::dismissDeleteDialog,
        title = "删除 Key",
        subtitle = "此操作不可撤销",
        footer = {
            AppButton(
                text = "取消",
                onClick = state::dismissDeleteDialog,
                variant = AppButtonVariant.Ghost,
                size = AppButtonSize.Small,
            )
            AppButton(
                text = "删除",
                onClick = state::deleteSelectedKey,
                variant = AppButtonVariant.Danger,
                size = AppButtonSize.Small,
                icon = AppIcons.Delete,
                loading = state.isSaving,
            )
        },
    ) {
        Text(
            text = "确定删除 Key「$key」？",
            style = typography.bodyStrong,
            color = colors.text,
        )
        Text(
            text = key,
            style = typography.monoKeyRow,
            color = colors.danger,
            modifier =
                Modifier
                    .padding(top = dimens.spaceSm)
                    .background(colors.danger.copy(alpha = 0.10f), shape)
                    .padding(horizontal = dimens.spaceSm, vertical = dimens.spaceXs + 2.dp),
        )
        Text(
            text = "该 Key 的值、TTL 与置顶状态会立即从当前列表移除。",
            style = typography.caption,
            color = colors.textTertiary,
            modifier = Modifier.padding(top = dimens.spaceSm),
        )
    }
}

@Preview(widthDp = 560, heightDp = 380)
@Composable
private fun DeleteKeyDialogPreview() {
    AppTheme(dark = true) {
        val state =
            remember {
                previewWorkspaceState().also {
                    it.selectKey("temp:otp:13800001111")
                    it.openDeleteDialog()
                }
            }
        DeleteKeyDialog(state = state)
    }
}
