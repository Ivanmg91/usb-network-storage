package com.usblocal.app.ui.viewer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.usblocal.app.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextViewerScreen(
    onNavigateBack: () -> Unit,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // When the file finishes downloading, trigger the text read
    LaunchedEffect(state.localFile) {
        if (state.localFile != null && state.textContent == null) {
            viewModel.loadTextContent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.file?.name ?: "Visor de texto",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.error != null -> {
                    EmptyState(
                        icon = "⚠️",
                        title = "Error",
                        subtitle = state.error
                    )
                }

                state.isLoading || state.textContent == null && state.localFile != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        if (state.isLoading) {
                            Text(
                                text = "Descargando ${(state.downloadProgress * 100).toInt()}%",
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        } else {
                            Text(
                                text = "Procesando texto...",
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }

                state.textContent != null -> {
                    SelectionContainer {
                        Text(
                            text = state.textContent ?: "",
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
