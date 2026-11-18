package com.czh.rediscompose.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
fun AppLoading(
    modifier: Modifier = Modifier,
    label: String? = null,
    inline: Boolean = false,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    if (inline) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(dimens.icon),
                color = colors.accent,
                strokeWidth = 1.5.dp,
            )
            if (label != null) {
                Text(
                    text = label,
                    style = typography.caption,
                    color = colors.textTertiary,
                )
            }
        }
    } else {
        Box(
            modifier = modifier.padding(dimens.space2Xl),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimens.iconXl),
                    color = colors.accent,
                    strokeWidth = 1.8.dp,
                )
                if (label != null) {
                    Text(
                        text = label,
                        style = typography.body,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppLoadingPreview() {
    AppTheme(dark = true) {
        Column(
            modifier =
                Modifier
                    .background(LocalAppColors.current.background)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppLoading(label = "正在加载 Keyspace…")
            AppLoading(label = "同步中", inline = true)
            AppLoading()
        }
    }
}
