package com.czh.redis

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform