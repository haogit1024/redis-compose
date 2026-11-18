package com.czh.rediscompose.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    contentPadding: Dp? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusLg)
    Column(
        modifier =
            modifier
                .background(colors.surface, shape)
                .border(1.dp, colors.border, shape),
    ) {
        if (title != null || actions != null) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.spaceLg - 1.dp, vertical = dimens.spaceMd + 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            ) {
                Column(Modifier.weight(1f)) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = typography.cardTitle,
                            color = colors.text,
                        )
                    }
                    if (description != null) {
                        Text(
                            text = description,
                            style = typography.tooltip,
                            color = colors.textTertiary,
                        )
                    }
                }
                if (actions != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
                        content = actions,
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border),
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding ?: dimens.cardPadding),
            content = content,
        )
    }
}

@Preview
@Composable
private fun AppCardPreview() {
    AppTheme(dark = true) {
        AppCard(
            modifier =
                Modifier
                    .background(LocalAppColors.current.background)
                    .padding(16.dp)
                    .width(380.dp),
            title = "服务器健康",
            description = "最近的采样结果",
            actions = {
                AppIconButton(com.czh.rediscompose.design.icon.AppIcons.Refresh, "刷新", {})
            },
        ) {
            Text(
                text = "内存 1.24 GB / 2 GB",
                style = LocalAppTypography.current.body,
                color = LocalAppColors.current.textSecondary,
            )
        }
    }
}
