package com.czh.rediscompose.feature.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppTooltip
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

@Composable
fun ConnectionsHeader(
    onlineCount: Int,
    totalCount: Int,
    onNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val titleBlock: @Composable () -> Unit = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = AppIcons.Connections,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(dimens.iconSm),
                    )
                    Text(
                        text = "CONNECTION LIBRARY",
                        style = typography.microLabel,
                        color = colors.accent,
                    )
                }
                Row(
                    modifier = Modifier.padding(top = dimens.spaceXs + 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                ) {
                    Text(
                        text = "连接库",
                        style = typography.pageTitle,
                        color = colors.text,
                    )
                    AppBadge(
                        text = "$onlineCount 在线",
                        tone = if (onlineCount > 0) AppBadgeTone.Success else AppBadgeTone.Plain,
                        icon = AppIcons.Wifi,
                    )
                    Text(
                        text = "$totalCount 个连接",
                        style = typography.monoMeta,
                        color = colors.textQuaternary,
                    )
                }
                Text(
                    text = "集中管理开发、预发与生产环境的 Redis 实例。支持 TLS、SSH 隧道与只读模式。",
                    style = typography.body,
                    color = colors.textTertiary,
                    modifier =
                        Modifier
                            .padding(top = dimens.spaceXs + 2.dp)
                            .widthIn(max = dimens.pageDescriptionMaxWidth),
                )
            }
        }
        val actions: @Composable () -> Unit = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTooltip("暂不支持导入配置") {
                    AppButton(
                        text = "导入配置",
                        onClick = {},
                        size = AppButtonSize.Medium,
                        variant = AppButtonVariant.Secondary,
                        icon = AppIcons.Upload,
                        enabled = false,
                    )
                }
                AppButton(
                    text = "新建连接",
                    onClick = onNew,
                    size = AppButtonSize.Medium,
                    variant = AppButtonVariant.Primary,
                    icon = AppIcons.Add,
                )
            }
        }
        if (maxWidth >= 700.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
            ) {
                Column(Modifier.weight(1f)) { titleBlock() }
                actions()
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceMd)) {
                titleBlock()
                actions()
            }
        }
    }
}
