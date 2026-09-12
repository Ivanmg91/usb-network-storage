package com.usblocal.app.ui.connections

import androidx.lifecycle.ViewModel
import com.usblocal.app.data.model.SmbConnection
import com.usblocal.app.data.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository
) : ViewModel() {

    val connections: StateFlow<List<SmbConnection>> = connectionRepository.connections

    fun deleteConnection(connectionId: String) {
        connectionRepository.deleteConnection(connectionId)
    }

    fun refresh() {
        connectionRepository.refreshConnections()
    }
}
