package com.usblocal.app.ui.connections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.usblocal.app.data.model.StepResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditConnectionScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditConnectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isEditing) "Editar conexión" else "Nueva conexión")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Connection name
            OutlinedTextField(
                value = state.connection.name,
                onValueChange = viewModel::updateName,
                label = { Text("Nombre de la conexión (opcional)") },
                placeholder = { Text("Ej: Router casa") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Host
            OutlinedTextField(
                value = state.connection.host,
                onValueChange = viewModel::updateHost,
                label = { Text("Dirección IP / Host *") },
                placeholder = { Text("192.168.1.1") },
                isError = state.validationErrors.containsKey("host"),
                supportingText = state.validationErrors["host"]?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )

            // Port
            OutlinedTextField(
                value = state.connection.port.toString(),
                onValueChange = viewModel::updatePort,
                label = { Text("Puerto") },
                placeholder = { Text("445") },
                isError = state.validationErrors.containsKey("port"),
                supportingText = state.validationErrors["port"]?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Share name
            OutlinedTextField(
                value = state.connection.shareName,
                onValueChange = viewModel::updateShareName,
                label = { Text("Recurso compartido *") },
                placeholder = { Text("USB") },
                isError = state.validationErrors.containsKey("shareName"),
                supportingText = state.validationErrors["shareName"]?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Guest access toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Acceso anónimo / invitado",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Conectar sin usuario ni contraseña",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.connection.isGuest,
                    onCheckedChange = viewModel::updateIsGuest
                )
            }

            // Credentials (hidden when guest)
            AnimatedVisibility(
                visible = !state.connection.isGuest,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.connection.username,
                        onValueChange = viewModel::updateUsername,
                        label = { Text("Usuario *") },
                        isError = state.validationErrors.containsKey("username"),
                        supportingText = state.validationErrors["username"]?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.connection.password,
                        onValueChange = viewModel::updatePassword,
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Ocultar" else "Mostrar"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.connection.domain,
                        onValueChange = viewModel::updateDomain,
                        label = { Text("Dominio / Workgroup (opcional)") },
                        placeholder = { Text("WORKGROUP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Remember credentials
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recordar credenciales",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = state.connection.rememberCredentials,
                    onCheckedChange = viewModel::updateRememberCredentials
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Test connection button
            OutlinedButton(
                onClick = viewModel::testConnection,
                enabled = !state.isTesting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Filled.NetworkCheck, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Probar conexión")
            }

            // Test results
            state.testResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.isFullySuccessful) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else if (result.firstError != null) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TestStepRow("Servidor accesible", result.hostReachable)
                        TestStepRow("Puerto SMB accesible", result.portAccessible)
                        TestStepRow("Protocolo SMB negociado", result.smbNegotiated)
                        TestStepRow("Credenciales válidas", result.authenticated)
                        TestStepRow("Recurso compartido accesible", result.shareAccessible)
                        TestStepRow("Listado de archivos", result.canListFiles)

                        result.firstError?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error.userMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save button
            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TestStepRow(label: String, result: StepResult) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (result) {
            is StepResult.Pending -> {
                Spacer(modifier = Modifier.size(20.dp))
            }
            is StepResult.InProgress -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }
            is StepResult.Success -> {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            is StepResult.Failed -> {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = when (result) {
                is StepResult.Failed -> MaterialTheme.colorScheme.error
                is StepResult.Success -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
