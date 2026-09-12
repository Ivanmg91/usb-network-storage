package com.usblocal.app.ui.browser.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usblocal.app.data.model.SmbFile
import com.usblocal.app.util.FileUtils

enum class FileAction {
    DOWNLOAD, SHARE, RENAME, DELETE, INFO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionsSheet(
    file: SmbFile,
    onAction: (FileAction) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // File header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = FileUtils.getFileIcon(file.extension, file.isDirectory),
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!file.isDirectory && file.size > 0) {
                        Text(
                            text = FileUtils.formatFileSize(file.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Actions
            if (!file.isDirectory) {
                ActionItem(
                    icon = Icons.Filled.Download,
                    label = "Descargar",
                    onClick = { onAction(FileAction.DOWNLOAD); onDismiss() }
                )
                ActionItem(
                    icon = Icons.Filled.Share,
                    label = "Compartir",
                    onClick = { onAction(FileAction.SHARE); onDismiss() }
                )
            }

            ActionItem(
                icon = Icons.Filled.DriveFileRenameOutline,
                label = "Renombrar",
                onClick = { onAction(FileAction.RENAME); onDismiss() }
            )

            ActionItem(
                icon = Icons.Filled.Delete,
                label = "Eliminar",
                tint = MaterialTheme.colorScheme.error,
                onClick = { onAction(FileAction.DELETE); onDismiss() }
            )

            ActionItem(
                icon = Icons.Filled.Info,
                label = "Información",
                onClick = { onAction(FileAction.INFO); onDismiss() }
            )
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}
