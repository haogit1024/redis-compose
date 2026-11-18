package com.czh.rediscompose.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.util.formatCount

/** Bottom SCAN status: database, progress and loaded/total counts. */
@Composable
internal fun KeyPaneFooter(
    state: WorkspaceState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val loaded = state.keys.size.toLong()
    val fraction =
        when {
            state.loadedAll -> 1f
            state.total > 0 -> (loaded.toFloat() / state.total).coerceIn(0f, 1f)
            else -> 0f
        }
    val statusText =
        when {
            state.loadError != null -> "加载失败"
            state.loadedAll -> "已加载全部 ${formatCount(loaded)} 个 Key"
            state.total > 0 -> "${formatCount(loaded)} / ${formatCount(state.total)}"
            else -> "已加载 ${formatCount(loaded)}"
        }
    val statusColor = if (state.loadError != null) colors.danger else colors.textQuaternary
    val barShape = RoundedCornerShape(dimens.scanBarHeight)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.surface.copy(alpha = 0.55f))
                .drawBehind {
                    drawRect(
                        color = colors.border,
                        topLeft = Offset.Zero,
                        size = Size(size.width, 1.dp.toPx()),
                    )
                }
                .padding(horizontal = dimens.spaceMd, vertical = dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.iconTextGap),
    ) {
        Icon(
            imageVector = AppIcons.Layers,
            contentDescription = null,
            tint = colors.textQuaternary,
            modifier = Modifier.size(dimens.treeSectionIconSize),
        )
        Text(
            text = "db${state.databaseIndex}",
            style = typography.monoMicro,
            color = colors.textQuaternary,
            maxLines = 1,
        )
        if (state.total > 0) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(dimens.scanBarHeight)
                        .background(colors.surface3, barShape),
            ) {
                if (fraction > 0f) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(dimens.scanBarHeight)
                                .background(
                                    Brush.horizontalGradient(listOf(colors.accent, colors.success)),
                                    barShape,
                                ),
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        Text(
            text = statusText,
            style = typography.monoMicro,
            color = statusColor,
            maxLines = 1,
        )
    }
}

@Preview(widthDp = 320)
@Composable
private fun KeyPaneFooterPreview() {
    AppTheme(dark = true) {
        val state = remember { previewWorkspaceState() }
        KeyPaneFooter(state = state)
    }
}

@Preview(widthDp = 320)
@Composable
private fun KeyPaneFooterLoadingPreview() {
    AppTheme(dark = false) {
        val state = remember { previewWorkspaceState(paused = true) }
        KeyPaneFooter(state = state)
    }
}
