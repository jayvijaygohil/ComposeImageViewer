package com.jayvijay.composeimageviewer.imageviewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Holds zoom and pan state for a zoomable image.
 *
 * @param scope The coroutine scope for launching animations.
 * @param minUserScale The minimum allowed user-controlled scale.
 * @param maxUserScale The maximum allowed user-controlled scale.
 * @param doubleTapZoomStep The additive step for double-tap zoom gesture.
 * @param defaultUserScale The default user-controlled scale to restore to.
 * @param initialScale The initial scale to restore to.
 * @param initialOffset The initial offset to restore to.
 */
class ZoomableImageState(
    private val scope: CoroutineScope,
    private val animationDispatcher: CoroutineDispatcher,
    private val minUserScale: Float,
    private val maxUserScale: Float,
    private val doubleTapZoomStep: Float,
    private val defaultUserScale: Float,
    initialScale: Float = 1f,
    initialOffset: Offset = Offset.Zero,
) {
    // Animatable states for smooth transitions of scale and offset.
    val scale = Animatable(initialScale)
    val offset = Animatable(initialOffset, Offset.VectorConverter)

    // Raw size states updated from the UI.
    var containerSize by mutableStateOf(IntSize.Zero)
    var imageSize by mutableStateOf(IntSize.Zero)

    // The base scale to fit the image within the container, preserving aspect ratio.
    private val baseScale by derivedStateOf {
        if (imageSize == IntSize.Zero || containerSize == IntSize.Zero) {
            1f
        } else {
            min(
                containerSize.width / imageSize.width.toFloat(),
                containerSize.height / imageSize.height.toFloat()
            )
        }
    }

    /**
     * The final scale applied to the graphics layer, combining base scale and user scale.
     */
    val graphicsLayerScale by derivedStateOf { baseScale * scale.value }

    /**
     * Animates the user-controlled scale by a given [delta].
     */
    private fun animateUserScaleBy(delta: Float) {
        scope.launch(animationDispatcher) {
            val targetScale = (scale.value + delta).coerceIn(minUserScale, maxUserScale)
            scale.animateTo(targetScale, animationSpec = spring())
            // After zooming, ensure the offset is within the new valid bounds.
            offset.snapTo(clampOffset(offset.value))
        }
    }

    /**
     * Animates zoom-in by a given [step].
     */
    fun zoomIn(step: Float) = animateUserScaleBy(step)

    /**
     * Animates zoom-out by a given [step].
     */
    fun zoomOut(step: Float) = animateUserScaleBy(-step)

    /**
     * Animates the scale and offset back to their initial state (1f scale, zero offset).
     */
    fun reset() {
        scope.launch(animationDispatcher) {
            scale.animateTo(defaultUserScale, animationSpec = spring())
            offset.animateTo(Offset.Zero, animationSpec = spring())
        }
    }

    /**
     * Handles a double-tap gesture, toggling zoom at the [tapPosition].
     */
    fun onDoubleTap(tapPosition: Offset) {
        scope.launch(animationDispatcher) {
            val currentScale = scale.value
            val targetScale = if (
                abs(currentScale - maxUserScale) < ZoomScaleTolerance
            ) {
                minUserScale
            } else {
                (currentScale + doubleTapZoomStep).coerceAtMost(maxUserScale)
            }

            // Animate to the new state
            if (
                abs(targetScale - minUserScale) < ZoomScaleTolerance
            ) {
                // If resetting, animate to center
                scale.animateTo(minUserScale, animationSpec = spring())
                offset.animateTo(Offset.Zero, animationSpec = spring())
            } else {
                // To zoom in on the `tapPosition`, we calculate a new offset. The logic is
                // derived from the principle that the tapped point should remain stationary on
                // the screen after the zoom.
                // Let C = container center, P = tap position, O_old = old offset.
                // The new offset O_new is calculated as:
                // O_new = (O_old - (P - C)) * scaleChange + (P - C)
                val deltaFromCenter = tapPosition - containerCenter()
                val scaleChange = targetScale / currentScale
                val newOffset = (offset.value - deltaFromCenter) * scaleChange + deltaFromCenter

                scale.animateTo(targetScale, animationSpec = spring())
                offset.animateTo(clampOffset(newOffset), animationSpec = spring())
            }
        }
    }

    /**
     * Handles a transform gesture (pan and zoom), updating the state.
     * This logic couples panning and zooming to keep the gesture's
     * centroid stationary during the transformation.
     *
     * @param centroid The center point of the transform gesture.
     * @param pan The change in offset from the gesture.
     * @param zoom The multiplicative change in scale from the gesture.
     */
    fun onTransform(centroid: Offset, pan: Offset, zoom: Float) {
        scope.launch(animationDispatcher) {
            val oldScale = scale.value
            val newScale = (oldScale * zoom).coerceIn(minUserScale, maxUserScale)
            val zoomRatio = newScale / oldScale

            // Calculate the new offset by applying pan and adjusting for the zoom
            // around the gesture's centroid. This keeps the point under the centroid stationary.
            // newOffset = (offset + pan) * zoomRatio + (centroid - containerCenter) * (1 - zoomRatio)
            val deltaFromCenter = centroid - containerCenter()
            val newOffset = deltaFromCenter * (1 - zoomRatio) + offset.value * zoomRatio + pan

            scale.snapTo(newScale)
            offset.snapTo(clampOffset(newOffset))
        }
    }

    /**
     * Clamps the given [offset] to ensure the image does not pan beyond its boundaries.
     */
    private fun clampOffset(offset: Offset): Offset {
        val totalScale = graphicsLayerScale

        val scaledImageWidth = imageSize.width * totalScale
        val scaledImageHeight = imageSize.height * totalScale

        val maxOffsetX = max(0f, (scaledImageWidth - containerSize.width) / 2f)
        val maxOffsetY = max(0f, (scaledImageHeight - containerSize.height) / 2f)

        return Offset(
            x = offset.x.coerceIn(-maxOffsetX, maxOffsetX),
            y = offset.y.coerceIn(-maxOffsetY, maxOffsetY),
        )
    }

    private fun containerCenter(): Offset =
        Offset(containerSize.width / 2f, containerSize.height / 2f)
}

