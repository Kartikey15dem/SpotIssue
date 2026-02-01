package org.example.project.auth.presentation.screens

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.LottieAnimation as LottieComposeAnimation
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Android-only Lottie animation composable.
 *
 * Usage:
 * LottieAnimation(R.raw.my_lottie, modifier = Modifier.size(120.dp))
 */
@Composable
fun LottieAnimation(
    @RawRes rawRes: Int,
    modifier: Modifier = Modifier,
    loop: Boolean = true,
    speed: Float = 1f
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (composition == null) {
            CircularProgressIndicator()
        } else {
            LottieComposeAnimation(
                composition = composition,
                iterations = if (loop) LottieConstants.IterateForever else 1,
                speed = speed,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
