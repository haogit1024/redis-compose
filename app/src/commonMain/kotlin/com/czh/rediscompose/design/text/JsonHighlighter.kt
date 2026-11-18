package com.czh.rediscompose.design.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle

/** Token colors used by [JsonHighlighter]; feature code maps them from `AppColors`. */
data class JsonHighlightColors(
    val key: Color,
    val string: Color,
    val number: Color,
    val boolean: Color,
    val punctuation: Color,
)

/**
 * Lightweight JSON tokenizer for the value editor.
 *
 * It never throws: invalid or partial JSON is still colored token by token so typing in the
 * editor does not flash an error state. String escapes, nested objects and Unicode text are
 * handled by scanning characters instead of relying on a parser.
 */
object JsonHighlighter {
    fun highlight(
        text: String,
        colors: JsonHighlightColors,
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        val builder = AnnotatedString.Builder(text)
        var index = 0
        while (index < text.length) {
            val character = text[index]
            when {
                character == '"' -> {
                    val start = index
                    index = stringEnd(text, index)
                    val style = if (nextNonWhitespace(text, index) == ':') colors.key else colors.string
                    builder.addStyle(SpanStyle(color = style), start, index)
                }
                character == '-' || character.isDigit() -> {
                    val start = index
                    index = numberEnd(text, index)
                    if (index > start) {
                        builder.addStyle(SpanStyle(color = colors.number), start, index)
                    } else {
                        index = start + 1
                    }
                }
                character.isLetter() -> {
                    val start = index
                    while (index < text.length && text[index].isLetter()) index += 1
                    val word = text.substring(start, index)
                    if (word == "true" || word == "false" || word == "null") {
                        builder.addStyle(SpanStyle(color = colors.boolean), start, index)
                    }
                }
                character in PUNCTUATION -> {
                    builder.addStyle(SpanStyle(color = colors.punctuation), index, index + 1)
                    index += 1
                }
                else -> index += 1
            }
        }
        return builder.toAnnotatedString()
    }

    /** Returns the index just past the closing quote (or the end for unterminated strings). */
    private fun stringEnd(
        text: String,
        start: Int,
    ): Int {
        var index = start + 1
        while (index < text.length) {
            when (text[index]) {
                '\\' -> index += 2
                '"' -> return index + 1
                else -> index += 1
            }
        }
        return text.length
    }

    private fun numberEnd(
        text: String,
        start: Int,
    ): Int {
        var index = start
        if (index < text.length && text[index] == '-') index += 1
        val integerStart = index
        while (index < text.length && text[index].isDigit()) index += 1
        if (index == integerStart) return start
        if (index < text.length && text[index] == '.') {
            index += 1
            while (index < text.length && text[index].isDigit()) index += 1
        }
        if (index < text.length && (text[index] == 'e' || text[index] == 'E')) {
            val exponentStart = index
            index += 1
            if (index < text.length && (text[index] == '+' || text[index] == '-')) index += 1
            val digitsStart = index
            while (index < text.length && text[index].isDigit()) index += 1
            if (index == digitsStart) index = exponentStart
        }
        return index
    }

    private fun nextNonWhitespace(
        text: String,
        start: Int,
    ): Char? {
        var index = start
        while (index < text.length && text[index].isWhitespace()) index += 1
        return text.getOrNull(index)
    }

    private val PUNCTUATION = charArrayOf('{', '}', '[', ']', ',', ':')
}
