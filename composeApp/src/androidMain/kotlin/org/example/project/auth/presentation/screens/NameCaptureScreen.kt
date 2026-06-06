package org.example.project.auth.presentation.screens

import androidx.compose.material3.Snackbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.auth.presentation.viewmodel.AuthEffect
import org.example.project.auth.presentation.viewmodel.NameCaptureEffect
import org.example.project.auth.presentation.viewmodel.NameCaptureIntent
import org.example.project.auth.presentation.viewmodel.NameCaptureUiState
import org.example.project.auth.presentation.viewmodel.NameCaptureViewModel
import org.koin.compose.viewmodel.koinViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import coil3.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import org.example.project.R
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTypography
import androidx.compose.foundation.clickable
import kotlinx.coroutines.flow.collectLatest

@Composable
fun NameCaptureScreen(
    viewModel: NameCaptureViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.handleIntent(NameCaptureIntent.CaptureFromCameraClicked)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.handleIntent(NameCaptureIntent.CameraImageCaptured(success))
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.handleIntent(NameCaptureIntent.ImageUrlChanged(uri.toString()))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NameCaptureEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                NameCaptureEffect.ShowImagePicker -> {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                is NameCaptureEffect.ShowCamera -> {
                    cameraLauncher.launch(effect.uri)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF323232),
                    contentColor = Color.White,
                    actionColor = Color(0xFF4A6CF7)
                )
            }
        },
        containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NameCaptureContent(
                uiState = uiState,
                onAction = { intent -> viewModel.handleIntent(intent) },
                onCameraClick = {
                    val permission = Manifest.permission.CAMERA
                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.handleIntent(NameCaptureIntent.CaptureFromCameraClicked)
                    } else {
                        cameraPermissionLauncher.launch(permission)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameCaptureContent(
    uiState: NameCaptureUiState,
    onAction: (NameCaptureIntent) -> Unit,
    onCameraClick: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val isLoading = uiState.isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "Complete Your Profile",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Profile picture with edit icon
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Profile Image
            if (uiState.isLoadingImage) {
                CircularProgressIndicator(
                    modifier = Modifier.size(120.dp),
                    color = Color(0xFF4A6CF7)
                )
            } else {
                AsyncImage(
                    model = uiState.imageUrl.ifBlank { R.drawable.ic_user_avatar },
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color(0xFF4A6CF7), CircleShape)
                        .clickable { onAction(NameCaptureIntent.PickFromGalleryClicked) },
                    contentScale = ContentScale.Crop
                )
            }

            // Edit icon overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4A6CF7))
                    .clickable { onCameraClick() }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = "Edit Picture",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Image source buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onAction(NameCaptureIntent.PickFromGalleryClicked) },
                enabled = !uiState.isLoadingImage
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Gallery")
            }

            OutlinedButton(
                onClick = onCameraClick,
                enabled = !uiState.isLoadingImage
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_report), // Using ic_report as camera placeholder if ic_camera doesn't exist
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Camera")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Tell us the name by which you want to post issues",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))


        OutlinedTextField(
            value = uiState.name,
            onValueChange = { onAction(NameCaptureIntent.NameChanged(it)) },
            label = { Text("Full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.clearFocus() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4A6CF7),
                focusedLabelColor = Color(0xFF4A6CF7)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                onAction(NameCaptureIntent.SubmitClicked)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6CF7)),
            shape = RoundedCornerShape(8.dp),
            enabled = uiState.name.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Get Started",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}