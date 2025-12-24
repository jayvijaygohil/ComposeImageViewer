package com.jayvijay.composeimageviewer.imageviewer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage

/**
 * Displays an image with zoom and pan gestures.
 *
 * Use separate `pointerInput` modifiers for different gestures.
 * This keeps gesture detectors independent in Compose.
 *
 * @param bitmap The image to display.
 * @param zoomState The state holder for zoom and pan.
 * @param contentDescription The accessibility description for the image.
 * @param enableDoubleTapToZoom Toggles the double-tap-to-zoom gesture.
 * @param enableTransform Toggles the transform gesture (pan and pinch-zoom).
 * @param modifier the compose modifier for this component.
 */
@Composable
fun ZoomableImage(
    bitmap: ImageBitmap,
    zoomState: ZoomableImageState,
    contentDescription: String?,
    enableDoubleTapToZoom: Boolean,
    enableTransform: Boolean,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(bitmap) {
        zoomState.imageSize = IntSize(bitmap.width, bitmap.height)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { zoomState.containerSize = it }
            // A separate pointerInput for each gesture detector is the correct approach
            // to allow them to operate concurrently and independently.
            .pointerInput(enableTransform) {
                if (enableTransform) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        zoomState.onTransform(centroid, pan, zoom)
                    }
                }
            }
            .pointerInput(enableDoubleTapToZoom) {
                if (enableDoubleTapToZoom) {
                    detectTapGestures(
                        onDoubleTap = { tapPosition ->
                            zoomState.onDoubleTap(tapPosition)
                        }
                    )
                }
            }
    ) {
        AsyncImage(
            model = bitmap.asAndroidBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = zoomState.graphicsLayerScale
                    scaleY = zoomState.graphicsLayerScale
                    translationX = zoomState.offset.value.x
                    translationY = zoomState.offset.value.y
                },
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit
        )
    }
}
