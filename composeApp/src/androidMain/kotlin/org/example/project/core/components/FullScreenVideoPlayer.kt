package org.example.project.core.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.example.project.R // Make sure to import your R file

@OptIn(UnstableApi::class)
@Composable
fun FullScreenVideoPlayer(url: String) {
    val context = LocalContext.current

    // State for Mute Button
    var isMuted by remember { mutableStateOf(false) }

    // 1. Initialize ExoPlayer
    val exoPlayer =
        remember {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                volume = if (isMuted) 0f else 1f
                prepare()
                playWhenReady = true
            }
        }

    // 2. Manage Lifecycle (Clean up when closed)
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // 3. UI Implementation
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Embed the Android View (PlayerView)
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    // useController = true (This is default, so standard controls will show)
                    setShowNextButton(false)
                    setShowPreviousButton(false)

                    layoutParams =
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // 4. Loading Spinner and Volume Sync logic
        var isBuffering by remember { mutableStateOf(true) }
        DisposableEffect(exoPlayer) {
            val listener =
                object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        isBuffering = (state == Player.STATE_BUFFERING)
                    }

                /* WHY WE OBSERVE VOLUME HERE:
                 * If the user taps the device's physical volume buttons or interacts with
                 * ExoPlayer's default UI controls, the actual volume of the video changes.
                 * By listening here, we ensure our custom `isMuted` Compose state (and
                 * thus the UI icon) is always 100% in sync with what the user actually hears.
                 */
                    override fun onVolumeChanged(volume: Float) {
                        isMuted = volume == 0f
                    }
                }
            exoPlayer.addListener(listener)
            onDispose { exoPlayer.removeListener(listener) }
        }

        if (isBuffering) {
            OverlayLoadingSpinner()
        }

        // ---------------------------------------------------------
        // 5. CUSTOM MUTE / UNMUTE BUTTON (Overlay)
        // ---------------------------------------------------------
        IconButton(
            onClick = {
                isMuted = !isMuted
                exoPlayer.volume = if (isMuted) 0f else 1f
            },
            modifier =
                Modifier
                    .align(Alignment.TopEnd) // Place at Top-Right
                    .statusBarsPadding()
                    .padding(end = 6.dp) // Add some margin
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape) // Semi-transparent background
                    .size(36.dp), // Size of the touch target
        ) {
            Icon(
                // Use your own drawable resources here
                painter =
                    painterResource(
                        id = if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up,
                    ),
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = Color.White,
            )
        }
    }
}
