package com.czh.rediscompose.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

enum class AppBadgeTone { Default, Accent, Success, Warning, Danger, Plain }

@Composable
fun AppBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: AppBadgeTone = AppBadgeTone.Default,
    icon: ImageVector? = null,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val contentColor =
        when (tone) {
            AppBadgeTone.Default -> colors.textSecondary
            AppBadgeTone.Accent -> colors.accent
            AppBadgeTone.Success -> colors.success
            AppBadgeTone.Warning -> colors.warning
            AppBadgeTone.Danger -> colors.danger
            AppBadgeTone.Plain -> colors.textSecondary
        }
    val backgroundColor =
        when (tone) {
            AppBadgeTone.Default -> colors.surface3
            AppBadgeTone.Accent -> colors.accentSoft
            AppBadgeTone.Success -> colors.success.copy(alpha = 0.12f)
            AppBadgeTone.Warning -> colors.warning.copy(alpha = 0.12f)
            AppBadgeTone.Danger -> colors.danger.copy(alpha = 0.12f)
            AppBadgeTone.Plain -> Color.Transparent
        }
    val borderColor =
        when (tone) {
            AppBadgeTone.Default, AppBadgeTone.Plain -> colors.border
            AppBadgeTone.Accent -> colors.accent.copy(alpha = 0.22f)
            AppBadgeTone.Success -> colors.success.copy(alpha = 0.22f)
            AppBadgeTone.Warning -> colors.warning.copy(alpha = 0.22f)
            AppBadgeTone.Danger -> colors.danger.copy(alpha = 0.22f)
        }
    val shape = RoundedCornerShape(dimens.radiusChip)
    Row(
        modifier =
            modifier
                .height(dimens.badgeHeight)
                .background(backgroundColor, shape)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(11.dp),
            )
        }
        Text(
            text = text,
            style = typography.badge,
            color = contentColor,
            maxLines = 1,
        )
    }
}

@Composable
fun AppTypeChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusChip)
    Box(
        modifier =
            modifier
                .background(color.copy(alpha = 0.11f), shape)
                .border(1.dp, color.copy(alpha = 0.20f), shape)
                .padding(horizontal = 7.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = typography.typeChip,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
fun AppTypeDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 6.dp,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .drawBehind {
                    drawCircle(
                        color = color.copy(alpha = 0.11f),
                        radius = this.size.minDimension / 2f + 3.dp.toPx(),
                    )
                }
                .background(color, CircleShape),
    )
}

@Preview
@Composable
private fun AppBadgePreview() {
    AppTheme(dark = true) {
        Column(
            modifier =
                Modifier
                    .background(LocalAppColors.current.background)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppBadge("默认")
                AppBadge("JSON", tone = AppBadgeTone.Accent)
                AppBadge("已同步", tone = AppBadgeTone.Success, icon = AppIcons.Check)
                AppBadge("TTL 42s", tone = AppBadgeTone.Warning)
                AppBadge("即将过期", tone = AppBadgeTone.Danger)
                AppBadge("Redis 7.4.2", tone = AppBadgeTone.Plain)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val colors = LocalAppColors.current
                AppTypeChip("STRING", colors.typeString)
                AppTypeChip("HASH", colors.typeHash)
                AppTypeChip("LIST", colors.typeList)
                AppTypeChip("SET", colors.typeSet)
                AppTypeChip("ZSET", colors.typeZset)
                AppTypeChip("STREAM", colors.typeStream)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val colors = LocalAppColors.current
                AppTypeDot(colors.typeString)
                AppTypeDot(colors.typeHash)
                AppTypeDot(colors.typeList)
                AppTypeDot(colors.typeSet)
                AppTypeDot(colors.typeZset)
                AppTypeDot(colors.typeStream)
                AppTypeDot(colors.typeNone)
            }
        }
    }
}
