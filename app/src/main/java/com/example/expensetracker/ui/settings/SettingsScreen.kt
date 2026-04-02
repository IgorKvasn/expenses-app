package com.example.expensetracker.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val showImportConfirmation by viewModel.showImportConfirmation.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val importFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.onImportFileSelected(it, context.contentResolver) }
    }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportImportState.Success -> {
                state.fileUri?.let { uri ->
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Export data"))
                }
                viewModel.clearExportState()
            }
            is ExportImportState.Error -> {
                snackbarHostState.showSnackbar("Export failed: ${state.message}")
                viewModel.clearExportState()
            }
            else -> {}
        }
    }

    LaunchedEffect(importState) {
        when (val state = importState) {
            is ExportImportState.Success -> {
                snackbarHostState.showSnackbar("Data imported successfully")
                viewModel.clearImportState()
            }
            is ExportImportState.Error -> {
                snackbarHostState.showSnackbar("Import failed: ${state.message}")
                viewModel.clearImportState()
            }
            else -> {}
        }
    }

    if (showImportConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportConfirmation() },
            title = { Text("Replace all data?") },
            text = { Text("This will delete all existing data and replace it with the imported data. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport() }) {
                    Text("Replace")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportConfirmation() }) {
                    Text("Cancel")
                }
            },
        )
    }

    val isLoading = exportState is ExportImportState.Loading || importState is ExportImportState.Loading

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            ListItem(
                headlineContent = { Text("Export Data") },
                supportingContent = { Text("Save all data as a JSON file") },
                leadingContent = {
                    if (exportState is ExportImportState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Filled.FileUpload, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading) {
                        val authority = "${context.packageName}.fileprovider"
                        viewModel.exportData(
                            cacheDir = context.cacheDir,
                            fileProviderAuthority = authority,
                            getUriForFile = { file ->
                                FileProvider.getUriForFile(context, authority, file)
                            },
                        )
                    },
            )
            ListItem(
                headlineContent = { Text("Import Data") },
                supportingContent = { Text("Restore data from a JSON backup") },
                leadingContent = {
                    if (importState is ExportImportState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Filled.FileDownload, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading) {
                        importFilePicker.launch(arrayOf("application/json"))
                    },
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
