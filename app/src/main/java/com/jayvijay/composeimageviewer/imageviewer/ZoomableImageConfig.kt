package com.jayvijay.composeimageviewer.imageviewer

import androidx.compose.runtime.Immutable

/**
 * Holds configuration settings for [ZoomableImagePreview].
 * behavior and available interactions.
 *
 * @param minZoom The minimum scale the user can zoom out to. Defaults to 1f (100%).
 * @param maxZoom The maximum scale the user can zoom in to. Defaults to 3f (300%).
 * @param defaultZoom The scale considered the "reset/default" zoom. By default equals [minZoom].
 * @param zoomStep The incremental amount of zoom applied when the zoom in/out buttons are pressed.
 * @param doubleTapZoomStep The incremental amount amount of zoom applied on a double-tap gesture.
 * @param enableDoubleTapToZoom Toggles the double-tap-to-zoom gesture on the image.
 * @param enableTransformGestures Toggles gestures like pinch-to-zoom and pan.
 */
@Immutable
data class ZoomableImageConfig(
    val minZoom: Float = 1f,
    val maxZoom: Float = 3f,
    val defaultZoom: Float = minZoom,
    val zoomStep: Float = 0.25f,
    val doubleTapZoomStep: Float = 0.25f,
    val enableDoubleTapToZoom: Boolean = true,
    val enableTransformGestures: Boolean = true,
)
