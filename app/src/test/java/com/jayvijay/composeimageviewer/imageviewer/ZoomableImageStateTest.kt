@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)

package com.jayvijay.composeimageviewer.imageviewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TestMonotonicFrameClock
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ZoomableImageStateTest {

    private lateinit var testScope: TestScope
    private lateinit var testClock: TestMonotonicFrameClock
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var imageZoomState: ZoomableImageState

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        testClock = TestMonotonicFrameClock(
            testScope,
            16_000_000L
        )
        imageZoomState = ZoomableImageState(
            scope = TestScope(testDispatcher + testClock),
            animationDispatcher = testDispatcher,
            minUserScale = 1f,
            maxUserScale = 3f,
            doubleTapZoomStep = 0.5f,
            defaultUserScale = 1f,
            initialScale = 1f,
            initialOffset = Offset.Zero
        )
    }

    @Test
    fun `should initialize with default values when created`() {
        assertEquals(1f, imageZoomState.scale.value)
        assertEquals(Offset.Zero, imageZoomState.offset.value)
        assertEquals(IntSize.Zero, imageZoomState.containerSize)
        assertEquals(IntSize.Zero, imageZoomState.imageSize)
    }

    @Test
    fun `should calculate graphics layer scale correctly when base scale changes`() = runTest {
        imageZoomState.containerSize = IntSize(400, 600)
        imageZoomState.imageSize = IntSize(800, 1200)

        assertEquals(0.5f, imageZoomState.graphicsLayerScale)
    }

    @Test
    fun `should zoom in by step when zoomIn is called`() = testScope.runTest {
        val initialScale = imageZoomState.scale.value
        val zoomStep = 0.5f

        imageZoomState.zoomIn(zoomStep)
        advanceUntilIdle()

        assertEquals(initialScale + zoomStep, imageZoomState.scale.value)
    }

    @Test
    fun `should zoom out by step when zoomOut is called`() = testScope.runTest {
        imageZoomState.scale.snapTo(2f)
        val initialScale = imageZoomState.scale.value
        val zoomStep = 0.5f

        imageZoomState.zoomOut(zoomStep)
        advanceUntilIdle()

        assertEquals(initialScale - zoomStep, imageZoomState.scale.value)
    }

    @Test
    fun `should not exceed max scale when zooming in`() = testScope.runTest {
        imageZoomState.scale.snapTo(2.8f)

        imageZoomState.zoomIn(0.5f)
        advanceUntilIdle()

        assertEquals(3f, imageZoomState.scale.value)
    }

    @Test
    fun `should not go below min scale when zooming out`() = testScope.runTest {
        imageZoomState.scale.snapTo(1.2f)

        imageZoomState.zoomOut(0.5f)
        advanceUntilIdle()

        assertEquals(1f, imageZoomState.scale.value)
    }

    @Test
    fun `should reset to default scale and zero offset when reset is called`() = testScope.runTest {
        imageZoomState.scale.snapTo(2.5f)
        imageZoomState.offset.snapTo(Offset(50f, 100f))

        imageZoomState.reset()
        advanceUntilIdle()

        assertEquals(1f, imageZoomState.scale.value)
        assertEquals(Offset.Zero, imageZoomState.offset.value)
    }

    @Test
    fun `should toggle zoom on double tap when at max scale`() = testScope.runTest {
        imageZoomState.containerSize = IntSize(400, 600)
        imageZoomState.imageSize = IntSize(800, 1200)
        imageZoomState.scale.snapTo(3f)
        val tapPosition = Offset(200f, 300f)

        imageZoomState.onDoubleTap(tapPosition)
        advanceUntilIdle()

        assertEquals(1f, imageZoomState.scale.value)
        assertEquals(Offset.Zero, imageZoomState.offset.value)
    }

    @Test
    fun `should zoom in on double tap when not at max scale`() = testScope.runTest {
        imageZoomState.containerSize = IntSize(400, 600)
        imageZoomState.imageSize = IntSize(800, 1200)
        val initialScale = 1.5f
        imageZoomState.scale.snapTo(initialScale)
        val tapPosition = Offset(200f, 300f)

        imageZoomState.onDoubleTap(tapPosition)
        advanceUntilIdle()

        assertEquals(initialScale + 0.5f, imageZoomState.scale.value)
    }

    @Test
    fun `should handle transform gesture with pan and zoom`() = testScope.runTest {
        imageZoomState.containerSize = IntSize(400, 600)
        imageZoomState.imageSize = IntSize(800, 1200)
        val centroid = Offset(200f, 300f)
        val pan = Offset(10f, 20f)
        val zoom = 1.5f

        imageZoomState.onTransform(centroid, pan, zoom)
        advanceUntilIdle()

        assertEquals(1.5f, imageZoomState.scale.value)
        assertNotEquals(Offset.Zero, imageZoomState.offset.value)
    }

    @Test
    fun `should clamp offset when panning beyond boundaries`() = testScope.runTest {
        imageZoomState.containerSize = IntSize(400, 600)
        imageZoomState.imageSize = IntSize(400, 600)
        imageZoomState.scale.snapTo(1f)

        imageZoomState.onTransform(
            centroid = Offset(200f, 300f),
            pan = Offset(100f, 100f),
            zoom = 1f
        )
        advanceUntilIdle()

        assertEquals(Offset.Zero, imageZoomState.offset.value)
    }

    @Test
    fun `should handle zero size gracefully`() = testScope.runTest {
        imageZoomState.containerSize = IntSize.Zero
        imageZoomState.imageSize = IntSize.Zero

        imageZoomState.zoomIn(0.5f)
        imageZoomState.onDoubleTap(Offset(100f, 100f))
        imageZoomState.onTransform(Offset.Zero, Offset(10f, 10f), 1.5f)
        advanceUntilIdle()

        assertTrue(imageZoomState.scale.value in 1f..3f)
        assertEquals(imageZoomState.scale.value, imageZoomState.graphicsLayerScale)
        assertEquals(Offset.Zero, imageZoomState.offset.value)
    }

    @Test
    fun `should maintain centroid position during zoom gesture`() = testScope.runTest {
        imageZoomState.containerSize = IntSize(400, 600)
        imageZoomState.imageSize = IntSize(800, 1200)
        val centroid = Offset(300f, 400f)

        imageZoomState.onTransform(centroid, Offset.Zero, 2f)
        advanceUntilIdle()

        assertEquals(2f, imageZoomState.scale.value)
        assertNotEquals(Offset.Zero, imageZoomState.offset.value)
    }

    @Test
    fun `should respect scale boundaries during transform`() = testScope.runTest {
        imageZoomState.scale.snapTo(2.5f)

        imageZoomState.onTransform(Offset(200f, 300f), Offset.Zero, 2f)
        advanceUntilIdle()

        assertEquals(3f, imageZoomState.scale.value)
    }

    @Test
    fun `should handle multiple rapid transformations`() = testScope.runTest {
        imageZoomState.containerSize = IntSize(400, 600)
        imageZoomState.imageSize = IntSize(800, 1200)

        repeat(5) {
            imageZoomState.onTransform(
                centroid = Offset(200f, 300f),
                pan = Offset(5f, 5f),
                zoom = 1.1f
            )
        }
        advanceUntilIdle()

        assertTrue(imageZoomState.scale.value > 1f)
        assertNotEquals(Offset.Zero, imageZoomState.offset.value)
    }
}
