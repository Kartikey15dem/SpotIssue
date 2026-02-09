package org.example.project.core.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.example.project.R

@OptIn(UnstableApi::class)
@Composable
fun VideoPreviewPlayer(
    videoUri: String,
    onFullscreenClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAspectRatioAvailable: (Float) -> Unit,
) {
    val context = LocalContext.current

    // --- State ---
    // We trust the Player's state, but we need a local variable to update the UI
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var showPlayButton by remember { mutableStateOf(true) }

    // --- ExoPlayer Init ---
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUri)))
            prepare()
            playWhenReady = false
            // repeatMode = Player.REPEAT_MODE_ONE // Optional: Uncomment if you want auto-looping
            volume = 1f

            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.height > 0) {
                        // Calculate ratio
                        var videoRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                        if(videoRatio < 1f) videoRatio = 1f
                        onAspectRatioAvailable(videoRatio)

                    }
                }
            })
        }
    }

    // --- Sync State with Player ---
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            // This is the CRITICAL fix. It triggers whenever the player actually starts/stops.
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                // If it starts playing, auto-hide the button (optional)
                if (playing) {
                    showPlayButton = false
                }
            }

            // Handle when video finishes naturally
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                    showPlayButton = true // Show the play button so user can restart
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Toggle the center button visibility
                showPlayButton = !showPlayButton
            }
    ) {
        // --- Video Layer ---
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT // Fills the card
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- Center Play/Pause Button ---
        androidx.compose.animation.AnimatedVisibility(
            visible = showPlayButton,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        // If playing, simple pause
                        exoPlayer.pause()
                    } else {
                        // FIX: If video ended, reset to 0 before playing
                        if (exoPlayer.playbackState == Player.STATE_ENDED) {
                            exoPlayer.seekTo(0)
                        }
                        exoPlayer.play()
                    }
                },
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                    contentDescription = "Toggle Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // --- Bottom Controls (Mute + Fullscreen) ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    isMuted = !isMuted
                    exoPlayer.volume = if (isMuted) 0f else 1f
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
                    ),
                    contentDescription = "Mute",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onFullscreenClick,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fullscreen),
                    contentDescription = "Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}