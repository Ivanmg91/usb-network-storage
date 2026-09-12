package com.usblocal.app.ui.viewer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.usblocal.app.data.model.SmbFile
import com.usblocal.app.data.repository.ConnectionRepository
import com.usblocal.app.data.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class ViewerState(
    val isLoading: Boolean = true,
    val file: SmbFile? = null,
    val localFile: File? = null,
    val textContent: String? = null,
    val error: String? = null,
    val downloadProgress: Float = 0f
)

@HiltViewModel
class ViewerViewModel @Inject constructor(
    application: Application,
    private val connectionRepository: ConnectionRepository,
    private val fileRepository: FileRepository,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val connectionId: String = savedStateHandle["connectionId"] ?: ""
    private val path: String = savedStateHandle["path"] ?: ""

    private val _state = MutableStateFlow(ViewerState())
    val state: StateFlow<ViewerState> = _state.asStateFlow()

    init {
        loadFile()
    }

    private fun loadFile() {
        val connection = connectionRepository.getConnection(connectionId)
        if (connection == null) {
            _state.update { it.copy(isLoading = false, error = "Conexión no encontrada") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // 1. Get file metadata (we assume it's in the parent dir's list)
            val parentPath = path.substringBeforeLast('\\', "")
            val filesResult = fileRepository.listFiles(connection, parentPath)
            val smbFile = filesResult.getOrNull()?.find { it.path == path }
            
            if (smbFile == null) {
                _state.update { it.copy(isLoading = false, error = "Archivo no encontrado") }
                return@launch
            }

            _state.update { it.copy(file = smbFile) }

            // 2. Prepare local cache file
            val cacheDir = File(getApplication<Application>().cacheDir, "viewer_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            
            val localFile = File(cacheDir, smbFile.name)
            
            // Note: In a real app we'd compare size/date to avoid re-downloading
            // For simplicity, we just download it every time to ensure freshness
            
            try {
                val outputStream = withContext(Dispatchers.IO) { FileOutputStream(localFile) }
                
                val result = fileRepository.downloadFile(
                    connection = connection,
                    remotePath = path,
                    outputStream = outputStream,
                    onProgress = { transferred, total ->
                        if (total > 0) {
                            _state.update { it.copy(downloadProgress = (transferred.toFloat() / total).coerceIn(0f, 1f)) }
                        }
                    }
                )

                result.fold(
                    onSuccess = {
                        _state.update { it.copy(isLoading = false, localFile = localFile) }
                    },
                    onFailure = {
                        _state.update { it.copy(isLoading = false, error = "Error al descargar el archivo para visualizar") }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Error guardando el archivo local") }
            }
        }
    }

    fun loadTextContent() {
        val file = _state.value.localFile ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Read up to 1MB of text to avoid memory issues
                val content = if (file.length() > 1_000_000) {
                    file.inputStream().reader().use { it.readText().take(1_000_000) } + "\n\n... [Texto truncado]"
                } else {
                    file.readText()
                }
                _state.update { it.copy(textContent = content) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Error al leer el texto") }
            }
        }
    }
}
