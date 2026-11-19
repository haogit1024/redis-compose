package com.czh.rediscompose.domain.connection

/** Result of the connection sheet's "测试连接" action. */
data class ConnectionTestResult(
    val success: Boolean,
    val latencyMillis: Long? = null,
    val version: String? = null,
    val errorMessage: String? = null,
)
