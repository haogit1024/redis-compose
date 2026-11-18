package com.czh.rediscompose.feature.workspace.dialog

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppDialog
import com.czh.rediscompose.design.component.AppTextField
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.feature.workspace.WorkspaceState
import com.czh.rediscompose.feature.workspace.previewWorkspaceState

/** Rename the selected key; an existing destination is rejected instead of overwritten. */
@Composable
internal fun RenameKeyDialog(state: WorkspaceState) {
    val oldName = state.renameDialogKey ?: return
    var input by remember(oldName) { mutableStateOf(oldName) }
    var error by remember(oldName) { mutableStateOf<String?>(null) }
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current

    AppDialog(
        visible = true,
        onDismissRequest = state::dismissRenameDialog,
        title = "重命名 Key",
        subtitle = oldName,
        footer = {
            AppButton(
                text = "取消",
                onClick = state::dismissRenameDialog,
                variant = AppButtonVariant.Ghost,
                size = AppButtonSize.Small,
            )
            AppButton(
                text = "重命名",
                onClick = {
                    val target = input.trim()
                    if (target.isEmpty()) {
                        error = "Key 名不能为空"
                    } else if (target == oldName) {
                        error = "新 Key 名与原名相同"
                    } else {
                        state.renameKey(target)
                    }
                },
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Small,
                icon = AppIcons.Edit,
                loading = state.isSaving,
            )
        },
    ) {
        AppTextField(
            value = input,
            onValueChange = {
                input = it
                error = null
            },
            placeholder = "新的 Key 名",
            leadingIcon = AppIcons.Key,
            mono = true,
            isError = error != null,
        )
        Text(
            text = "RENAME 会保留值、TTL 与置顶状态；如果目标 Key 已存在，本次操作会被拒绝。",
            style = typography.caption,
            color = colors.textTertiary,
            modifier = Modifier.padding(top = dimens.spaceSm),
        )
        if (error != null) {
            Text(
                text = error.orEmpty(),
                style = typography.caption,
                color = colors.danger,
                modifier = Modifier.padding(top = dimens.spaceXs),
            )
        }
    }
}

@Preview(widthDp = 560, heightDp = 400)
@Composable
private fun RenameKeyDialogPreview() {
    AppTheme(dark = true) {
        val state =
            remember {
                previewWorkspaceState().also {
                    it.selectKey("cache:user:1001:profile")
                    it.openRenameDialog()
                }
            }
        RenameKeyDialog(state = state)
    }
}
