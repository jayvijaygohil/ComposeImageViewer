package com.jayvijay.composeimageviewer.imageviewer

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state for the zoomable image screen.
 */
sealed interface ImageViewerUiState {
    data object Empty : ImageViewerUiState

    data class Content(
        val heading: String,
        val bitmap: ImageBitmap,
        val contentDescription: String?,
    ) : ImageViewerUiState
}

/**
 * ViewModel for managing zoomable image data across configuration changes.
 * Stores the bitmap to avoid re-fetching after rotation.
 */
class ZoomableImageViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ImageViewerUiState>(ImageViewerUiState.Empty)
    val uiState: StateFlow<ImageViewerUiState> = _uiState.asStateFlow()

    /**
     * Sets the image data to be displayed on the zoomable screen.
     *
     * @param bitmap The [ImageBitmap] to display.
     * @param contentDescription The accessibility content description for the image.
     * @param heading The title to display above the image.
     */
    fun showImage(bitmap: ImageBitmap, contentDescription: String?, heading: String) {
        _uiState.value = ImageViewerUiState.Content(
            heading = heading,
            bitmap = bitmap,
            contentDescription = contentDescription,
        )
    }

    /**
     * Clears the stored image data. This should be called when the user dismisses the zoom
     * view to release the bitmap from memory and ensure it isn't displayed again accidentally.
     */
    fun clearImage() {
        _uiState.value = ImageViewerUiState.Empty
    }

    override fun onCleared() {
        super.onCleared()
        _uiState.value = ImageViewerUiState.Empty
    }
}
