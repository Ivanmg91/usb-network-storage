package com.usblocal.app.ui.browser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usblocal.app.data.model.SmbConnection
import com.usblocal.app.data.model.SmbFile
import com.usblocal.app.data.repository.ConnectionRepository
import com.usblocal.app.data.repository.FileRepository
import com.usblocal.app.data.smb.SmbException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.usblocal.app.ui.browser.components.TransferProgress

enum class SortOption {
    NAME_ASC, NAME_DESC,
    DATE_DESC, DATE_ASC,
    SIZE_DESC, SIZE_ASC
}

data class FileBrowserState(
    val connection: SmbConnection? = null,
    val currentPath: String = "",
    val files: List<SmbFile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val pathHistory: List<String> = listOf(""),
    val sortOption: SortOption = SortOption.NAME_ASC,
    val storageInfo: Pair<Long, Long>? = null,
    val freeSpaceBytes: Long? = null,

    // Dialogs
    val showCreateFolderDialog: Boolean = false,
    val showRenameDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showFileInfoDialog: Boolean = false,
    val selectedFileForAction: SmbFile? = null,
    val showFileActionsSheet: Boolean = false,

    // Transfers
    val transferProgress: TransferProgress? = null,

    // Operation feedback
    val operationMessage: String? = null,
    val isOperating: Boolean = false
)

