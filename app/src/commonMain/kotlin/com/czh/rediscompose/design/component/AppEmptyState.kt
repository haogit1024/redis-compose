package com.czh.rediscompose.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

@Composable
fun AppEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val iconShape = RoundedCornerShape(dimens.radiusLg)
    Column(
        modifier = modifier.padding(horizontal = dimens.spaceXl, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(dimens.emptyIconSize)
                    .background(colors.accentSoft, iconShape)
                    .border(1.dp, colors.accent.copy(alpha = 0.22f), iconShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(dimens.iconXl),
            )
        }
        Text(
            text = title,
            style = typography.bodyStrong,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = dimens.spaceMd + 1.dp),
        )
        if (description != null) {
            Text(
                text = description,
                style = typography.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .padding(top = 3.dp)
                        .widthIn(max = 300.dp),
            )
        }
        if (action != null) {
            Box(Modifier.padding(top = dimens.spaceMd)) {
                action()
            }
        }
    }
}

@Preview
@Composable
private fun AppEmptyStatePreview() {
    AppTheme(dark = true) {
        Box(
            Modifier
                .background(LocalAppColors.current.background)
                .padding(16.dp),
        ) {
            AppEmptyState(
                icon = AppIcons.Key,
                title = "这个数据库还没有 Key",
                description = "连接后可在这里浏览、搜索和编辑键值。也可以先新建一个 Key。",
                action = {
                    AppButton("新建 Key", {}, variant = AppButtonVariant.Primary, icon = AppIcons.Add)
                },
            )
        }
    }
}
