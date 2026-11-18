package com.czh.rediscompose.domain.connection

enum class ConnectionField {
    NAME,
    HOST,
    PORT,
    USERNAME,
    PASSWORD,
    DATABASE,
    TLS_CA,
    TLS_CERT,
    TLS_KEY,
    SSH_HOST,
    SSH_PORT,
    SSH_USER,
    SSH_KEY,
}

data class ConnectionValidation(val errors: Map<ConnectionField, String>) {
    val isValid: Boolean get() = errors.isEmpty()
}

/**
 * Field-level validation for the connection form.
 *
 * Returns every problem instead of throwing so the editor can render messages below the
 * matching inputs.
 */
fun validateConnection(profile: ConnectionProfile): ConnectionValidation {
    val errors = linkedMapOf<ConnectionField, String>()

    if (profile.name.isBlank()) {
        errors[ConnectionField.NAME] = "请输入连接名称"
    }
    if (profile.host.isBlank()) {
        errors[ConnectionField.HOST] = "请输入主机地址"
    }
    if (profile.port !in 1..65535) {
        errors[ConnectionField.PORT] = "端口需在 1–65535 之间"
    }
    if (profile.defaultDatabase !in 0..255) {
        errors[ConnectionField.DATABASE] = "数据库编号需在 0–255 之间"
    }

    if (profile.tls.enabled) {
        validatePath(profile.tls.caPath)?.let { errors[ConnectionField.TLS_CA] = it }
        validatePath(profile.tls.clientCertPath)?.let { errors[ConnectionField.TLS_CERT] = it }
        validatePath(profile.tls.clientKeyPath)?.let { errors[ConnectionField.TLS_KEY] = it }
    }

    if (profile.ssh.enabled) {
        if (profile.ssh.host.isBlank()) {
            errors[ConnectionField.SSH_HOST] = "请输入 SSH 主机地址"
        }
        if (profile.ssh.port !in 1..65535) {
            errors[ConnectionField.SSH_PORT] = "SSH 端口需在 1–65535 之间"
        }
        if (profile.ssh.username.isBlank()) {
            errors[ConnectionField.SSH_USER] = "请输入 SSH 用户名"
        }
        if (profile.ssh.authMethod == SshAuthMethod.PRIVATE_KEY && profile.ssh.privateKeyPath.isBlank()) {
            errors[ConnectionField.SSH_KEY] = "请选择 SSH 私钥文件"
        }
    }

    return ConnectionValidation(errors)
}

private fun validatePath(path: String): String? {
    if (path.isBlank()) return null
    val looksAbsolute =
        path.startsWith("/") ||
            path.startsWith("~") ||
            WINDOWS_ABSOLUTE.containsMatchIn(path)
    return if (looksAbsolute) null else "请填写绝对路径或以 ~ 开头"
}

private val WINDOWS_ABSOLUTE = Regex("""^[A-Za-z]:[\\/]""")
