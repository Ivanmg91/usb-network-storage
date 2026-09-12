package com.usblocal.app.data.model

/** Detailed result of a connection test, reporting each step */
data class ConnectionTestResult(
    val hostReachable: StepResult = StepResult.Pending,
    val portAccessible: StepResult = StepResult.Pending,
    val smbNegotiated: StepResult = StepResult.Pending,
    val authenticated: StepResult = StepResult.Pending,
    val shareAccessible: StepResult = StepResult.Pending,
    val canListFiles: StepResult = StepResult.Pending
) {
    val isFullySuccessful: Boolean
        get() = listOf(
            hostReachable, portAccessible, smbNegotiated,
            authenticated, shareAccessible, canListFiles
        ).all { it is StepResult.Success }

    val currentStep: String
        get() = when {
            hostReachable is StepResult.Pending -> "Comprobando servidor..."
            portAccessible is StepResult.Pending -> "Comprobando puerto SMB..."
            smbNegotiated is StepResult.Pending -> "Negociando protocolo SMB..."
            authenticated is StepResult.Pending -> "Verificando credenciales..."
            shareAccessible is StepResult.Pending -> "Accediendo al recurso compartido..."
            canListFiles is StepResult.Pending -> "Listando contenido..."
            isFullySuccessful -> "¡Conexión correcta!"
            else -> "Error en la prueba"
        }

    val firstError: SmbError?
        get() = listOf(
            hostReachable, portAccessible, smbNegotiated,
            authenticated, shareAccessible, canListFiles
        ).filterIsInstance<StepResult.Failed>().firstOrNull()?.error
}

sealed class StepResult {
    data object Pending : StepResult()
    data object InProgress : StepResult()
    data object Success : StepResult()
    data class Failed(val error: SmbError) : StepResult()
}
