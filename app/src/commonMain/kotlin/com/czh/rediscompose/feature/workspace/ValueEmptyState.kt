package com.czh.rediscompose.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppKbd
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.util.ShortcutLabels

/** Right pane when no key is selected. */
@Composable
internal fun ValueEmptyState(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val glyphShape = RoundedCornerShape(dimens.valueEmptyGlyphRadius)
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = dimens.valueEmptyMaxWidth)
                    .padding(dimens.space3Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(dimens.valueEmptyGlyph)
                        .background(colors.surface2, glyphShape)
                        .border(1.dp, colors.border, glyphShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Key,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(dimens.valueEmptyIcon),
                )
            }
            Text(
                text = "从左侧选择一个 Key 查看或编辑值",
                style = typography.sectionTitle,
                color = colors.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = dimens.spaceLg),
            )
            Text(
                text = "Key 列表来自真实 SCAN。选中后可以查看元信息、编辑 6 种类型并在控制台中继续操作。",
                style = typography.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .padding(top = dimens.spaceXs)
                        .widthIn(max = dimens.valueEmptyDescriptionMaxWidth),
            )
            Row(
                modifier = Modifier.padding(top = dimens.spaceLg),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceLg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShortcutHint(keys = ShortcutLabels.commandKey("F"), label = "搜索 Key")
                ShortcutHint(keys = "R", label = "刷新")
            }
        }
    }
}

@Composable
private fun ShortcutHint(
    keys: String,
    label: String,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.spaceXs + 1.dp),
    ) {
        AppKbd(keys)
        Text(text = label, style = typography.caption, color = colors.textTertiary)
    }
}

@Preview(widthDp = 720, heightDp = 560)
@Composable
private fun ValueEmptyStatePreview() {
    AppTheme(dark = true) {
        ValueEmptyState(Modifier.background(LocalAppColors.current.background))
    }
}
