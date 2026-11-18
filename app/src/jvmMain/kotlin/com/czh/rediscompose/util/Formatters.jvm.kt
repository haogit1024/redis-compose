package com.czh.rediscompose.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")

actual fun formatEpochSeconds(seconds: Long): String =
    Instant.ofEpochSecond(seconds)
        .atZone(ZoneId.systemDefault())
        .format(TIME_FORMATTER)
