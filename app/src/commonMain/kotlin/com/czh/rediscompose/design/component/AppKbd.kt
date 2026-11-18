package com.czh.rediscompose.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

@Composable
fun AppKbd(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusChip)
    Box(
        modifier =
            modifier
                .defaultMinSize(minWidth = dimens.kbdHeight, minHeight = dimens.kbdHeight)
                .background(colors.surface3, shape)
                .border(1.dp, colors.borderStrong, shape)
                .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = typography.kbd,
            color = colors.textTertiary,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun AppKbdPreview() {
    AppTheme(dark = true) {
        Row(
            modifier =
                Modifier
                    .background(LocalAppColors.current.background)
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppKbd("⌘")
            AppKbd("K")
            AppKbd("Esc")
        }
    }
}
