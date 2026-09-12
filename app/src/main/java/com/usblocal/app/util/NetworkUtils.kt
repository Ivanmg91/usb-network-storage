package com.usblocal.app.util

import java.net.Inet4Address
import java.net.InetAddress

object NetworkUtils {

    /** Validate an IP address or hostname */
    fun isValidHost(host: String): Boolean {
        if (host.isBlank()) return false
        // Check IPv4 pattern
        val ipv4Regex = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")
        if (ipv4Regex.matches(host)) {
            return host.split('.').all { part ->
                val num = part.toIntOrNull() ?: return false
                num in 0..255
            }
        }
        // Accept hostnames (letters, digits, hyphens, dots)
        val hostnameRegex = Regex("""^[a-zA-Z0-9]([a-zA-Z0-9\-]*[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9\-]*[a-zA-Z0-9])?)*$""")
        return hostnameRegex.matches(host)
    }

    /** Check if port is valid */
    fun isValidPort(port: Int): Boolean = port in 1..65535

    /** Check if an address is a private/local network address */
    fun isLocalAddress(host: String): Boolean {
        return try {
            val address = InetAddress.getByName(host)
            address.isSiteLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress
        } catch (e: Exception) {
            // Fallback: check common private ranges
            isPrivateIpRange(host)
        }
    }

    private fun isPrivateIpRange(host: String): Boolean {
        val parts = host.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false

        return when {
            // 10.0.0.0 - 10.255.255.255
            parts[0] == 10 -> true
            // 172.16.0.0 - 172.31.255.255
            parts[0] == 172 && parts[1] in 16..31 -> true
            // 192.168.0.0 - 192.168.255.255
            parts[0] == 192 && parts[1] == 168 -> true
            // Loopback
            parts[0] == 127 -> true
            else -> false
        }
    }

    /** Build SMB URL for display */
    fun buildSmbUrl(host: String, port: Int, shareName: String): String {
        val portPart = if (port != 445) ":$port" else ""
        return "smb://$host$portPart/$shareName"
    }
}
