package org.example.project.profile.presentation.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTypography
import org.example.project.profile.presentation.viewmodel.EditProfileIntent
import org.example.project.profile.presentation.viewmodel.EditProfileSideEffect
import org.example.project.profile.presentation.viewmodel.EditProfileState
import org.example.project.profile.presentation.viewmodel.EditProfileViewModel
import org.example.project.R
import org.koin.compose.viewmodel.koinViewModel
import org.example.project.theme.IssueSpotTheme

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import org.example.project.profile.presentation.viewmodel.EmailChangeStep
import org.example.project.utils.media.MediaCompressorUtil
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import androidx.compose.ui.draw.rotate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    viewModel: EditProfileViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraFilePath by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onIntent(EditProfileIntent.CaptureFromCameraClicked)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Camera permission denied")
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            coroutineScope.launch {
                cameraFilePath?.let {
                    val compressedFile = MediaCompressorUtil.compressImage(context, "file://$it")
                    viewModel.onIntent(EditProfileIntent.ImageUrlChanged(compressedFile?.absolutePath ?: it))
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val compressedFile = MediaCompressorUtil.compressImage(context, uri.toString())
                if (compressedFile != null) {
                    viewModel.onIntent(EditProfileIntent.ImageUrlChanged(compressedFile.absolutePath))
                } else {
                    viewModel.onIntent(EditProfileIntent.ImageUrlChanged(uri.toString()))
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collectLatest { effect ->
            when (effect) {
                is EditProfileSideEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is EditProfileSideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                EditProfileSideEffect.ProfileSaved ,EditProfileSideEffect.BackPreseed -> {
                    onNavigateBack()
                }
                EditProfileSideEffect.LogoutSuccess -> {
                    onNavigateBack()
                }
                EditProfileSideEffect.ShowImagePicker -> {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                EditProfileSideEffect.ShowCamera -> {
                    val file = File(context.filesDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    cameraUri = uri
                    cameraFilePath = file.absolutePath
                    cameraLauncher.launch(uri)
                }

                else -> {}
            }
        }
    }

    if (state.showEmailChangeDialog) {
        EmailChangeDialog(
            state = state,
            onIntent = viewModel::onIntent
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", style = IssueSpotTypography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Back",
                            tint = IssueSpotColors.OnSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onIntent(EditProfileIntent.LogoutClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_logout),
                            contentDescription = "Logout",
                            tint = IssueSpotColors.Error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IssueSpotColors.Surface,
                    titleContentColor = IssueSpotColors.OnSurface
                )
            )
        },
        snackbarHost = { 
            SnackbarHost(snackbarHostState) { data ->
                androidx.compose.material3.Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF323232),
                    contentColor = Color.White,
                    actionColor = Color(0xFF4A6CF7)
                )
            } 
        },
        containerColor = Color.White
    ) { paddingValues ->
        EditProfileContent(
            modifier = modifier.padding(paddingValues).background(Color.White),
            state = state,
            onIntent = viewModel::onIntent,
            onCameraClick = {
                val permission = Manifest.permission.CAMERA
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.onIntent(EditProfileIntent.CaptureFromCameraClicked)
                } else {
                    cameraPermissionLauncher.launch(permission)
                }
            }
        )
    }
}

