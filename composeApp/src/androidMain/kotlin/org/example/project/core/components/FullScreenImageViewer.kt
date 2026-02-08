package org.example.project.core.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil3.compose.SubcomposeAsyncImage


@Composable
fun FullScreenImageViewer(url: String) {
    // 1. State for Zoom (Scale) and Pan (Offset)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // 2. Track the size of the image container to calculate bounds
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 3. Measure the container size
            .onSizeChanged { size = it }
            // 4. Handle Gestures
            .pointerInput(Unit) {
                // DETECT PINCH AND DRAG
                detectTransformGestures { _, pan, zoom, _ ->
                    // A. Calculate New Scale
                    val newScale = (scale * zoom).coerceIn(1f, 4f) // Min 1x, Max 4x
                    scale = newScale

                    // B. Calculate New Offset (Pan)
                    // We only allow panning if we are zoomed in
                    if (scale > 1f) {
                        // Calculate the maximum drag limit based on how much we are zoomed in
                        val maxX = (size.width * (scale - 1)) / 2
                        val maxY = (size.height * (scale - 1)) / 2

                        val newOffset = offset + pan

                        // Coerce the offset so we can't drag the image off-screen
                        offset = Offset(
                            x = newOffset.x.coerceIn(-maxX, maxX),
                            y = newOffset.y.coerceIn(-maxY, maxY)
                        )
                    } else {
                        // If zoomed out, reset offset to center
                        offset = Offset.Zero
                    }
                }
            }
            .pointerInput(Unit) {
                // DETECT DOUBLE TAP
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            // Reset to normal
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            // Zoom in to 2x
                            scale = 2f
                        }
                    }
                )
            }
    ) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = "Full screen image",
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                // 5. Apply the transformations to the Image Layer
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            loading = {
                OverlayLoadingSpinner()
            },
            contentScale = ContentScale.Fit
        )
    }
}