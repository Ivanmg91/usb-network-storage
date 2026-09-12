package com.usblocal.app.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SmbConnection(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val host: String = "",
    val port: Int = 445,
    val shareName: String = "",
    val username: String = "",
    val password: String = "",
    val domain: String = "",
    val isGuest: Boolean = false,
    val rememberCredentials: Boolean = true
) {
    /** Builds the display path like "smb://192.168.1.1/USB" */
    fun toSmbUrl(): String = "smb://$host${if (port != 445) ":$port" else ""}/$shareName"

    /** Validates all required fields are filled */
    fun isValid(): Boolean {
        if (host.isBlank()) return false
        if (shareName.isBlank()) return false
        if (port !in 1..65535) return false
        if (!isGuest && username.isBlank()) return false
        return true
    }

    /** Returns a copy with the password cleared (for logging/display) */
    fun sanitized(): SmbConnection = copy(password = "***")
}
