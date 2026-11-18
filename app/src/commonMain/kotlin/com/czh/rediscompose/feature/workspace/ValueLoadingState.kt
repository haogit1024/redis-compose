package com.czh.rediscompose.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.czh.rediscompose.design.component.AppLoading
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens

/** Header stays visible; meta boxes and the editor area show lightweight skeletons. */
@Composable
internal fun ValueLoadingState(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val boxShape = RoundedCornerShape(dimens.metaBoxRadius)
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.metaGridGap),
        ) {
            repeat(4) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(dimens.valueEmptyGlyph - dimens.spaceSm)
                            .background(colors.surface2, boxShape),
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(colors.surface, RoundedCornerShape(dimens.editorCardRadius)),
            contentAlignment = Alignment.Center,
        ) {
            AppLoading(label = "正在读取值…")
        }
    }
}

@Preview(widthDp = 760, heightDp = 460)
@Composable
private fun ValueLoadingStatePreview() {
    AppTheme(dark = true) {
        ValueLoadingState(
            Modifier
                .background(LocalAppColors.current.background)
                .padding(LocalAppDimens.current.space2Xl),
        )
    }
}
