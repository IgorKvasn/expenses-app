package com.example.expensetracker.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.repository.NotificationPreferenceRepository
import com.example.expensetracker.domain.usecase.ExportImportJsonUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

sealed interface ExportImportState {
    data object Idle : ExportImportState
    data object Loading : ExportImportState
    data class Success(val fileUri: Uri? = null) : ExportImportState
    data class Error(val message: String) : ExportImportState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportImportJsonUseCase: ExportImportJsonUseCase,
    private val notificationPreferenceRepository: NotificationPreferenceRepository,
) : ViewModel() {

    private val _exportState = MutableStateFlow<ExportImportState>(ExportImportState.Idle)
    val exportState: StateFlow<ExportImportState> = _exportState

    private val _importState = MutableStateFlow<ExportImportState>(ExportImportState.Idle)
    val importState: StateFlow<ExportImportState> = _importState

    private val _showImportConfirmation = MutableStateFlow(false)
    val showImportConfirmation: StateFlow<Boolean> = _showImportConfirmation

    private var pendingImportJson: String? = null

    val isMonthlyBalanceNotificationEnabled: StateFlow<Boolean> =
        notificationPreferenceRepository.isMonthlyBalanceNotificationEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setMonthlyBalanceNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferenceRepository.setMonthlyBalanceNotificationEnabled(enabled)
        }
    }

    fun exportData(cacheDir: File, fileProviderAuthority: String, getUriForFile: (File) -> Uri) {
        viewModelScope.launch {
            _exportState.value = ExportImportState.Loading
            try {
                val jsonString = exportImportJsonUseCase.exportToJson()
                val fileName = "expense_tracker_backup_${LocalDate.now()}.json"
                val file = File(cacheDir, fileName)
                file.writeText(jsonString)
                val uri = getUriForFile(file)
                _exportState.value = ExportImportState.Success(fileUri = uri)
            } catch (e: Exception) {
                _exportState.value = ExportImportState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun onImportFileSelected(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            try {
                val jsonString = contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: throw IllegalArgumentException("Could not read file")
                pendingImportJson = jsonString
                _showImportConfirmation.value = true
            } catch (e: Exception) {
                _importState.value = ExportImportState.Error(e.message ?: "Could not read file")
            }
        }
    }

    fun confirmImport() {
        val jsonString = pendingImportJson ?: return
        _showImportConfirmation.value = false
        viewModelScope.launch {
            _importState.value = ExportImportState.Loading
            try {
                exportImportJsonUseCase.importFromJson(jsonString)
                _importState.value = ExportImportState.Success()
                pendingImportJson = null
            } catch (e: Exception) {
                _importState.value = ExportImportState.Error(e.message ?: "Import failed")
            }
        }
    }

    fun dismissImportConfirmation() {
        _showImportConfirmation.value = false
        pendingImportJson = null
    }

    fun clearExportState() {
        _exportState.value = ExportImportState.Idle
    }

    fun clearImportState() {
        _importState.value = ExportImportState.Idle
    }
}
