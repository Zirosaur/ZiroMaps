package com.zirosaur.ziromaps.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zirosaur.ziromaps.data.repository.MapRepository
import com.zirosaur.ziromaps.domain.NotGeoPdfException
import com.zirosaur.ziromaps.model.MapProjectItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapLibraryViewModel @Inject constructor(
    private val mapRepository: MapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapLibraryUiState(isLoading = true))
    val uiState: StateFlow<MapLibraryUiState> = _uiState.asStateFlow()

    init {
        observeMaps()
    }

    private fun observeMaps() {
        mapRepository.getMapProjects()
            .onEach { projects ->
                _uiState.update { current ->
                    current.copy(
                        maps = projects,
                        isLoading = false,
                        loadingMessage = null
                    )
                }
            }
            .catch { throwable ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = throwable.localizedMessage ?: "Gagal memuat daftar peta."
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun importMap(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Menyalin dan memvalidasi file GeoPDF…",
                    errorMessage = null
                )
            }

            val result = mapRepository.importMapFromUri(uri)
            result.fold(
                onSuccess = { importedMap ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            userMessage = "Peta \"${importedMap.name}\" berhasil diimpor."
                        )
                    }
                },
                onFailure = { exception ->
                    if (exception is NotGeoPdfException) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadingMessage = null,
                                notGeoPdfDialogVisible = true
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadingMessage = null,
                                errorMessage = exception.localizedMessage
                                    ?: "Gagal mengimpor file PDF."
                            )
                        }
                    }
                }
            )
        }
    }

    fun confirmDeleteMap(item: MapProjectItem) {
        _uiState.update { it.copy(deleteConfirmationProject = item) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(deleteConfirmationProject = null) }
    }

    fun deleteConfirmedMap() {
        val target = _uiState.value.deleteConfirmationProject ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(deleteConfirmationProject = null) }
            try {
                mapRepository.deleteMapProject(target.id)
                _uiState.update {
                    it.copy(userMessage = "Peta \"${target.name}\" telah dihapus.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Gagal menghapus peta: ${e.localizedMessage}")
                }
            }
        }
    }

    fun dismissNotGeoPdfDialog() {
        _uiState.update { it.copy(notGeoPdfDialogVisible = false) }
    }

    fun dismissErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
