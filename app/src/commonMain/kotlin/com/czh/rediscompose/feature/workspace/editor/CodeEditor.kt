package com.czh.rediscompose.feature.workspace.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.text.JsonHighlightColors
import com.czh.rediscompose.design.text.JsonHighlighter
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

/**
 * Mono code area with a logical-line gutter and optional JSON token coloring.
 *
 * The gutter and text share one vertical scroll container; wrapped visual lines are not
 * counted separately, which matches the Phase 5 acceptance for large values.
 */
@Composable
internal fun CodeEditor(
    value: String,
    onValueChange: (String) -> Unit,
    highlighted: Boolean,
    readOnly: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String = "空字符串",
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val scrollState = rememberScrollState()
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

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.codeBackground),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
        ) {
            Text(
                text = lineNumbers,
                style = typography.codeGutter,
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
            Box(
                modifier =
                    Modifier
                        .weight(1f)
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
                        style = typography.code,
                        color = colors.textQuaternary,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = readOnly,
                    singleLine = false,
                    textStyle = typography.code.copy(color = colors.text),
                    cursorBrush = SolidColor(colors.accent),
                    visualTransformation = transformation,
                )
            }
        }
    }
}

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
        )
    }
}
