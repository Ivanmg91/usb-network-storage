package com.usblocal.app.data.model

/** Typed SMB errors with user-friendly messages in Spanish */
sealed class SmbError(
    val userMessage: String,
    val technicalMessage: String = ""
) {
    class HostUnreachable(host: String) : SmbError(
        userMessage = "Servidor no encontrado: $host\nComprueba la dirección IP y que estés conectado a la misma red.",
        technicalMessage = "Host unreachable: $host"
    )

    class PortInaccessible(host: String, port: Int) : SmbError(
        userMessage = "Puerto SMB inaccesible ($port)\nEl servicio SMB puede no estar activo en $host.",
        technicalMessage = "Connection refused: $host:$port"
    )

    class AuthenticationFailed : SmbError(
        userMessage = "Usuario o contraseña incorrectos.\nRevisa las credenciales e inténtalo de nuevo.",
        technicalMessage = "STATUS_LOGON_FAILURE"
    )

    class AccessDenied(path: String = "") : SmbError(
        userMessage = "Permiso denegado${if (path.isNotEmpty()) ": $path" else ""}.\nNo tienes permisos para acceder a este recurso.",
        technicalMessage = "STATUS_ACCESS_DENIED"
    )

    class ShareNotFound(shareName: String) : SmbError(
        userMessage = "Recurso compartido no encontrado: \"$shareName\"\nVerifica el nombre del recurso en la configuración del router.",
        technicalMessage = "STATUS_BAD_NETWORK_NAME: $shareName"
    )

    class FileNotFound(path: String) : SmbError(
        userMessage = "Archivo o carpeta no encontrado: $path",
        technicalMessage = "STATUS_OBJECT_NAME_NOT_FOUND: $path"
    )

    class FileAlreadyExists(name: String) : SmbError(
        userMessage = "Ya existe un archivo con el nombre \"$name\".",
        technicalMessage = "STATUS_OBJECT_NAME_COLLISION: $name"
    )

    class NegotiationError : SmbError(
        userMessage = "Versión SMB no compatible.\nEste servidor puede requerir SMB1, que no está soportado por seguridad.",
        technicalMessage = "SMB negotiation failed"
    )

    class ConnectionLost : SmbError(
        userMessage = "Se perdió la conexión con el servidor.\nComprueba tu conexión Wi-Fi.",
        technicalMessage = "Connection lost"
    )

    class Timeout : SmbError(
        userMessage = "Tiempo de espera agotado.\nEl servidor no responde. Comprueba la conexión de red.",
        technicalMessage = "Connection timeout"
    )

    class InsufficientStorage : SmbError(
        userMessage = "Espacio insuficiente en el almacenamiento.",
        technicalMessage = "STATUS_DISK_FULL"
    )

    class PermissionDenied(operation: String = "") : SmbError(
        userMessage = "No tienes permiso para realizar esta operación${if (operation.isNotEmpty()) ": $operation" else ""}.",
        technicalMessage = "Permission denied"
    )

    class Unknown(cause: Throwable? = null) : SmbError(
        userMessage = "Error desconocido.\nInténtalo de nuevo o revisa la configuración.",
        technicalMessage = cause?.message ?: "Unknown error"
    )
}
