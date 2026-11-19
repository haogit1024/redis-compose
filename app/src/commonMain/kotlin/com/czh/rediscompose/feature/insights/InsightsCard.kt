package com.czh.rediscompose.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

/** Card shell used by every insights section (design: `.chart-card`). */
@Composable
internal fun InsightsCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.insightsCardRadius)
    Column(
        modifier =
            modifier
                .background(colors.surface, shape)
                .border(1.dp, colors.border, shape),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimens.chartCardPaddingH,
                        end = dimens.chartCardPaddingH,
                        top = dimens.spaceMd,
                        bottom = dimens.spaceXs + 2.dp,
                    ),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = typography.chartTitle,
                    color = colors.text,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = typography.monoMicro,
                        color = colors.textQuaternary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (actions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                    content = actions,
                )
            }
        }
        content()
    }
}

/** Compact inline empty state for cards that have no samples yet. */
@Composable
internal fun InsightsEmptyHint(
    text: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = dimens.space2Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        if (icon != null) {
            Box(
                modifier =
                    Modifier
                        .size(dimens.iconButtonSize)
                        .background(colors.surface3, RoundedCornerShape(dimens.radiusSm)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textQuaternary,
                    modifier = Modifier.size(dimens.iconSm),
                )
            }
        }
        Text(
            text = text,
            style = typography.bodySmall,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            Text(
                text = description,
                style = typography.tooltip,
                color = colors.textQuaternary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
