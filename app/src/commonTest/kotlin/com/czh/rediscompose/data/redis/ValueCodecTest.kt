package com.czh.rediscompose.data.redis

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValueCodecTest {
    @Test
    fun `decode keeps valid utf8`() {
        assertEquals("héllo 世界", ValueCodec.decode("héllo 世界".encodeToByteArray()))
    }

    @Test
    fun `decode rejects invalid utf8`() {
        assertNull(ValueCodec.decode(byteArrayOf(0xFF.toByte())))
    }

    @Test
    fun `decode rejects overlong utf8 sequences`() {
        assertNull(ValueCodec.decode(byteArrayOf(0xC0.toByte(), 0x80.toByte())))
    }

    @Test
    fun `hex prints uppercase space separated bytes`() {
        val bytes = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        assertEquals("DE AD BE EF", ValueCodec.hex(bytes))
    }

    @Test
    fun `hex of empty payload is empty`() {
        assertEquals("", ValueCodec.hex(ByteArray(0)))
    }

    @Test
    fun `hexToBytes accepts spaced and prefixed input`() {
        val expected = byteArrayOf(0xDE.toByte(), 0xAD.toByte())
        assertContentEquals(expected, ValueCodec.hexToBytes("DE AD"))
        assertContentEquals(expected, ValueCodec.hexToBytes("dead"))
        assertContentEquals(expected, ValueCodec.hexToBytes("0xDEAD"))
        assertContentEquals(expected, ValueCodec.hexToBytes("  de  ad  "))
    }

    @Test
    fun `hexToBytes rejects odd and invalid input`() {
        assertNull(ValueCodec.hexToBytes("ABC"))
        assertNull(ValueCodec.hexToBytes("ZZ"))
        assertNull(ValueCodec.hexToBytes("0x"))
    }

    @Test
    fun `isJson only accepts objects and arrays`() {
        assertTrue(ValueCodec.isJson("""{"user":"1001"}"""))
        assertTrue(ValueCodec.isJson("""[1,2,3]"""))
        assertFalse(ValueCodec.isJson("plain text"))
        assertFalse(ValueCodec.isJson("123"))
        assertFalse(ValueCodec.isJson("""{"broken": }"""))
    }

    @Test
    fun `prettyJson formats valid json and rejects invalid`() {
        val pretty = ValueCodec.prettyJson("""{"a":1,"b":[2,3]}""")
        assertTrue(pretty != null && '\n' in pretty)
        assertTrue(pretty!!.contains("\"a\": 1"))
        assertNull(ValueCodec.prettyJson("not json"))
    }
}
