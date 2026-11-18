package com.czh.rediscompose.design.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.czh.rediscompose.resources.Res
import com.czh.rediscompose.resources.inter_medium
import com.czh.rediscompose.resources.inter_regular
import com.czh.rediscompose.resources.inter_semibold
import com.czh.rediscompose.resources.jetbrains_mono_medium
import com.czh.rediscompose.resources.jetbrains_mono_regular
import org.jetbrains.compose.resources.Font

/**
 * Type scale from `design-tokens.md` §2.
 *
 * Inter (400/500/600) is the UI family and JetBrains Mono (400/500) is used for keys,
 * values and code. Both are bundled in `composeResources/font`, so no network access is
 * required at runtime.
 */
data class AppTypography(
    val uiFamily: FontFamily,
    val monoFamily: FontFamily,
    val display: TextStyle,
    val pageTitle: TextStyle,
    val keyTitle: TextStyle,
    val sectionTitle: TextStyle,
    val cardTitle: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val bodySmall: TextStyle,
    val caption: TextStyle,
    val microLabel: TextStyle,
    val monoValue: TextStyle,
    val monoKeyRow: TextStyle,
    val monoMeta: TextStyle,
    val monoMicro: TextStyle,
    val monoBody: TextStyle,
    val code: TextStyle,
    val codeGutter: TextStyle,
    val metaValue: TextStyle,
    val metaLabel: TextStyle,
    val collectionIndex: TextStyle,
    val button: TextStyle,
    val buttonSmall: TextStyle,
    val input: TextStyle,
    val badge: TextStyle,
    val typeChip: TextStyle,
    val filterChip: TextStyle,
    val kbd: TextStyle,
    val segment: TextStyle,
    val tooltip: TextStyle,
)

@Composable
fun rememberAppTypography(): AppTypography {
    val uiFamily =
        FontFamily(
            Font(Res.font.inter_regular, FontWeight.W400),
            Font(Res.font.inter_medium, FontWeight.W500),
            Font(Res.font.inter_semibold, FontWeight.W600),
        )
    val monoFamily =
        FontFamily(
            Font(Res.font.jetbrains_mono_regular, FontWeight.W400),
            Font(Res.font.jetbrains_mono_medium, FontWeight.W500),
        )
    return remember(uiFamily, monoFamily) {
        AppTypography(
            uiFamily = uiFamily,
            monoFamily = monoFamily,
            display =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight(650),
                    fontSize = 28.sp,
                    letterSpacing = (-0.9).sp,
                    lineHeight = 34.sp,
                ),
            pageTitle =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight(650),
                    fontSize = 22.sp,
                    letterSpacing = (-0.7).sp,
                    lineHeight = 27.sp,
                ),
            keyTitle =
                TextStyle(
                    fontFamily = monoFamily,
                    fontWeight = FontWeight(550),
                    fontSize = 18.sp,
                    letterSpacing = (-0.4).sp,
                    lineHeight = 24.sp,
                ),
            sectionTitle =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight(600),
                    fontSize = 14.sp,
                    letterSpacing = (-0.15).sp,
                    lineHeight = 20.sp,
                ),
            cardTitle =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight.W600,
                    fontSize = 12.sp,
                    letterSpacing = (-0.06).sp,
                    lineHeight = 16.sp,
                ),
            body =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight(450),
                    fontSize = 13.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 19.sp,
                ),
            bodyStrong =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight(550),
                    fontSize = 13.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 19.sp,
                ),
            bodySmall =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight(450),
                    fontSize = 11.5.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 16.sp,
                ),
            caption =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight(450),
                    fontSize = 11.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 16.sp,
                ),
            microLabel =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight(600),
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    lineHeight = 12.sp,
                ),
            monoValue =
                TextStyle(
                    fontFamily = monoFamily,
                    fontWeight = FontWeight.W400,
                    fontSize = 11.sp,
                    letterSpacing = (-0.1).sp,
                    lineHeight = 17.sp,
                ),
            monoMeta =
                TextStyle(
                    fontFamily = monoFamily,
                    fontWeight = FontWeight.W400,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 14.sp,
                ),
            monoMicro =
                TextStyle(
                    fontFamily = monoFamily,
                    fontWeight = FontWeight.W400,
                    fontSize = 8.5.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 11.sp,
                ),
            monoKeyRow =
                TextStyle(
                    fontFamily = monoFamily,
                    fontWeight = FontWeight.W400,
                    fontSize = 10.5.sp,
                    letterSpacing = (-0.21).sp,
                    lineHeight = 14.sp,
                ),
            monoBody =
                TextStyle(
                    fontFamily = monoFamily,
                    fontWeight = FontWeight.W400,
                    fontSize = 11.5.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 18.sp,
                ),
            code =
                TextStyle(
                    fontFamily = monoFamily,
                    fontWeight = FontWeight.W400,
                    fontSize = 11.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 21.sp,
                ),
            codeGutter =
                TextStyle(
                    fontFamily = monoFamily,
                    fontWeight = FontWeight.W400,
                    fontSize = 9.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.End,
                ),
            metaValue =
                TextStyle(
                    fontFamily = monoFamily,
                    fontWeight = FontWeight.W500,
                    fontSize = 12.sp,
                    letterSpacing = (-0.1).sp,
                    lineHeight = 16.sp,
                ),
            metaLabel =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight.W600,
                    fontSize = 8.5.sp,
                    letterSpacing = 0.68.sp,
                    lineHeight = 11.sp,
                ),
            collectionIndex =
                TextStyle(
                    fontFamily = monoFamily,
                    fontWeight = FontWeight.W400,
                    fontSize = 9.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 12.sp,
                ),
            button =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight.W500,
                    fontSize = 12.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 16.sp,
                ),
            buttonSmall =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight.W500,
                    fontSize = 11.5.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 15.sp,
                ),
            input =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight(450),
                    fontSize = 12.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 18.sp,
                ),
            badge =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight(550),
                    fontSize = 9.5.sp,
                    letterSpacing = 0.2.sp,
                    lineHeight = 12.sp,
                ),
            typeChip =
                TextStyle(
                    fontFamily = monoFamily,
                    fontWeight = FontWeight(600),
                    fontSize = 9.sp,
                    letterSpacing = 0.54.sp,
                    lineHeight = 12.sp,
                ),
            filterChip =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight(550),
                    fontSize = 9.sp,
                    letterSpacing = 0.18.sp,
                    lineHeight = 12.sp,
                ),
            kbd =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight.W500,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 12.sp,
                ),
            segment =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight.W500,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 14.sp,
                ),
            tooltip =
                TextStyle(
                    fontFamily = uiFamily,
                    fontWeight = FontWeight(450),
                    fontSize = 10.sp,
                    letterSpacing = 0.sp,
                    lineHeight = 13.sp,
                ),
        )
    }
}
