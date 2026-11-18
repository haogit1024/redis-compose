package com.czh.rediscompose.util

import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FormattersDesktopTest {
    private lateinit var originalZone: TimeZone

    @BeforeTest
    fun setUp() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterTest
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    @Test
    fun `epoch seconds use local wall clock time`() {
        assertEquals("00:00:00", formatEpochSeconds(0))
        assertEquals("01:01:01", formatEpochSeconds(3_661))
    }
}
