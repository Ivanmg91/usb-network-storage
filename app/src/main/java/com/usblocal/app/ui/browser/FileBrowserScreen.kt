package com.usblocal.app.ui.browser

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import com.usblocal.app.ui.browser.components.BreadcrumbBar
import com.usblocal.app.ui.browser.components.FileAction
import com.usblocal.app.ui.browser.components.FileActionsSheet
import com.usblocal.app.ui.browser.components.FileListItem
import com.usblocal.app.ui.browser.components.TransferProgressDialog
import com.usblocal.app.ui.browser.components.TransferProgressDialog
import com.usblocal.app.ui.common.EmptyState
import com.usblocal.app.ui.common.LoadingIndicator
import com.usblocal.app.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMediaViewer: (connectionId: String, path: String) -> Unit = { _, _ -> },
    onNavigateToTextViewer: (connectionId: String, path: String) -> Unit = { _, _ -> },
    viewModel: FileBrowserViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Handle back navigation
    BackHandler(enabled = viewModel.canNavigateBack()) {
        viewModel.navigateBack()
    }

    // Show operation messages
    LaunchedEffect(state.operationMessage) {
        state.operationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearOperationMessage()
        }
    }

    // Launchers for upload and download
    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null) {
            val file = state.selectedFileForAction
            if (file != null) {
                try {
                    val outputStream = context.contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        viewModel.downloadFile(file, outputStream)
                    }
                } catch (e: Exception) {
                    coroutineScope.launch { snackbarHostState.showSnackbar("Error al abrir archivo local") }
                }
            }
        }
    }

    val getContentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val docFile = DocumentFile.fromSingleUri(context, uri)
                val name = docFile?.name ?: "archivo_subido"
                val size = docFile?.length() ?: 0L
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    viewModel.uploadFile(name, size, inputStream)
                }
            } catch (e: Exception) {
                coroutineScope.launch { snackbarHostState.showSnackbar("Error al abrir archivo para subir") }
            }
        }
    }

    // Helper for sharing
    fun shareFile(file: com.usblocal.app.data.model.SmbFile) {
        coroutineScope.launch {
            try {
                val sharedDir = File(context.cacheDir, "shared")
                if (!sharedDir.exists()) sharedDir.mkdirs()
                val localFile = File(sharedDir, file.name)
                val outputStream = withContext(Dispatchers.IO) { FileOutputStream(localFile) }

                // Note: using the same download logic.
                viewModel.downloadFile(file, outputStream)
                
                // We observe the download in the UI and fire the intent when complete.
                // But it's tricky to wait for it here if the ViewModel manages state.
                // For simplicity, we just notify the user that we are preparing the share.
                // A complete implementation would wait for `isComplete` and then fire the Intent.
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error preparando archivo para compartir")
            }
        }
    }
    
    // Check when a share download completes (if it was meant for sharing - tracked loosely via cache dir)
    LaunchedEffect(state.transferProgress?.isComplete) {
        if (state.transferProgress?.isComplete == true && !state.transferProgress!!.isUpload) {
            // Check if it's in the shared dir
            val sharedDir = File(context.cacheDir, "shared")
            val localFile = File(sharedDir, state.transferProgress!!.fileName)
            if (localFile.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    localFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir archivo"))
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    BreadcrumbBar(
                        shareName = state.connection?.shareName ?: "SMB",
                        currentPath = state.currentPath,
                        onNavigateToPath = viewModel::navigateToPath
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!viewModel.navigateBack()) onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, "Actualizar")
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, "Más opciones")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ordenar por nombre (A-Z)") },
                                onClick = {
                                    showMenu = false
                                    viewModel.setSortOption(SortOption.NAME_ASC)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ordenar por fecha") },
                                onClick = {
                                    showMenu = false
                                    viewModel.setSortOption(SortOption.DATE_DESC)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ordenar por tamaño") },
                                onClick = {
                                    showMenu = false
                                    viewModel.setSortOption(SortOption.SIZE_DESC)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Nueva carpeta") },
                                leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.showCreateFolderDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Subir archivo") },
                                leadingIcon = { Icon(Icons.Filled.FileUpload, null) },
                                onClick = {
                                    showMenu = false
                                    getContentLauncher.launch("*/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Actualizar") },
                                leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.refresh()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            state.storageInfo?.let { (free, total) ->
                StorageBar(freeBytes = free, totalBytes = total)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Loading progress bar
            AnimatedVisibility(
                visible = state.isLoading || state.isOperating,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Content
            when {
                state.isLoading && state.files.isEmpty() -> {
                    LoadingIndicator("Conectando...")
                }

                state.error != null && state.files.isEmpty() -> {
                    EmptyState(
                        icon = "⚠️",
                        title = "No se puede conectar con el servidor",
                        subtitle = state.error ?: "Comprobar Wi-Fi, dirección IP, recurso compartido y credenciales."
                    )
                }

                state.files.isEmpty() && !state.isLoading -> {
                    EmptyState(
                        icon = "📂",
                        title = "Esta carpeta está vacía"
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(
                                items = state.files,
                                key = { it.path }
                            ) { file ->
                                FileListItem(
                                    file = file,
                                    onClick = {
                                        if (file.isDirectory) {
                                            viewModel.navigateToFolder(file)
                                        } else {
                                            // Handle file opening based on type
                                            when {
                                                FileUtils.isImageFile(file.extension) || FileUtils.isVideoFile(file.extension) -> {
                                                    state.connection?.id?.let { id ->
                                                        onNavigateToMediaViewer(id, file.path)
                                                    }
                                                }
                                                FileUtils.isTextFile(file.extension) -> {
                                                    state.connection?.id?.let { id ->
                                                        onNavigateToTextViewer(id, file.path)
                                                    }
                                                }
                                                else -> {
                                                    // Fallback: show actions
                                                    viewModel.showFileActionsSheet(file)
                                                }
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.showFileActionsSheet(file)
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 64.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== Dialogs & Bottom Sheets ====================

    // File Actions Bottom Sheet
    if (state.showFileActionsSheet && state.selectedFileForAction != null) {
        val file = state.selectedFileForAction!!
        FileActionsSheet(
            file = file,
            onAction = { action ->
                when (action) {
                    FileAction.DOWNLOAD -> {
                        // Launch SAF document creator
                        createDocLauncher.launch(file.name)
                    }
                    FileAction.SHARE -> {
                        shareFile(file)
                    }
                    FileAction.RENAME -> viewModel.showRenameDialog(file)
                    FileAction.DELETE -> viewModel.showDeleteDialog(file)
                    FileAction.INFO -> viewModel.showFileInfoDialog(file)
                }
            },
            onDismiss = viewModel::dismissFileActionsSheet
        )
    }

    // Create folder dialog
    if (state.showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::dismissCreateFolderDialog,
            title = { Text("Nueva carpeta") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Nombre de la carpeta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.createFolder(folderName) },
                    enabled = folderName.isNotBlank()
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCreateFolderDialog) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Rename dialog
    if (state.showRenameDialog) {
        val file = state.selectedFileForAction
        var newName by remember(file) { mutableStateOf(file?.name ?: "") }
        AlertDialog(
            onDismissRequest = viewModel::dismissRenameDialog,
            title = { Text("Renombrar") },
            text = {
                Column {
                    Text(
                        text = "Nombre actual: ${file?.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nuevo nombre") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.rename(newName) },
                    enabled = newName.isNotBlank() && newName != file?.name
                ) {
                    Text("Renombrar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRenameDialog) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (state.showDeleteDialog) {
        val file = state.selectedFileForAction
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("Eliminar") },
            text = {
                Text(
                    if (file?.isDirectory == true) {
                        "¿Quieres eliminar la carpeta \"${file.name}\" y todo su contenido?"
                    } else {
                        "¿Quieres eliminar \"${file?.name}\"?"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::deleteConfirmed) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    // File Info Dialog
    if (state.showFileInfoDialog) {
        val file = state.selectedFileForAction
        AlertDialog(
            onDismissRequest = viewModel::dismissFileInfoDialog,
            title = { Text("Información") },
            text = {
                Column {
                    Text(
                        text = "Nombre: ${file?.name}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Ruta: ${file?.path}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (file?.isDirectory == false) {
                        Text(
                            text = "Tamaño: ${FileUtils.formatFileSize(file.size)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "Modificado: ${FileUtils.formatDate(file?.lastModified ?: 0L)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissFileInfoDialog) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Transfer Progress Dialog
    state.transferProgress?.let { progress ->
        TransferProgressDialog(
            progress = progress,
            onCancel = {
                // Cancel not implemented yet - just dismiss for now
                viewModel.updateTransferProgress(null)
            },
            onDismiss = {
                viewModel.updateTransferProgress(null)
            }
        )
    }
}
