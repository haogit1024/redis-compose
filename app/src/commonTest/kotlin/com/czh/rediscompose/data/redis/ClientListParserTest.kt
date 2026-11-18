package com.czh.rediscompose.data.redis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientListParserTest {
    @Test
    fun `parses fields in any order including empty names`() {
        val raw =
            """
            id=3 addr=127.0.0.1:5000 laddr=127.0.0.1:6379 fd=8 name= age=0 idle=12 flags=N db=2 sub=0 psub=0
            id=4 addr=10.0.0.2:6000 name=worker age=100 idle=0 db=0
            """.trimIndent()
        val clients = ClientListParser.parse(raw)
        assertEquals(2, clients.size)
        assertEquals("3", clients[0].id)
        assertEquals("127.0.0.1:5000", clients[0].address)
        assertEquals("", clients[0].name)
        assertEquals(2, clients[0].database)
        assertEquals(12, clients[0].idleSeconds)
        assertEquals("worker", clients[1].name)
        assertEquals(0, clients[1].database)
    }

    @Test
    fun `ignores blank and malformed lines`() {
        val clients =
            ClientListParser.parse(
                """
                   

                name=missing-id
                id=9 addr=1.2.3.4:1 name=x db=1 idle=5
                """.trimIndent(),
            )
        assertEquals(1, clients.size)
        assertEquals("9", clients.single().id)
    }

    @Test
    fun `empty input returns empty list`() {
        assertTrue(ClientListParser.parse("").isEmpty())
    }
}
