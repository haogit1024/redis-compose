package com.czh.rediscompose.util

/**
 * Splits a one-line Redis command into arguments, honoring single/double quotes and
 * backslash escapes inside double quotes.
 *
 * Returns null when a quote is left open so the console can show a parse error instead of
 * sending a truncated command.
 */
object RedisCommandParser {
    fun split(raw: String): List<String>? {
        val result = mutableListOf<String>()
        var index = 0
        while (index < raw.length) {
            while (index < raw.length && raw[index].isWhitespace()) index += 1
            if (index >= raw.length) break
            val token = StringBuilder()
            var quote: Char? = null
            while (index < raw.length) {
                val character = raw[index]
                when {
                    quote == null && character.isWhitespace() -> break
                    quote == null && (character == '"' || character == '\'') -> {
                        quote = character
                        index += 1
                    }
                    quote != null && character == quote -> {
                        quote = null
                        index += 1
                    }
                    quote == '"' && character == '\\' && index + 1 < raw.length -> {
                        val escaped = raw[index + 1]
                        token.append(
                            when (escaped) {
                                'n' -> '\n'
                                't' -> '\t'
                                'r' -> '\r'
                                'b' -> '\b'
                                else -> escaped
                            },
                        )
                        index += 2
                    }
                    else -> {
                        token.append(character)
                        index += 1
                    }
                }
            }
            if (quote != null) return null
            result += token.toString()
            while (index < raw.length && raw[index].isWhitespace()) index += 1
        }
        return result
    }
}
