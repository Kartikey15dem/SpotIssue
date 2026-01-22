package org.example.project.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.example.project.R

@Composable
fun FullscreenVideoOverlay(
    url: String,
    videoAspectRatio: Float?,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Video player with clickable modifier - tapping video toggles controls
        VideoPlayer(
            url = url,
            modifier = Modifier,
            autoPlay = true,
            showControls = false,
            isFullscreen = true,
            videoAspectRatio = videoAspectRatio,
            onVideoEnd ={}
        )

        // Close button overlay - on top of everything, NOT affected by video clicks
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x99000000))
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Close fullscreen",
                tint = Color.White
            )
        }
    }
}
