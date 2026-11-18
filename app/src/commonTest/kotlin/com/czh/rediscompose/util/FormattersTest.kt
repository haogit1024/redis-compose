package com.czh.rediscompose.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FormattersTest {
    @Test
    fun `ttl distinguishes permanent expired and soon`() {
        assertEquals("永久", formatTtl(-1))
        assertEquals("已过期", formatTtl(-2))
        assertEquals("即将过期", formatTtl(0))
    }

    @Test
    fun `ttl uses the right unit boundaries`() {
        assertEquals("1s", formatTtl(1))
        assertEquals("42s", formatTtl(42))
        assertEquals("59s", formatTtl(59))
        assertEquals("1m 0s", formatTtl(60))
        assertEquals("1m 30s", formatTtl(90))
        assertEquals("28m 14s", formatTtl(1694))
        assertEquals("1h 0m", formatTtl(3600))
        assertEquals("1h 1m", formatTtl(3660))
        assertEquals("1d 0h", formatTtl(86400))
        assertEquals("2d 3h", formatTtl(2 * 86400L + 3 * 3600L))
    }

    @Test
    fun `compact ttl keeps one unit for key rows`() {
        assertEquals("永久", formatTtlCompact(-1))
        assertEquals("已过期", formatTtlCompact(-2))
        assertEquals("0s", formatTtlCompact(0))
        assertEquals("42s", formatTtlCompact(42))
        assertEquals("1m", formatTtlCompact(60))
        assertEquals("28m", formatTtlCompact(1_694))
        assertEquals("2h", formatTtlCompact(7_200))
        assertEquals("3d", formatTtlCompact(3 * 86_400L))
    }

    @Test
    fun `bytes fall back to an em dash`() {
        assertEquals("—", formatBytes(null))
    }

    @Test
    fun `bytes choose units and trim trailing zeros`() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("1023 B", formatBytes(1023))
        assertEquals("1 KB", formatBytes(1024))
        assertEquals("1.4 KB", formatBytes(1433))
        assertEquals("2 GB", formatBytes(2L * 1024 * 1024 * 1024))
        assertEquals("1.24 GB", formatBytes(1331439861))
    }

    @Test
    fun `count uses thousands separators`() {
        assertEquals("0", formatCount(0))
        assertEquals("999", formatCount(999))
        assertEquals("1,000", formatCount(1000))
        assertEquals("12,480", formatCount(12_480))
        assertEquals("-1,500", formatCount(-1_500))
    }

    @Test
    fun `duration switches between micros millis and seconds`() {
        assertEquals("900 µs", formatDurationMicros(900))
        assertEquals("1.5 ms", formatDurationMicros(1_500))
        assertEquals("12 ms", formatDurationMicros(12_000))
        assertEquals("1.5 s", formatDurationMicros(1_500_000))
    }
}