@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val fileRepository: FileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val connectionId: String = savedStateHandle["connectionId"] ?: ""

    private val _state = MutableStateFlow(FileBrowserState())
    val state: StateFlow<FileBrowserState> = _state.asStateFlow()

    init {
        loadConnection()
    }

    private fun loadConnection() {
        val connection = connectionRepository.getConnection(connectionId)
        if (connection != null) {
            _state.update { it.copy(connection = connection) }
            loadFiles("")
        } else {
            _state.update { it.copy(error = "Conexión no encontrada") }
        }
    }

    fun loadFiles(path: String, forceRefresh: Boolean = false) {
        val connection = _state.value.connection ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = fileRepository.listFiles(connection, path, forceRefresh)

            result.fold(
                onSuccess = { files ->
                    _state.update {
                        val newHistory = if (path != it.currentPath) {
                            if (it.pathHistory.lastOrNull() != path) {
                                it.pathHistory + path
                            } else it.pathHistory
                        } else it.pathHistory

                        it.copy(
                            files = sortFiles(files, it.sortOption),
                            currentPath = path,
                            pathHistory = newHistory,
                            isLoading = false,
                            error = null
                        )
                    }

                    // Fetch storage info asynchronously without blocking UI
                    viewModelScope.launch {
                        connection?.let { conn ->
                            val storageResult = fileRepository.getStorageInfo(conn, path)
                            storageResult.onSuccess { info ->
                                _state.update { state -> state.copy(storageInfo = info) }
                            }
                        }
                    }
                },
                onFailure = { throwable ->
                    val errorMsg = if (throwable is SmbException) {
                        throwable.smbError.userMessage
                    } else {
                        throwable.message ?: "Error desconocido"
                    }
                    _state.update { it.copy(isLoading = false, error = errorMsg) }
                }
            )
        }
    }

    fun refresh() {
        loadFiles(_state.value.currentPath, forceRefresh = true)
    }

    fun navigateToFolder(folder: SmbFile) {
        if (folder.isDirectory) {
            loadFiles(folder.path)
        }
    }

    fun navigateToPath(path: String) {
        loadFiles(path)
    }

    fun setSortOption(option: SortOption) {
        _state.update {
            it.copy(
                sortOption = option,
                files = sortFiles(it.files, option)
            )
        }
    }

    private fun sortFiles(files: List<SmbFile>, option: SortOption): List<SmbFile> {
        val comparator = when (option) {
            SortOption.NAME_ASC -> compareBy<SmbFile> { it.name.lowercase() }
            SortOption.NAME_DESC -> compareByDescending<SmbFile> { it.name.lowercase() }
            SortOption.DATE_DESC -> compareByDescending<SmbFile> { it.lastModified }
            SortOption.DATE_ASC -> compareBy<SmbFile> { it.lastModified }
            SortOption.SIZE_DESC -> compareByDescending<SmbFile> { it.size }
            SortOption.SIZE_ASC -> compareBy<SmbFile> { it.size }
        }
        // Always put directories first, then apply the chosen sorting
        return files.sortedWith(compareByDescending<SmbFile> { it.isDirectory }.then(comparator))
    }

    fun navigateBack(): Boolean {
        val history = _state.value.pathHistory
        if (history.size <= 1) return false

        val newHistory = history.dropLast(1)
        val previousPath = newHistory.last()
        _state.update { it.copy(pathHistory = newHistory) }
        loadFiles(previousPath)
        return true
    }

    fun canNavigateBack(): Boolean = _state.value.pathHistory.size > 1

    // ==================== File Actions Sheet ====================

    fun showFileActionsSheet(file: SmbFile) {
        _state.update { it.copy(showFileActionsSheet = true, selectedFileForAction = file) }
    }

    fun dismissFileActionsSheet() {
        _state.update { it.copy(showFileActionsSheet = false) }
    }

    fun showFileInfoDialog(file: SmbFile? = null) {
        if (file != null) {
            _state.update { it.copy(selectedFileForAction = file, showFileInfoDialog = true) }
        } else {
            _state.update { it.copy(showFileInfoDialog = true) }
        }
    }

    fun dismissFileInfoDialog() {
        _state.update { it.copy(showFileInfoDialog = false, selectedFileForAction = null) }
    }

    // ==================== File Operations ====================

    fun showCreateFolderDialog() {
        _state.update { it.copy(showCreateFolderDialog = true) }
    }

    fun dismissCreateFolderDialog() {
        _state.update { it.copy(showCreateFolderDialog = false) }
    }

    fun createFolder(name: String) {
        val connection = _state.value.connection ?: return
        val path = _state.value.currentPath

        viewModelScope.launch {
            _state.update { it.copy(isOperating = true, showCreateFolderDialog = false) }

            val result = fileRepository.createFolder(connection, path, name)
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isOperating = false, operationMessage = "Carpeta creada") }
                    refresh()
                },
                onFailure = { throwable ->
                    val msg = (throwable as? SmbException)?.smbError?.userMessage
                        ?: "Error al crear la carpeta"
                    _state.update { it.copy(isOperating = false, operationMessage = msg) }
                }
            )
        }
    }

    fun showRenameDialog(file: SmbFile) {
        _state.update { it.copy(showRenameDialog = true, selectedFileForAction = file) }
    }

    fun dismissRenameDialog() {
        _state.update { it.copy(showRenameDialog = false, selectedFileForAction = null) }
    }

    fun rename(newName: String) {
        val connection = _state.value.connection ?: return
        val file = _state.value.selectedFileForAction ?: return

        viewModelScope.launch {
            _state.update { it.copy(isOperating = true, showRenameDialog = false) }

            val result = fileRepository.rename(connection, file.path, newName)
            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(isOperating = false, operationMessage = "Renombrado correctamente", selectedFileForAction = null)
                    }
                    refresh()
                },
                onFailure = { throwable ->
                    val msg = (throwable as? SmbException)?.smbError?.userMessage ?: "Error al renombrar"
                    _state.update { it.copy(isOperating = false, operationMessage = msg, selectedFileForAction = null) }
                }
            )
        }
    }

    fun showDeleteDialog(file: SmbFile) {
        _state.update { it.copy(showDeleteDialog = true, selectedFileForAction = file) }
    }

    fun dismissDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = false, selectedFileForAction = null) }
    }

    fun deleteConfirmed() {
        val connection = _state.value.connection ?: return
        val file = _state.value.selectedFileForAction ?: return

        viewModelScope.launch {
            _state.update { it.copy(isOperating = true, showDeleteDialog = false) }

            val result = fileRepository.delete(connection, file.path, file.isDirectory)
            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(isOperating = false, operationMessage = "Eliminado correctamente", selectedFileForAction = null)
                    }
                    refresh()
                },
                onFailure = { throwable ->
                    val msg = (throwable as? SmbException)?.smbError?.userMessage ?: "Error al eliminar"
                    _state.update { it.copy(isOperating = false, operationMessage = msg, selectedFileForAction = null) }
                }
            )
        }
    }

    // ==================== Transfers ====================

    fun updateTransferProgress(progress: TransferProgress?) {
        _state.update { it.copy(transferProgress = progress) }
    }

    fun downloadFile(file: SmbFile, outputStream: java.io.OutputStream) {
        val connection = _state.value.connection ?: return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    transferProgress = TransferProgress(
                        fileName = file.name,
                        totalBytes = file.size,
                        isActive = true,
                        isUpload = false
                    )
                )
            }

            val result = fileRepository.downloadFile(
                connection = connection,
                remotePath = file.path,
                outputStream = outputStream,
                onProgress = { transferred, total ->
                    _state.update {
                        it.copy(
                            transferProgress = it.transferProgress?.copy(
                                bytesTransferred = transferred,
                                totalBytes = total
                            )
                        )
                    }
                }
            )

            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            transferProgress = it.transferProgress?.copy(
                                isComplete = true,
                                isActive = false
                            )
                        )
                    }
                },
                onFailure = { throwable ->
                    val msg = (throwable as? SmbException)?.smbError?.userMessage ?: "Error al descargar"
                    _state.update {
                        it.copy(
                            transferProgress = it.transferProgress?.copy(
                                isActive = false,
                                error = msg
                            )
                        )
                    }
                }
            )
        }
    }

    fun uploadFile(name: String, size: Long, inputStream: java.io.InputStream) {
        val connection = _state.value.connection ?: return
        val currentPath = _state.value.currentPath
        val fullPath = if (currentPath.isEmpty()) name else "$currentPath\\$name"

        viewModelScope.launch {
            _state.update {
                it.copy(
                    transferProgress = TransferProgress(
                        fileName = name,
                        totalBytes = size,
                        isActive = true,
                        isUpload = true
                    )
                )
            }

            val result = fileRepository.uploadFile(
                connection = connection,
                remotePath = fullPath,
                inputStream = inputStream,
                totalSize = size,
                onProgress = { transferred, total ->
                    _state.update {
                        it.copy(
                            transferProgress = it.transferProgress?.copy(
                                bytesTransferred = transferred,
                                totalBytes = total
                            )
                        )
                    }
                }
            )

            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            transferProgress = it.transferProgress?.copy(
                                isComplete = true,
                                isActive = false
                            )
                        )
                    }
                    refresh()
                },
                onFailure = { throwable ->
                    val msg = (throwable as? SmbException)?.smbError?.userMessage ?: "Error al subir"
                    _state.update {
                        it.copy(
                            transferProgress = it.transferProgress?.copy(
                                isActive = false,
                                error = msg
                            )
                        )
                    }
                }
            )
        }
    }

    fun clearOperationMessage() {
        _state.update { it.copy(operationMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        // Don't close connections here — they're managed by the singleton data source
    }
}
