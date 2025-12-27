@file:OptIn(ExperimentalMaterial3Api::class)

package com.jayvijay.composeimageviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.jayvijay.composeimageviewer.imageviewer.ImageViewerUiState
import com.jayvijay.composeimageviewer.imageviewer.ZoomableImagePreview
import com.jayvijay.composeimageviewer.imageviewer.ZoomableImageViewModel
import com.jayvijay.composeimageviewer.ui.theme.ComposeImageViewerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeImageViewerTheme {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen(
    viewModel: ZoomableImageViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val bitmap = loadRemoteImageBitmap(
            context = context,
            url = MainScreenDefaults.imageUrl,
        ).getOrNull() ?: return@LaunchedEffect

        viewModel.showImage(
            bitmap = bitmap,
            contentDescription = MainScreenDefaults.imageDescription,
            heading = "",
        )
    }

    MainScreenContent(
        uiState = uiState,
    )
}

@Composable
private fun MainScreenContent(
    uiState: ImageViewerUiState,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Compose Image Viewer") },
            )
        },
    ) { innerPadding ->
        when (val state = uiState) {
            ImageViewerUiState.Empty -> {
                Text(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    text = "No image available.",
                )
            }
            is ImageViewerUiState.Content -> {
                ZoomableImagePreview(
                    heading = "",
                    bitmap = state.bitmap,
                    imageContentDescription = state.contentDescription,
                    zoomInContentDescription = MainScreenDefaults.zoomInLabel,
                    zoomOutContentDescription = MainScreenDefaults.zoomOutLabel,
                    resetZoomContentDescription = MainScreenDefaults.resetZoomLabel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    ComposeImageViewerTheme {
        MainScreenContent(
            uiState = ImageViewerUiState.Empty,
        )
    }
}

private suspend fun loadRemoteImageBitmap(
    context: android.content.Context,
    url: String,
    dispatcher: CoroutineContext = Dispatchers.IO,
): Result<ImageBitmap> = withContext(dispatcher) {
    runCatching {
        val app = context.applicationContext as? ComposeImageViewerApp
            ?: error("Missing ComposeImageViewerApp")
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()

        val result = app.imageLoader.execute(request)
        result.image?.toBitmap()?.asImageBitmap()
            ?: error("Image decode failed")
    }
}

private object MainScreenDefaults {
    const val imageUrl = "https://picsum.photos/1080"
    const val imageDescription = "Sample image"
    const val zoomInLabel = "Zoom in"
    const val zoomOutLabel = "Zoom out"
    const val resetZoomLabel = "Reset zoom"
}
