# Compose Image Viewer

A versatile and highly-configurable image viewer for Jetpack Compose that supports smooth pinch-to-zoom, double-tap-to-zoom, and panning gestures. Built with Material 3, it provides a modern and intuitive user experience for image interaction.

## Features

*   **Smooth Gestures:** Seamlessly pan, pinch-to-zoom, and double-tap-to-zoom.
*   **Fluid Animations:** Utilizes physics-based spring animations for a natural and responsive feel during transformations.
*   **UI Controls:** Includes a floating toolbar with buttons to zoom in, zoom out, and reset the view to its default state.
*   **State Restoration:** Preserves the current zoom and pan state across configuration changes (e.g., screen rotation) using a `ViewModel`.
*   **Highly Customizable:** Easily configure minimum/maximum zoom levels, gesture behavior, and animation properties through a simple configuration class.
*   **Accessibility:** Content descriptions for UI controls and live region announcements for zoom level changes.

## Demo

The main component, `ZoomableImagePreview`, provides an out-of-the-box UI with a floating control bar at the bottom.

<img src="assets/Screenshot.png" alt="Compose Image Viewer demo" width="320" />

## Implementation

Follow these steps to integrate the image viewer into your app.

### 1. State Management

Use the `ZoomableImageViewModel` to manage the state of the image viewer. This ensures the image and its zoom/pan state are preserved across configuration changes.

```kotlin
// In your screen or activity
val viewModel: ZoomableImageViewModel = viewModel()
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

### 2. Load the Image

Load your image as an `ImageBitmap`. The example below uses Coil to load an image from a URL. Ensure you load the bitmap on a background thread.

```kotlin
// Example function to load a remote image
private suspend fun loadRemoteImageBitmap(
    context: android.content.Context,
    url: String,
    dispatcher: CoroutineContext = Dispatchers.IO,
): Result<ImageBitmap> = withContext(dispatcher) {
    runCatching {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false) // Required for zoomable image manipulation
            .build()

        val result = context.imageLoader.execute(request)
        result.image?.toBitmap()?.asImageBitmap()
            ?: error("Image decode failed")
    }
}
```

### 3. Display the Viewer

Use a `LaunchedEffect` to load the image and update the `ViewModel`. Then, render `ZoomableImagePreview` based on the `uiState`.

```kotlin
@Composable
private fun MainScreen(viewModel: ZoomableImageViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Load the image when the screen is first composed
    LaunchedEffect(Unit) {
        val bitmap = loadRemoteImageBitmap(
            context = context,
            url = "https://picsum.photos/1080",
        ).getOrNull() ?: return@LaunchedEffect

        viewModel.showImage(
            bitmap = bitmap,
            contentDescription = "Sample remote image",
            heading = ""
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Compose Image Viewer") })
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is ImageViewerUiState.Content -> {
                ZoomableImagePreview(
                    bitmap = state.bitmap,
                    imageContentDescription = state.contentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
            ImageViewerUiState.Empty -> {
                // Show a loading indicator or placeholder
                Text(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    text = "Loading image..."
                )
            }
        }
    }
}
```

## Customization

You can customize the behavior of the image viewer by passing a `ZoomableImageConfig` object to the `ZoomableImagePreview` composable.

```kotlin
val customConfig = ZoomableImageConfig(
    minZoom = 0.5f,        // Minimum zoom scale (e.g., 50%)
    maxZoom = 5f,          // Maximum zoom scale (e.g., 500%)
    zoomStep = 0.5f,       // Incremental zoom for UI buttons
    enableDoubleTapToZoom = true,
    enableTransformGestures = true,
)

ZoomableImagePreview(
    // ... other parameters
    config = customConfig
)
```

### Configuration Options

| Parameter                 | Type    | Default      | Description                                                    |
| ------------------------- | :------ | :----------- | :------------------------------------------------------------- |
| `minZoom`                 | `Float` | `1f`         | The minimum scale the user can zoom out to.                    |
| `maxZoom`                 | `Float` | `3f`         | The maximum scale the user can zoom in to.                     |
| `defaultZoom`             | `Float` | `minZoom`    | The scale to use when the "reset" button is pressed.           |
| `zoomStep`                | `Float` | `0.25f`      | The amount of zoom applied when the zoom in/out buttons are used. |
| `doubleTapZoomStep`       | `Float` | `0.25f`      | The amount of zoom applied on a double-tap gesture.            |
| `enableDoubleTapToZoom`   | `Boolean` | `true`     | Toggles the double-tap-to-zoom gesture on the image.           |
| `enableTransformGestures` | `Boolean` | `true`     | Toggles pinch-to-zoom and pan gestures.                        |

## Core Components

The library is built on a few key components that can be used independently for more advanced use cases:

*   **`ZoomableImagePreview`**: The primary, high-level composable that includes the image viewer, UI controls, and a heading. This is the recommended component for most use cases.
*   **`ZoomableImage`**: A lower-level composable that focuses purely on displaying a bitmap and handling transform gestures (pan, zoom). It does not include any UI controls.
*   **`ZoomableImageState`**: A state holder that manages the complex logic for zoom, pan, animations, and offset clamping. It is created using `rememberZoomableImageState()`.
*   **`ZoomableImageConfig`**: A data class for providing all configuration settings to the viewer.

## License

This project is licensed under the **GNU General Public License v3.0**. See the `LICENSE.md` file for details.