@Composable
fun EditProfileContent(
    modifier: Modifier = Modifier,
    state: EditProfileState,
    onIntent: (EditProfileIntent) -> Unit,
    onCameraClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.isSaving) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = IssueSpotColors.Primary
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Profile Picture",
            style = IssueSpotTypography.titleMedium,
            color = IssueSpotColors.OnBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoadingImage) {
                CircularProgressIndicator(
                    modifier = Modifier.size(120.dp),
                    color = IssueSpotColors.Primary
                )
            } else {
                AsyncImage(
                    model = state.imageUrl.ifBlank { null },
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(3.dp, IssueSpotColors.Primary, CircleShape)
                        .clickable { onIntent(EditProfileIntent.PickFromGalleryClicked) },
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_user_avatar),
                    fallback = painterResource(R.drawable.ic_user_avatar)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(IssueSpotColors.Primary)
                    .clickable {
                        onCameraClick()
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = "Edit Picture",
                    tint = IssueSpotColors.OnPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onIntent(EditProfileIntent.PickFromGalleryClicked) },
                enabled = !state.isLoadingImage
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
                enabled = !state.isLoadingImage
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_video),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Camera")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Full Name
        Text(
            text = "Full Name",
            style = IssueSpotTypography.titleSmall,
            color = IssueSpotColors.OnBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.name,
            onValueChange = { onIntent(EditProfileIntent.NameChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = IssueSpotColors.Outline,
                focusedBorderColor = IssueSpotColors.Primary
            )
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Email Address",
            style = IssueSpotTypography.titleSmall,
            color = IssueSpotColors.OnBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.email,
                onValueChange = { onIntent(EditProfileIntent.EmailChanged(it)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = IssueSpotColors.Outline,
                    focusedBorderColor = IssueSpotColors.Primary
                )
            )

            Button(
                onClick = { onIntent(EditProfileIntent.ShowEmailChangeDialogClicked) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IssueSpotColors.Primary.copy(alpha = 0.1f),
                    contentColor = IssueSpotColors.Primary
                )
            ) {
                Text("Update", style = IssueSpotTypography.labelLarge)
            }
        }

        Spacer(Modifier.height(24.dp))


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onIntent(EditProfileIntent.ResetClicked) },
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Reset")
            }

            Button(
                onClick = { onIntent(EditProfileIntent.SaveChangesClicked) },
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving && state.name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IssueSpotColors.Primary,
                    contentColor = IssueSpotColors.OnPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = IssueSpotColors.OnPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text("Save Changes")
            }
        }

    }
}

@Composable
fun EmailChangeDialog(
    state: EditProfileState,
    onIntent: (EditProfileIntent) -> Unit
) {
    var otpCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!state.isEmailUpdating) onIntent(EditProfileIntent.DismissEmailChangeDialog) },
        title = {
            Text(
                text = if (state.emailChangeStep == EmailChangeStep.Request) "Change Email" else "Verify Email",
                style = IssueSpotTypography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.emailChangeStep == EmailChangeStep.Request) {
                    Text(
                        "Enter your new email address. We will send a verification code to it.",
                        style = IssueSpotTypography.bodySmall
                    )
                    OutlinedTextField(
                        value = state.newEmail,
                        onValueChange = { onIntent(EditProfileIntent.NewEmailChanged(it)) },
                        placeholder = { Text("New Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !state.isEmailUpdating
                    )
                } else {
                    Text(
                        "Enter the 6-digit code sent to ${state.newEmail}",
                        style = IssueSpotTypography.bodySmall
                    )
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 6) otpCode = it },
                        placeholder = { Text("6-digit code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !state.isEmailUpdating
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (state.emailChangeStep == EmailChangeStep.Request) {
                        onIntent(EditProfileIntent.RequestEmailChangeClicked)
                    } else {
                        onIntent(EditProfileIntent.VerifyEmailChangeClicked(otpCode))
                    }
                },
                enabled = !state.isEmailUpdating && (if (state.emailChangeStep == EmailChangeStep.Request) state.newEmail.isNotBlank() else otpCode.length == 6),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isEmailUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (state.emailChangeStep == EmailChangeStep.Request) "Send OTP" else "Verify")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onIntent(EditProfileIntent.DismissEmailChangeDialog) },
                enabled = !state.isEmailUpdating,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        containerColor = IssueSpotColors.Surface,
        titleContentColor = IssueSpotColors.OnSurface,
        textContentColor = IssueSpotColors.OnSurface
    )
}

@Preview
@Composable
fun EditProfileContentPreview() {
    IssueSpotTheme {
        EditProfileContent(
            state = EditProfileState(
                name = "John Doe",
                imageUrl = "",
                isLoadingImage = false,
                isSaving = false
            ),
            onIntent = {}
        )
    }
}
