package com.czh.rediscompose.feature.workspace.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.rediscompose.design.text.JsonHighlightColors
import com.czh.rediscompose.design.text.JsonHighlighter
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.design.theme.trackTextInputFocus
import com.czh.rediscompose.util.SettingsLimits

/**
 * Mono code area with a logical-line gutter and optional JSON token coloring.
 *
 * Font size, line numbers and soft wrap come from settings. Turning wrap off
 * measures the longest line (capped) and places the field in a horizontal scroll container
 * so long JSON stays on one line without freezing on very large values.
 */
@Composable
internal fun CodeEditor(
    value: String,
    onValueChange: (String) -> Unit,
    highlighted: Boolean,
    readOnly: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String = "空字符串",
    fontSize: Int = 11,
    showLineNumbers: Boolean = true,
    wordWrap: Boolean = true,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val fontSizeValue = SettingsLimits.editorFontSize(fontSize)
    val lineHeight = (fontSizeValue + 10).sp
    val codeStyle = typography.code.copy(fontSize = fontSizeValue.sp, lineHeight = lineHeight)
    val gutterStyle =
        typography.codeGutter.copy(
            fontSize = (fontSizeValue - 2).coerceAtLeast(8).sp,
            lineHeight = lineHeight,
        )
    val lineCount = value.lineSequence().count().coerceAtLeast(1)
    val lineNumbers = remember(lineCount) { (1..lineCount).joinToString("\n") }
    val jsonColors =
        remember(colors) {
            JsonHighlightColors(
                key = colors.jsonKey,
                string = colors.jsonString,
                number = colors.jsonNumber,
                boolean = colors.jsonBoolean,
                punctuation = colors.jsonPunctuation,
            )
        }
    val transformation =
        remember(highlighted, jsonColors) {
            if (highlighted) JsonVisualTransformation(jsonColors) else VisualTransformation.None
        }
    val measuredTextWidth =
        remember(value, codeStyle) {
            if (wordWrap) {
                0.dp
            } else {
                with(density) {
                    measureLongestLine(measurer, value, codeStyle).toDp()
                }
            }
        }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.codeBackground),
    ) {
        val gutterWidth = if (showLineNumbers) dimens.codeGutterWidth else 0.dp
        val availableTextWidth = (maxWidth - gutterWidth).coerceAtLeast(1.dp)
        val textWidth =
            if (wordWrap) {
                availableTextWidth
            } else {
                maxOf(measuredTextWidth + dimens.codePaddingH * 2, availableTextWidth)
            }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(verticalScroll),
        ) {
            if (showLineNumbers) {
                Text(
                    text = lineNumbers,
                    style = gutterStyle,
                    color = colors.codeLineNumber,
                    textAlign = TextAlign.End,
                    modifier =
                        Modifier
                            .width(dimens.codeGutterWidth)
                            .drawBehind {
                                drawRect(
                                    color = colors.border,
                                    topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                                    size = Size(1.dp.toPx(), size.height),
                                )
                            }
                            .padding(horizontal = dimens.spaceSm, vertical = dimens.codePaddingV),
                )
            }
            Box(Modifier.weight(1f)) {
                Row(
                    modifier =
                        if (wordWrap) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScroll)
                        },
                ) {
                    Box(
                        modifier =
                            Modifier
                                .width(textWidth)
                                .padding(
                                    start = dimens.codePaddingH,
                                    end = dimens.codePaddingH,
                                    top = dimens.codePaddingV,
                                    bottom = dimens.codePaddingV,
                                ),
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = codeStyle,
                                color = colors.textQuaternary,
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier.fillMaxWidth().trackTextInputFocus(),
                            readOnly = readOnly,
                            singleLine = false,
                            textStyle = codeStyle.copy(color = colors.text),
                            cursorBrush = SolidColor(colors.accent),
                            visualTransformation = transformation,
                        )
                    }
                }
            }
        }
    }
}

private fun measureLongestLine(
    measurer: androidx.compose.ui.text.TextMeasurer,
    value: String,
    style: TextStyle,
): Float {
    var longest = ""
    var count = 0
    for (line in value.lineSequence()) {
        if (count++ >= MEASURE_LINE_LIMIT) break
        if (line.length > longest.length) longest = line
    }
    if (longest.isEmpty()) return 0f
    return measurer.measure(longest.take(MEASURE_LINE_CHARS), style).size.width.toFloat()
}

private const val MEASURE_LINE_LIMIT = 2_000
private const val MEASURE_LINE_CHARS = 4_000

private class JsonVisualTransformation(
    private val colors: JsonHighlightColors,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(
            text = JsonHighlighter.highlight(text.text, colors),
            offsetMapping = OffsetMapping.Identity,
        )
}

@Preview(widthDp = 720, heightDp = 320)
@Composable
private fun CodeEditorPreview() {
    AppTheme(dark = true) {
        CodeEditor(
            value =
                """
                {
                  "name": "ada",
                  "age": 36,
                  "active": true,
                  "tags": ["redis", "desktop"]
                }
                """.trimIndent(),
            onValueChange = {},
            highlighted = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 13,
        )
    }
}

@Preview(widthDp = 720, heightDp = 240)
@Composable
private fun CodeEditorNoWrapPreview() {
    AppTheme(dark = false) {
        CodeEditor(
            value = """{"a":[1,2,3,4,5,6,7,8,9,10],"b":"a rather long single-line value for no-wrap mode"}""",
            onValueChange = {},
            highlighted = true,
            readOnly = false,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 12,
            showLineNumbers = false,
            wordWrap = false,
        )
    }
}
