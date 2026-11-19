package com.czh.rediscompose.util

actual fun isMacOs(): Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("mac")
