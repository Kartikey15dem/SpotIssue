package org.example.project.auth.presentation.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.project.auth.presentation.LocationPermissionHandler
import org.example.project.auth.presentation.viewmodel.LocationFetchViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.example.project.R

/**
 * Platform-specific wrapper that handles permission requests before showing LocationFetchScreen.
 * Android implementation will request location permissions.
 * iOS implementation can handle location authorization.
 */
@Composable
fun LocationFetchScreenWithPermissions(
name: String,
email: String,
onLocationFetched: () -> Unit
) {
    val context = LocalContext.current
    val permissionHandler: LocationPermissionHandler = koinInject()

    // Request permission when screen is first composed
    LaunchedEffect(Unit) {
        val activity = context as? Activity
        if (activity != null && !permissionHandler.hasLocationPermission()) {
            permissionHandler.requestLocationPermission(activity)
        }
    }

    // Show the actual location fetch screen
    LocationFetchScreen(
        name = name,
        email = email,
        onLocationFetched = onLocationFetched
    )
}

@Composable
fun LocationFetchScreen(
    name: String,
    email: String,
    onLocationFetched: () -> Unit,
    viewModel: LocationFetchViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Set name and email when screen is created
    LaunchedEffect(name, email) {
        viewModel.setUserData(name, email)
    }

    // ViewModel will start its own initialization (e.g., fetch location) when created.
    LaunchedEffect(Unit) {
        viewModel.startLocationFlow()
    }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            delay(1500) // Show completion animation for a bit
            onLocationFetched()
        }
    }

    LocationFetchContent(
        uiState = uiState,
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LocationFetchContent(
    uiState: LocationFetchUiState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animation placeholder for location fetching - almost full width
        AnimatedContent(
            targetState = uiState.currentStep,
            transitionSpec = {
                (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
            }
        ) { step ->
            when (step) {
                LocationFetchStep.FETCHING -> {
                    LocationFetchingAnimation()
                }
                LocationFetchStep.COMPLETED -> {
                    LocationCompletedAnimation(uiState.address)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Status text
        AnimatedContent(
            targetState = uiState.currentStep,
            transitionSpec = {
                fadeIn() with fadeOut()
            }
        ) { step ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (step) {
                        LocationFetchStep.FETCHING -> "Fetching your location..."
                        LocationFetchStep.COMPLETED -> "Location fetched successfully!"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                if (step == LocationFetchStep.COMPLETED && uiState.address != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.address,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = uiState.error,
                fontSize = 14.sp,
                color = Color.Red,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LocationFetchingAnimation() {
    // Lottie animation for location fetching - almost full width
    LottieAnimation(
        rawRes = R.raw.location_fetch,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(1f),
        loop = true,
        speed = 1f
    )
}

@Composable
private fun LocationCompletedAnimation(address: String?) {
    // Lottie animation for completion - almost full width
    LottieAnimation(
        rawRes = R.raw.success,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(1f),
        loop = false,
        speed = 1f
    )
}

enum class LocationFetchStep {
    FETCHING,
    COMPLETED
}

data class LocationFetchUiState(
    val currentStep: LocationFetchStep = LocationFetchStep.FETCHING,
    val address: String? = null,
    val isCompleted: Boolean = false,
    val error: String? = null
)
