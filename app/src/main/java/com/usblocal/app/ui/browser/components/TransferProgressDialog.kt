package com.usblocal.app.ui.browser.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.usblocal.app.util.FileUtils

data class TransferProgress(
    val fileName: String = "",
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val isUpload: Boolean = false,
    val isActive: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    val percentage: Float
        get() = if (totalBytes > 0) (bytesTransferred.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val percentageInt: Int get() = (percentage * 100).toInt()
}

@Composable
fun TransferProgressDialog(
    progress: TransferProgress,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.percentage,
        animationSpec = tween(300),
        label = "progress"
    )

    AlertDialog(
        onDismissRequest = {
            if (progress.isComplete || progress.error != null) onDismiss()
        },
        title = {
            Text(
                text = if (progress.isUpload) "Subiendo archivo" else "Descargando archivo",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = progress.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (progress.error != null) {
                    Text(
                        text = progress.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (progress.isComplete) {
                    Text(
                        text = if (progress.isUpload) "✅ Archivo subido correctamente"
                        else "✅ Archivo descargado correctamente",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${FileUtils.formatFileSize(progress.bytesTransferred)} / ${FileUtils.formatFileSize(progress.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${progress.percentageInt}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (progress.isComplete || progress.error != null) {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        },
        dismissButton = {
            if (!progress.isComplete && progress.error == null) {
                TextButton(onClick = onCancel) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.error)
                }
            }
            if (progress.error != null) {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        }
    )
}
