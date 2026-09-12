package com.usblocal.app.ui.connections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usblocal.app.data.model.ConnectionTestResult
import com.usblocal.app.data.model.SmbConnection
import com.usblocal.app.data.repository.ConnectionRepository
import com.usblocal.app.data.smb.SmbDataSource
import com.usblocal.app.util.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditConnectionState(
    val connection: SmbConnection = SmbConnection(),
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: ConnectionTestResult? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val savedSuccessfully: Boolean = false
)

@HiltViewModel
class AddEditConnectionViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val smbDataSource: SmbDataSource,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val connectionId: String? = savedStateHandle["connectionId"]

    private val _state = MutableStateFlow(AddEditConnectionState())
    val state: StateFlow<AddEditConnectionState> = _state.asStateFlow()

    init {
        connectionId?.let { id ->
            connectionRepository.getConnection(id)?.let { existing ->
                _state.update {
                    it.copy(connection = existing, isEditing = true)
                }
            }
        }
    }

    fun updateName(value: String) = updateConnection { copy(name = value) }
    fun updateHost(value: String) = updateConnection { copy(host = value.trim()) }
    fun updatePort(value: String) {
        val port = value.toIntOrNull() ?: return
        updateConnection { copy(port = port) }
    }
    fun updateShareName(value: String) = updateConnection { copy(shareName = value.trim()) }
    fun updateUsername(value: String) = updateConnection { copy(username = value) }
    fun updatePassword(value: String) = updateConnection { copy(password = value) }
    fun updateDomain(value: String) = updateConnection { copy(domain = value.trim()) }
    fun updateIsGuest(value: Boolean) = updateConnection { copy(isGuest = value) }
    fun updateRememberCredentials(value: Boolean) = updateConnection { copy(rememberCredentials = value) }

    private fun updateConnection(transform: SmbConnection.() -> SmbConnection) {
        _state.update {
            it.copy(
                connection = it.connection.transform(),
                validationErrors = emptyMap(),
                testResult = null
            )
        }
    }

    fun validate(): Boolean {
        val conn = _state.value.connection
        val errors = mutableMapOf<String, String>()

        if (conn.host.isBlank()) {
            errors["host"] = "La dirección IP/host es obligatoria"
        } else if (!NetworkUtils.isValidHost(conn.host)) {
            errors["host"] = "Dirección IP/host no válida"
        }

        if (!NetworkUtils.isValidPort(conn.port)) {
            errors["port"] = "Puerto no válido (1-65535)"
        }

        if (conn.shareName.isBlank()) {
            errors["shareName"] = "El nombre del recurso es obligatorio"
        }

        if (!conn.isGuest && conn.username.isBlank()) {
            errors["username"] = "El usuario es obligatorio (o activa acceso anónimo)"
        }

        _state.update { it.copy(validationErrors = errors) }
        return errors.isEmpty()
    }

    fun testConnection() {
        if (!validate()) return

        viewModelScope.launch {
            _state.update { it.copy(isTesting = true, testResult = null) }

            smbDataSource.testConnection(_state.value.connection).collect { result ->
                _state.update { it.copy(testResult = result) }
            }

            _state.update { it.copy(isTesting = false) }
        }
    }

    fun save() {
        if (!validate()) return

        _state.update { it.copy(isSaving = true) }

        val conn = _state.value.connection.let {
            if (it.name.isBlank()) it.copy(name = "${it.host}/${it.shareName}") else it
        }

        connectionRepository.saveConnection(conn)
        _state.update { it.copy(isSaving = false, savedSuccessfully = true) }
    }
}
