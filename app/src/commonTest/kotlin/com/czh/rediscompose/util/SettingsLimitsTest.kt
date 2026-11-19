package com.czh.rediscompose.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsLimitsTest {
    @Test
    fun `numeric limits clamp to their documented ranges`() {
        assertEquals(11, SettingsLimits.editorFontSize(3))
        assertEquals(18, SettingsLimits.editorFontSize(40))
        assertEquals(13, SettingsLimits.editorFontSize(13))

        assertEquals(100, SettingsLimits.scanBatchSize(1))
        assertEquals(10_000, SettingsLimits.scanBatchSize(50_000))
        assertEquals(1_000, SettingsLimits.scanBatchSize(1_000))

        assertEquals(1, SettingsLimits.truncateValueKb(0))
        assertEquals(1_024, SettingsLimits.truncateValueKb(9_999))
        assertEquals(10, SettingsLimits.truncateValueKb(10))
    }

    @Test
    fun `key separator keeps a short value and falls back to colon`() {
        assertEquals(":", SettingsLimits.keySeparator(""))
        assertEquals("|", SettingsLimits.keySeparator("|"))
        assertEquals("::", SettingsLimits.keySeparator("::"))
        assertEquals("abc", SettingsLimits.keySeparator("abcdef"))
    }

    @Test
    fun `sensitive field detection follows the documented pattern`() {
        assertTrue(SensitiveFields.isSensitive("password"))
        assertTrue(SensitiveFields.isSensitive("user_password"))
        assertTrue(SensitiveFields.isSensitive("API_KEY"))
        assertTrue(SensitiveFields.isSensitive("api-key"))
        assertTrue(SensitiveFields.isSensitive("authToken"))
        assertTrue(SensitiveFields.isSensitive("private_seed"))
        assertFalse(SensitiveFields.isSensitive("username"))
        assertFalse(SensitiveFields.isSensitive("auth_header"))
        assertEquals("••••••", SensitiveFields.maskedOrPlain("secret", masked = true))
        assertEquals("secret", SensitiveFields.maskedOrPlain("secret", masked = false))
    }

    @Test
    fun `dangerous commands include flush config set and scripts`() {
        assertTrue(DangerousCommands.isDangerous("FLUSHALL"))
        assertTrue(DangerousCommands.isDangerous("flushdb"))
        assertTrue(DangerousCommands.isDangerous("CONFIG SET maxmemory 1gb"))
        assertTrue(DangerousCommands.isDangerous("script flush"))
        assertTrue(DangerousCommands.isDangerous("EVAL \"return 1\" 0"))
        assertFalse(DangerousCommands.isDangerous("CONFIG GET maxmemory"))
        assertFalse(DangerousCommands.isDangerous("GET user:1"))
        assertFalse(DangerousCommands.isDangerous(""))
        assertFalse(DangerousCommands.isDangerous("\"unclosed"))
    }
}
