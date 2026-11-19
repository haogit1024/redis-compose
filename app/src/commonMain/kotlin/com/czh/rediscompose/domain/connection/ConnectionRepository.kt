package com.czh.rediscompose.domain.connection

interface ConnectionRepository {
    suspend fun load(): List<ConnectionProfile>

    suspend fun save(connections: List<ConnectionProfile>)

    /**
     * Updates the at-rest password preference for future saves.
     *
     * Changing the setting must not rewrite the file immediately; it takes effect the next
     * time a connection is saved (Phase 9 contract).
     */
    fun setEncryptPasswords(encryptPasswords: Boolean) = Unit
}
