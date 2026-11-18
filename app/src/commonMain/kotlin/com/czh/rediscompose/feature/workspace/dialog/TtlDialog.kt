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
import com.czh.rediscompose.util.formatTtl

/** Set or remove an expiry; `-1` maps to PERSIST. */
@Composable
internal fun TtlDialog(state: WorkspaceState) {
    val key = state.ttlDialogKey ?: return
    val currentTtl = state.keyDetail?.summary?.ttlSeconds ?: state.ttlOf(key) ?: -1L
    var input by remember(key) {
        mutableStateOf(if (currentTtl > 0) currentTtl.toString() else "")
    }
    var error by remember(key) { mutableStateOf<String?>(null) }
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current

    AppDialog(
        visible = true,
        onDismissRequest = state::dismissTtlDialog,
        title = "修改 TTL",
        subtitle = key,
        footer = {
            AppButton(
                text = "取消",
                onClick = state::dismissTtlDialog,
                variant = AppButtonVariant.Ghost,
                size = AppButtonSize.Small,
            )
            AppButton(
                text = "保存",
                onClick = {
                    val seconds = input.trim().toLongOrNull()
                    if (seconds == null) {
                        error = "请输入整数秒数"
                    } else if (seconds == 0L || seconds < -1L) {
                        error = "TTL 必须为 -1 或正整数"
                    } else {
                        state.updateTtl(seconds)
                    }
                },
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Small,
                icon = AppIcons.Clock,
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
            placeholder = "秒数；-1 表示永久",
            leadingIcon = AppIcons.Clock,
            mono = true,
            isError = error != null,
        )
        Text(
            text = "当前 TTL：${formatTtl(currentTtl)}",
            style = typography.caption,
            color = colors.textTertiary,
            modifier = Modifier.padding(top = dimens.spaceSm),
        )
        Text(
            text = "输入 -1 将移除过期时间（PERSIST），输入正整数秒数将执行 EXPIRE。",
            style = typography.monoMicro,
            color = colors.textQuaternary,
            modifier = Modifier.padding(top = dimens.spaceXs),
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

@Preview(widthDp = 560, heightDp = 420)
@Composable
private fun TtlDialogPreview() {
    AppTheme(dark = true) {
        val state =
            remember {
                previewWorkspaceState().also {
                    it.selectKey("cache:user:1001:profile")
                    it.openTtlDialog()
                }
            }
        TtlDialog(state = state)
    }
}
