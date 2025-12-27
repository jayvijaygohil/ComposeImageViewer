@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3ComponentOverrideApi::class,
)

package com.jayvijay.composeimageviewer.imageviewer

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.abs

/**
 * Displays a full-screen, zoomable image preview overlay.
 *
 * @param heading The title text displayed at the top of the preview.
 * @param bitmap The image to be displayed in the preview.
 * @param modifier The modifier to be applied to the dialog's content.
 * @param config Configuration for zoom behavior and gestures.
 * @param imageContentDescription Accessibility description for the displayed image.
 * @param zoomInContentDescription Accessibility description for the zoom-in button.
 * @param zoomOutContentDescription Accessibility description for the zoom-out button.
 * @param resetZoomContentDescription Accessibility description for the reset-zoom button.
 * @param zoomPercentageContentDescription A string resource ID for the zoom percentage's
 * accessibility label. It should be a format string that accepts one argument (e.g., "Zoom is at %s").
 **/
@Composable
fun ZoomableImagePreview(
    heading: String,
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier,
    config: ZoomableImageConfig = ZoomableImageConfig(),
    imageContentDescription: String? = null,
    zoomInContentDescription: String? = null,
    zoomOutContentDescription: String? = null,
    resetZoomContentDescription: String? = null,
    @StringRes zoomPercentageContentDescription: Int? = null,
) {
    var isFirstComposition by remember { mutableStateOf(true) }

    val zoomState = rememberZoomableImageState(
        minUserScale = config.minZoom,
        maxUserScale = config.maxZoom,
        doubleTapZoomStep = config.doubleTapZoomStep,
        defaultUserScale = config.defaultZoom,
    )

    var displayedZoomPercentage by remember {
        mutableIntStateOf((zoomState.scale.value * ZoomPercentMultiplier).toInt())
    }

    LaunchedEffect(zoomState) {
        snapshotFlow { zoomState.scale.value }
            .map { (it * ZoomPercentMultiplier).toInt() }
            .distinctUntilChanged()
            .debounce(ZoomPercentDebounceMs)
            .collectLatest { percentage ->
                displayedZoomPercentage = percentage
                isFirstComposition = false
            }
    }

    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val (headingView, imageViewer, zoomControls) = createRefs()

        ZoomableImage(
            bitmap = bitmap,
            zoomState = zoomState,
            contentDescription = imageContentDescription,
            enableDoubleTapToZoom = config.enableDoubleTapToZoom,
            enableTransform = config.enableTransformGestures,
            modifier = Modifier
                .constrainAs(imageViewer) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)

                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
        )

        heading.takeIf { it.isNotBlank() }?.let {
            Text(
                text = heading,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .constrainAs(headingView) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)

                        width = Dimension.fillToConstraints
                    }
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
            )
        }

        ZoomControls(
            zoomState = zoomState,
            config = config,
            displayedZoomPercentage = displayedZoomPercentage,
            isFirstComposition = isFirstComposition,
            resetZoomContentDescription = resetZoomContentDescription,
            zoomOutContentDescription = zoomOutContentDescription,
            zoomPercentageContentDescription = zoomPercentageContentDescription,
            zoomInContentDescription = zoomInContentDescription,
            modifier = Modifier.constrainAs(zoomControls) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)

                width = Dimension.wrapContent
                height = Dimension.wrapContent
            }
        )
    }
}

/**
 * A self-contained panel with buttons to control the image zoom.
 *
 * @param zoomState The state holder that manages the zoom and pan.
 * @param config The configuration for zoom behavior.
 * @param displayedZoomPercentage The current zoom level to display as text.
 * @param isFirstComposition A flag to prevent the live region from announcing on initial composition.
 * @param resetZoomContentDescription Accessibility description for the reset-zoom button.
 * @param zoomOutContentDescription Accessibility description for the zoom-out button.
 * @param zoomPercentageContentDescription A string resource ID for the zoom percentage's
 * accessibility label. It should be a format string that accepts one argument (e.g., "Zoom is at %s").
 * @param zoomInContentDescription Accessibility description for the zoom-in button.
 * @param modifier The modifier to be applied to the control panel card.
 */
@Composable
private fun ZoomControls(
    zoomState: ZoomableImageState,
    config: ZoomableImageConfig,
    displayedZoomPercentage: Int,
    isFirstComposition: Boolean,
    resetZoomContentDescription: String?,
    zoomOutContentDescription: String?,
    @StringRes zoomPercentageContentDescription: Int?,
    zoomInContentDescription: String?,
    modifier: Modifier = Modifier
) {
    val currentScale = zoomState.scale.value
    val isAtDefault = abs(currentScale - config.defaultZoom) < ZoomScaleTolerance
    val canZoomIn = currentScale < config.maxZoom - ZoomScaleTolerance
    val canZoomOut = currentScale > config.minZoom + ZoomScaleTolerance

    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier.padding(bottom = 12.dp),
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        IconButton(
            onClick = { zoomState.reset() },
            enabled = !isAtDefault,
            modifier = Modifier
                .size(36.dp),
            colors = IconButtonDefaults.iconButtonColors(),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = resetZoomContentDescription.orEmpty()
            )
        }

        Box(
            modifier = Modifier
                .height(24.dp)
                .padding(horizontal = 6.dp)
                .align(Alignment.CenterVertically)
        ) {
            VerticalDivider(modifier = Modifier.height(24.dp))
        }

        IconButton(
            onClick = { zoomState.zoomOut(config.zoomStep) },
            enabled = canZoomOut,
            modifier = Modifier.size(36.dp),
            colors = IconButtonDefaults.iconButtonColors(),
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = zoomOutContentDescription.orEmpty()
            )
        }

        val zoomPercentageModifier = Modifier
            .padding(horizontal = 4.dp)
            .align(Alignment.CenterVertically)
            .then(
                if (zoomPercentageContentDescription != null) {
                    val description = stringResource(
                        zoomPercentageContentDescription,
                        "$displayedZoomPercentage%"
                    )
                    Modifier.semantics {
                        contentDescription = description
                        if (!isFirstComposition) {
                            liveRegion = LiveRegionMode.Polite
                        }
                    }
                } else {
                    Modifier
                }
            )

        Text(
            text = "$displayedZoomPercentage%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = zoomPercentageModifier
        )

        IconButton(
            onClick = { zoomState.zoomIn(config.zoomStep) },
            enabled = canZoomIn,
            modifier = Modifier
                .size(36.dp),
            colors = IconButtonDefaults.iconButtonColors(),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = zoomInContentDescription.orEmpty()
            )
        }
    }
}
