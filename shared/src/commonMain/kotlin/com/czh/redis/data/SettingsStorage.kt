package com.czh.redis.data

expect fun loadSettings(): AppSettings

expect fun saveSettings(settings: AppSettings)
