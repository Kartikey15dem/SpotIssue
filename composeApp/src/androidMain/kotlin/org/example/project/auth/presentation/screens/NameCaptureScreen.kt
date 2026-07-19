package org.example.project.auth.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.example.project.R
import org.example.project.core.components.AppErrorDialog
import org.example.project.feature.auth.viewmodel.NameCaptureEffect
import org.example.project.feature.auth.viewmodel.NameCaptureIntent
import org.example.project.feature.auth.viewmodel.NameCaptureUiState
import org.example.project.feature.auth.viewmodel.NameCaptureViewModel
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTheme
import org.example.project.theme.IssueSpotTypography
import org.example.project.utils.media.MediaCompressorUtil
import java.io.File

@Composable
fun NameCaptureScreen(viewModel: NameCaptureViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraFilePath by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                viewModel.handleIntent(NameCaptureIntent.CaptureFromCameraClicked)
            }
        }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
        ) { success ->
            if (success) {
                coroutineScope.launch {
                    cameraFilePath?.let {
                        val compressedFile = MediaCompressorUtil.compressImage(context, "file://$it")
                        viewModel.handleIntent(NameCaptureIntent.ImageUrlChanged(compressedFile?.absolutePath ?: it))
                    }
                }
            }
        }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri: Uri? ->
            if (uri != null) {
                coroutineScope.launch {
                    val compressedFile = MediaCompressorUtil.compressImage(context, uri.toString())
                    if (compressedFile != null) {
                        viewModel.handleIntent(NameCaptureIntent.ImageUrlChanged(compressedFile.absolutePath))
                    } else {
                        viewModel.handleIntent(NameCaptureIntent.ImageUrlChanged(uri.toString()))
                    }
                }
            }
        }

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NameCaptureEffect.ShowDialog -> {
                    errorDialogMessage = effect.message
                }
                NameCaptureEffect.ShowImagePicker -> {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }
                NameCaptureEffect.ShowCamera -> {
                    val file = File(context.filesDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                    val uri =
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file,
                        )
                    cameraUri = uri
                    cameraFilePath = file.absolutePath
                    cameraLauncher.launch(uri)
                }
            }
        }
    }

    errorDialogMessage?.let { message ->
        AppErrorDialog(
            message = message,
            onDismiss = { errorDialogMessage = null },
        )
    }

    Scaffold(
        containerColor = Color.White,
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameCaptureContent(
    uiState: NameCaptureUiState,
    onAction: (NameCaptureIntent) -> Unit,
    onCameraClick: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val isLoading = uiState.isLoading
    val spacing = IssueSpotTheme.spacing
    val shapes = MaterialTheme.shapes

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(IssueSpotColors.Surface)
                .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(spacing.huge))

        Text(
            text = "Complete Your Profile",
            style = IssueSpotTypography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = IssueSpotColors.OnSurface,
        )

        Spacer(modifier = Modifier.height(spacing.large))

        // Profile picture with edit icon
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Profile Image
            if (uiState.isLoadingImage) {
                CircularProgressIndicator(
                    modifier = Modifier.size(120.dp),
                    color = IssueSpotColors.Primary,
                )
            } else {
                AsyncImage(
                    model = uiState.imageUrl.ifBlank { null },
                    contentDescription = "Profile Picture",
                    modifier =
                        Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(3.dp, IssueSpotColors.Primary, CircleShape)
                            .clickable { onAction(NameCaptureIntent.PickFromGalleryClicked) },
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_user_avatar),
                    fallback = painterResource(R.drawable.ic_user_avatar),
                )
            }

            // Edit icon overlay
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(IssueSpotColors.Primary)
                        .clickable { onCameraClick() }
                        .padding(spacing.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = "Edit Picture",
                    tint = IssueSpotColors.OnPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(spacing.medium))

        // Image source buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            OutlinedButton(
                onClick = { onAction(NameCaptureIntent.PickFromGalleryClicked) },
                enabled = !uiState.isLoadingImage,
                shape = shapes.medium,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(spacing.extraSmall))
                Text("Gallery", style = IssueSpotTypography.labelMedium)
            }

            OutlinedButton(
                onClick = onCameraClick,
                enabled = !uiState.isLoadingImage,
                shape = shapes.medium,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_video),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(spacing.extraSmall))
                Text("Camera", style = IssueSpotTypography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(spacing.extraLarge))

        Text(
            text = "Tell us the name by which you want to post issues",
            style = IssueSpotTypography.bodyLarge,
            color = IssueSpotColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(spacing.medium))

        OutlinedTextField(
            value = uiState.name,
            onValueChange = { onAction(NameCaptureIntent.NameChanged(it)) },
            label = { Text("Full name", style = IssueSpotTypography.bodyMedium) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            shape = shapes.medium,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions =
                KeyboardActions(
                    onNext = { focusManager.clearFocus() },
                ),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IssueSpotColors.Primary,
                    focusedLabelColor = IssueSpotColors.Primary,
                    unfocusedBorderColor = IssueSpotColors.Outline,
                ),
        )

        Spacer(modifier = Modifier.height(spacing.large))

        Button(
            onClick = {
                focusManager.clearFocus()
                onAction(NameCaptureIntent.SubmitClicked)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = IssueSpotColors.Primary,
                    contentColor = IssueSpotColors.OnPrimary,
                ),
            shape = shapes.medium,
            enabled = uiState.name.isNotBlank() && !isLoading,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = IssueSpotColors.OnPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = "Get Started",
                    style = IssueSpotTypography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = spacing.extraSmall),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
