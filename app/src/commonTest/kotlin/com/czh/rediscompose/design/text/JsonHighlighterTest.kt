package com.czh.rediscompose.design.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonHighlighterTest {
    private val colors =
        JsonHighlightColors(
            key = Color(0xFF000001),
            string = Color(0xFF000002),
            number = Color(0xFF000003),
            boolean = Color(0xFF000004),
            punctuation = Color(0xFF000005),
        )

    private fun colored(
        text: String,
        color: Color,
    ): List<String> =
        JsonHighlighter.highlight(text, colors).spanStyles
            .filter { it.item.color == color }
            .map { span -> text.substring(span.start, span.end) }

    @Test
    fun `highlights object keys strings numbers and literals`() {
        val text = """{"name":"Ada","age":36,"active":true,"score":1.5e2,"none":null}"""
        val result = JsonHighlighter.highlight(text, colors)

        assertEquals(text, result.text)
        assertEquals(listOf("\"name\"", "\"age\"", "\"active\"", "\"score\"", "\"none\""), colored(text, colors.key))
        assertEquals(listOf("\"Ada\""), colored(text, colors.string))
        assertEquals(listOf("36", "1.5e2"), colored(text, colors.number))
        assertEquals(listOf("true", "null"), colored(text, colors.boolean))
        assertTrue(colored(text, colors.punctuation).contains("{"))
    }

    @Test
    fun `escaped quotes inside strings do not end the token`() {
        val text = """{"quote":"a\"b","slash":"c\\d"}"""
        val strings = colored(text, colors.string)

        assertEquals(listOf("\"a\\\"b\"", "\"c\\\\d\""), strings)
    }

    @Test
    fun `nested arrays keep their own tokens`() {
        val text = """{"tags":["a","b"],"nested":{"n":1}}"""
        val keys = colored(text, colors.key)

        assertEquals(listOf("\"tags\"", "\"nested\"", "\"n\""), keys)
        assertEquals(listOf("\"a\"", "\"b\""), colored(text, colors.string))
        assertEquals(listOf("1"), colored(text, colors.number))
    }

    @Test
    fun `invalid json does not throw and keeps the text`() {
        val text = """{oops: "unterminated"""
        val result = JsonHighlighter.highlight(text, colors)

        assertEquals(text, result.text)
        assertEquals(listOf("\"unterminated"), colored(text, colors.string))
    }

    @Test
    fun `unicode content is preserved`() {
        val text = """{"城市":"上海","emoji":"🚀"}"""
        val result = JsonHighlighter.highlight(text, colors)

        assertEquals(text, result.text)
        assertTrue(colored(text, colors.key).contains("\"城市\""))
        assertTrue(colored(text, colors.string).contains("\"上海\""))
        assertTrue(colored(text, colors.string).contains("\"🚀\""))
    }

    @Test
    fun `empty text returns empty annotated string`() {
        assertEquals(AnnotatedString(""), JsonHighlighter.highlight("", colors))
    }
}
