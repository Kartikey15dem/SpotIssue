package org.example.project.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.SubcomposeAsyncImage
import org.example.project.R
import org.example.project.home.domain.models.MediaType

@Composable
fun OverlayProvider(
    content: @Composable () -> Unit
) {
    val overlayController = remember { OverlayController() }

    CompositionLocalProvider(LocalOverlayController provides overlayController) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Main App Content
            content()

            // 2. The Overlay
            // We use standard Kotlin "let" to safely unwrap the nullable state
            overlayController.currentOverlay?.let { data ->

                // The Scrim (Dark background)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .zIndex(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Render content based on Enum
                    when (data.type) {
                        MediaType.IMAGE -> FullScreenImageViewer(url = data.url)
                        MediaType.VIDEO -> FullScreenVideoPlayer(url = data.url)
                        MediaType.PDF -> {}
                    }
                    IconButton(
                        onClick = { overlayController.hide() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(start = 6.dp)// avoid system bar overlap // distance from edges
                            .zIndex(2f)                         // ensure it's above the image // minimum recommended touch target
                            .clip(CircleShape)
                            .background(Color.Black)
                            .size(36.dp)

                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Remove media",
                            modifier = Modifier.size(28.dp),
                            tint = Color.White // Black Icon
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun OverlayLoadingSpinner() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}
