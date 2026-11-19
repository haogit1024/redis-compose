package com.czh.rediscompose.feature.connections

import com.czh.rediscompose.domain.connection.ConnectionColor
import com.czh.rediscompose.domain.connection.ConnectionEnvironment
import com.czh.rediscompose.domain.connection.ConnectionField
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.SshAuthMethod
import com.czh.rediscompose.domain.connection.SshConfig
import com.czh.rediscompose.domain.connection.TlsConfig
import com.czh.rediscompose.domain.connection.TlsVerifyMode
import com.czh.rediscompose.domain.connection.validateConnection
import com.czh.rediscompose.util.currentEpochMillis

enum class ConnectionFormMode { NEW, EDIT, DUPLICATE }

enum class ConnectionSheetTab(val label: String) {
    BASIC("基础"),
    TLS("TLS"),
    SSH("SSH 隧道"),
    ADVANCED("高级"),
}

/**
 * Editable copy of a connection.
 *
 * Numeric fields stay strings while the sheet is open so the user can clear a field
 * without it snapping back; [validate] converts them once and reports field errors.
 */
data class ConnectionDraft(
    val id: String? = null,
    val mode: ConnectionFormMode = ConnectionFormMode.NEW,
    val name: String = "",
    val environment: ConnectionEnvironment = ConnectionEnvironment.LOCAL,
    val host: String = "127.0.0.1",
    val port: String = "6379",
    val username: String = "",
    val password: String = "",
    val defaultDatabase: String = "0",
    val keySeparator: String = ":",
    val color: ConnectionColor = ConnectionColor.IRIS,
    val group: String = "",
    val readOnly: Boolean = false,
    val tlsEnabled: Boolean = false,
    val tlsCaPath: String = "",
    val tlsClientCertPath: String = "",
    val tlsClientKeyPath: String = "",
    val tlsVerifyMode: TlsVerifyMode = TlsVerifyMode.STRICT,
    val sshEnabled: Boolean = false,
    val sshHost: String = "",
    val sshPort: String = "22",
    val sshUsername: String = "",
    val sshAuthMethod: SshAuthMethod = SshAuthMethod.PRIVATE_KEY,
    val sshPassword: String = "",
    val sshPrivateKeyPath: String = "",
    val connectTimeout: String = "5000",
    val readTimeout: String = "5000",
    val lastOpenedAtEpochMillis: Long? = null,
    val createdAtEpochMillis: Long = 0L,
) {
    val isEdit: Boolean get() = mode == ConnectionFormMode.EDIT

    val title: String
        get() =
            when (mode) {
                ConnectionFormMode.NEW -> "新建连接"
                ConnectionFormMode.EDIT -> "编辑连接"
                ConnectionFormMode.DUPLICATE -> "复制连接"
            }

    val subtitle: String
        get() =
            when (mode) {
                ConnectionFormMode.NEW -> "配置 Redis 实例、TLS 与 SSH 隧道"
                ConnectionFormMode.EDIT -> "修改后会断开旧会话，重新打开时使用新配置"
                ConnectionFormMode.DUPLICATE -> "保存为一个独立的新连接"
            }

    fun toProfile(
        nowEpochMillis: Long,
        id: String? = this.id,
    ): ConnectionProfile =
        ConnectionProfile(
            id = id ?: "conn-$nowEpochMillis",
            name = name.trim(),
            environment = environment,
            host = host.trim(),
            port = port.toIntOrNull() ?: -1,
            username = username.trim(),
            password = password,
            defaultDatabase = defaultDatabase.toIntOrNull() ?: -1,
            keySeparator = keySeparator,
            color = color,
            group = group.trim(),
            tls =
                TlsConfig(
                    enabled = tlsEnabled,
                    caPath = tlsCaPath.trim(),
                    clientCertPath = tlsClientCertPath.trim(),
                    clientKeyPath = tlsClientKeyPath.trim(),
                    verifyMode = tlsVerifyMode,
                ),
            ssh =
                SshConfig(
                    enabled = sshEnabled,
                    host = sshHost.trim(),
                    port = sshPort.toIntOrNull() ?: -1,
                    username = sshUsername.trim(),
                    authMethod = sshAuthMethod,
                    password = sshPassword,
                    privateKeyPath = sshPrivateKeyPath.trim(),
                ),
            readOnly = readOnly,
            connectTimeoutMillis = connectTimeout.toIntOrNull() ?: 0,
            readTimeoutMillis = readTimeout.toIntOrNull() ?: 0,
            lastOpenedAtEpochMillis = lastOpenedAtEpochMillis,
            createdAtEpochMillis = if (createdAtEpochMillis > 0L) createdAtEpochMillis else nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis,
        )

    fun validate(nowEpochMillis: Long = currentEpochMillis()): Map<ConnectionField, String> {
        val profile = toProfile(nowEpochMillis)
        val errors = validateConnection(profile).errors.toMutableMap()
        if (port.toIntOrNull() == null) errors[ConnectionField.PORT] = "请输入有效端口号"
        if (defaultDatabase.toIntOrNull() == null) errors[ConnectionField.DATABASE] = "请输入有效数据库编号"
        if (connectTimeout.toIntOrNull() == null) errors[ConnectionField.CONNECT_TIMEOUT] = "请输入有效毫秒数"
        if (readTimeout.toIntOrNull() == null) errors[ConnectionField.READ_TIMEOUT] = "请输入有效毫秒数"
        if (sshEnabled && sshPort.toIntOrNull() == null) errors[ConnectionField.SSH_PORT] = "请输入有效 SSH 端口"
        return errors
    }

    companion object {
        fun new(): ConnectionDraft = ConnectionDraft()

        fun fromProfile(
            profile: ConnectionProfile,
            mode: ConnectionFormMode,
        ): ConnectionDraft =
            ConnectionDraft(
                id = profile.id,
                mode = mode,
                name = profile.name,
                environment = profile.environment,
                host = profile.host,
                port = profile.port.toString(),
                username = profile.username,
                password = profile.password,
                defaultDatabase = profile.defaultDatabase.toString(),
                keySeparator = profile.keySeparator,
                color = profile.color,
                group = profile.group,
                readOnly = profile.readOnly,
                tlsEnabled = profile.tls.enabled,
                tlsCaPath = profile.tls.caPath,
                tlsClientCertPath = profile.tls.clientCertPath,
                tlsClientKeyPath = profile.tls.clientKeyPath,
                tlsVerifyMode = profile.tls.verifyMode,
                sshEnabled = profile.ssh.enabled,
                sshHost = profile.ssh.host,
                sshPort = profile.ssh.port.toString(),
                sshUsername = profile.ssh.username,
                sshAuthMethod = profile.ssh.authMethod,
                sshPassword = profile.ssh.password,
                sshPrivateKeyPath = profile.ssh.privateKeyPath,
                connectTimeout = profile.connectTimeoutMillis.toString(),
                readTimeout = profile.readTimeoutMillis.toString(),
                lastOpenedAtEpochMillis = profile.lastOpenedAtEpochMillis,
                createdAtEpochMillis = profile.createdAtEpochMillis,
            )
    }
}