/**
 * A factory composable to create and remember a [ZoomableImageState] instance.
 *
 * @param minUserScale The minimum allowed user-controlled scale.
 * @param maxUserScale The maximum allowed user-controlled scale.
 * @param doubleTapZoomStep The additive step to apply on a double tap.
 * @param defaultUserScale The default user-controlled scale to restore to.
 */
@Composable
fun rememberZoomableImageState(
    minUserScale: Float,
    maxUserScale: Float,
    doubleTapZoomStep: Float,
    defaultUserScale: Float = minUserScale,
    animationDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
): ZoomableImageState {
    val scope = rememberCoroutineScope()
    val saver = remember(
        minUserScale,
        maxUserScale,
        defaultUserScale,
        doubleTapZoomStep,
        animationDispatcher,
    ) {
        Saver<ZoomableImageState, List<Float>>(
            save = {
                listOf(it.scale.value, it.offset.value.x, it.offset.value.y)
            },
            restore = {
                ZoomableImageState(
                    scope = scope,
                    animationDispatcher = animationDispatcher,
                    minUserScale = minUserScale,
                    maxUserScale = maxUserScale,
                    defaultUserScale = defaultUserScale,
                    doubleTapZoomStep = doubleTapZoomStep,
                    initialScale = it[0],
                    initialOffset = Offset(it[1], it[2]),
                )
            }
        )
    }

    return rememberSaveable(saver = saver) {
        ZoomableImageState(
            scope = scope,
            animationDispatcher = animationDispatcher,
            minUserScale = minUserScale,
            maxUserScale = maxUserScale,
            defaultUserScale = defaultUserScale,
            doubleTapZoomStep = doubleTapZoomStep,
        )
    }
}
