package com.zirosaur.ziromaps.ui.library

import com.zirosaur.ziromaps.model.MapProjectItem

/**
 * Immutable UI state for MapLibraryScreen.
 */
data class MapLibraryUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val maps: List<MapProjectItem> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val notGeoPdfDialogVisible: Boolean = false,
    val deleteConfirmationProject: MapProjectItem? = null,
    val userMessage: String? = null
) {
    val filteredMaps: List<MapProjectItem>
        get() = if (searchQuery.isBlank()) {
            maps
        } else {
            maps.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
        }
}
