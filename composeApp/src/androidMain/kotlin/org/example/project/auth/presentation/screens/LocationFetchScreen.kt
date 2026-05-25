package org.example.project.auth.presentation.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import org.example.project.auth.presentation.viewmodel.LocationFetchEffect
import org.example.project.auth.presentation.viewmodel.LocationFetchIntent
import org.example.project.auth.presentation.viewmodel.LocationFetchStep
import org.example.project.auth.presentation.viewmodel.LocationFetchUiState
import org.example.project.auth.presentation.viewmodel.LocationFetchViewModel
import org.example.project.utils.location.LocationPermissionHandler
import org.koin.compose.koinInject
import org.example.project.R
import org.koin.compose.viewmodel.koinViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun LocationFetchScreenWithPermissions(
    onLocationFetched: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionHandler: LocationPermissionHandler = koinInject()

    val viewModel: LocationFetchViewModel = koinViewModel()

    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    var hasRequestedInitialPermission by rememberSaveable { mutableStateOf(false) }

    fun isGpsEnabled() = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    fun evaluateHardwareState() {
        val activity = context as? Activity ?: return

        if (!permissionHandler.hasLocationPermission()) {
            if (permissionHandler.shouldShowRequestPermissionRationale(activity)) {
                viewModel.handleIntent(LocationFetchIntent.ShowRationale)
            } else {
                viewModel.handleIntent(LocationFetchIntent.PermissionDenied)
            }
        } else if (!isGpsEnabled()) {
            viewModel.handleIntent(LocationFetchIntent.GpsDisabled)
        } else {
            viewModel.handleIntent(LocationFetchIntent.StartLocationFlow)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val activity = context as? Activity

                if (activity != null && !hasRequestedInitialPermission && !permissionHandler.hasLocationPermission()) {
                    hasRequestedInitialPermission = true
                    permissionHandler.requestLocationPermission(activity)
                } else {
                    evaluateHardwareState()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LocationFetchScreen(
        onLocationFetched = onLocationFetched,
        viewModel = viewModel,
        onOpenAppSettings = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        },
        onPromptGpsSettings = {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    )
}

@Composable
fun LocationFetchScreen(
    onLocationFetched: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onPromptGpsSettings: () -> Unit,
    viewModel: LocationFetchViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LocationFetchEffect.NavigateToNextScreen -> {
                    if (uiState.isCompleted) delay(1500)
                    onLocationFetched()
                }
                is LocationFetchEffect.OpenAppSettings -> onOpenAppSettings()
                is LocationFetchEffect.PromptGpsSettings -> onPromptGpsSettings()
            }
        }
    }

    LocationFetchContent(
        uiState = uiState,
        onIntent = { viewModel.handleIntent(it) }
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LocationFetchContent(
    uiState: LocationFetchUiState,
    onIntent: (LocationFetchIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = uiState.currentStep,
            transitionSpec = {
                (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
            }
        ) { step ->
            when (step) {
                LocationFetchStep.FETCHING -> LocationFetchingAnimation()
                LocationFetchStep.COMPLETED -> LocationCompletedAnimation(uiState.address)
                LocationFetchStep.ERROR -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .aspectRatio(1f)
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {

                        Image(
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp),
                            painter = painterResource(id = R.drawable.location_not_available),
                            contentDescription = "",
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.currentStep != LocationFetchStep.ERROR) {
            Text(
                text = when (uiState.currentStep) {
                    LocationFetchStep.FETCHING -> "Fetching your location..."
                    LocationFetchStep.COMPLETED -> "Location fetched successfully!"
                    else -> ""
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.currentStep == LocationFetchStep.COMPLETED && uiState.address != null) {
            Text(
                text = uiState.address,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        } else if (uiState.currentStep == LocationFetchStep.ERROR && uiState.errorState != null) {

            Text(
                text = uiState.errorState.message,
                fontSize = 15.sp,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onIntent(LocationFetchIntent.ActionClicked) },
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6CF7)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = uiState.errorState.primaryButtonText,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            if (uiState.errorState.showSecondaryRetry) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onIntent(LocationFetchIntent.RetryClicked) },
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("I've turned it on, Retry", color = Color(0xFF4A6CF7))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Continue without location",
                color = Color(0xFF4A6CF7),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { onIntent(LocationFetchIntent.ContinueWithoutLocation) }
                    .padding(8.dp)
            )
        }
    }
}



@Composable
private fun LocationFetchingAnimation() {
    // Load the composition directly from the URL
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Url("https://lottie.host/4c31b4f6-857d-419d-97e5-7c722e5c2e99/EhLlK9bqgJ.lottie")
    )

    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 1f,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(1f)
    )
}

@Composable
private fun LocationCompletedAnimation(address: String?) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Url("https://lottie.host/17c7befc-f9f2-4e6e-a977-b7c9f5455f17/2HFHUsSkt1.lottie")
    )

    LottieAnimation(
        composition = composition,
        iterations = 1,
        speed = 1f,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(1f)
    )
}