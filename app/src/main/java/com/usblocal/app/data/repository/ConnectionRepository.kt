package com.usblocal.app.data.repository

import com.usblocal.app.data.model.SmbConnection
import com.usblocal.app.data.storage.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepository @Inject constructor(
    private val secureStorage: SecureStorage
) {
    private val _connections = MutableStateFlow<List<SmbConnection>>(emptyList())
    val connections: StateFlow<List<SmbConnection>> = _connections.asStateFlow()

    init {
        refreshConnections()
    }

    fun refreshConnections() {
        _connections.value = secureStorage.loadConnections()
    }

    fun saveConnection(connection: SmbConnection) {
        secureStorage.saveConnection(connection)
        refreshConnections()
    }

    fun deleteConnection(connectionId: String) {
        secureStorage.deleteConnection(connectionId)
        refreshConnections()
    }

    fun getConnection(connectionId: String): SmbConnection? {
        return secureStorage.getConnection(connectionId)
    }
}
