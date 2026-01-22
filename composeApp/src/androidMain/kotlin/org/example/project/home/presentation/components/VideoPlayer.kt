package org.example.project.home.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private const val TAG = "VideoPlayer"

@OptIn(UnstableApi::class)
@Composable
 fun VideoPlayer(
    url: String,
    modifier: Modifier,
    autoPlay: Boolean,
    showControls: Boolean,
    onVideoEnd: () -> Unit,
    isFullscreen: Boolean,
    videoAspectRatio: Float?
) {
    val context = LocalContext.current
    val config = LocalConfiguration.current

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = autoPlay
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    // Listener that logs playback errors
    val playerListener = remember {
        object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // Log full exception and useful fields
                Log.e(
                    TAG,
                    "ExoPlayer error: code=${error.errorCode} name=${error.errorCodeName} message=${error.message}",
                    error
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onVideoEnd()
                }
            }
        }
    }

    // Attach/detach listener
    DisposableEffect(player) {
        player.addListener(playerListener)
        onDispose {
            player.removeListener(playerListener)
        }
    }

    DisposableEffect(autoPlay) {
        player.playWhenReady = autoPlay
        onDispose {}
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    val finalModifier = if (isFullscreen) {
        val screenWidth = config.screenWidthDp
        val screenHeight = config.screenHeightDp
        val isLandscape = screenWidth > screenHeight
        val aspect = videoAspectRatio ?: (16f / 9f)
        val isVideoLandscape = aspect > 1f

        when {
            !isVideoLandscape && !isLandscape -> modifier.fillMaxWidth().aspectRatio(aspect)
            isVideoLandscape && isLandscape -> modifier.fillMaxSize()
            !isVideoLandscape && isLandscape -> modifier.fillMaxHeight().aspectRatio(aspect)
            else -> modifier.fillMaxWidth().aspectRatio(aspect)
        }
    } else {
        modifier
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = showControls
                controllerAutoShow = false
            }
        },
        update = { view ->
            view.useController = showControls
        },
        modifier = finalModifier
    )
}

