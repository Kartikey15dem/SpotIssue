package org.example.project.profile.presentation.screens

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

/**
 * Edit Profile Screen with ViewModel integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    viewModel: EditProfileViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle side effects
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
            }
        }
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IssueSpotColors.Surface,
                    titleContentColor = IssueSpotColors.OnSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = IssueSpotColors.Background
    ) { paddingValues ->
        EditProfileContent(
            modifier = modifier.padding(paddingValues),
            state = state,
            onIntent = viewModel::onIntent
        )
    }
}

/**
 * Content composable for Edit Profile Screen
 */
@Composable
fun EditProfileContent(
    modifier: Modifier = Modifier,
    state: EditProfileState,
    onIntent: (EditProfileIntent) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Loading indicator
        if (state.isSaving) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = IssueSpotColors.Primary
            )
        }

        Spacer(Modifier.height(16.dp))

        // Profile Picture Section
        Text(
            text = "Edit Profile",
            style = IssueSpotTypography.titleMedium,
            color = IssueSpotColors.OnBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(16.dp))

        // Profile picture with edit icon
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Profile Image
            if (state.isLoadingImage) {
                CircularProgressIndicator(
                    modifier = Modifier.size(120.dp),
                    color = IssueSpotColors.Primary
                )
            } else {
                AsyncImage(
                    model = state.imageUrl.ifBlank { "https://via.placeholder.com/150" },
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(3.dp, IssueSpotColors.Primary, CircleShape)
                        .clickable { onIntent(EditProfileIntent.PickFromGalleryClicked) },
                    contentScale = ContentScale.Crop
                )
            }

            // Edit icon overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(IssueSpotColors.Primary)
                    .clickable {
                        // Show dialog to choose between gallery and camera
                        onIntent(EditProfileIntent.PickFromGalleryClicked)
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

        // Image source buttons
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
                onClick = { onIntent(EditProfileIntent.CaptureFromCameraClicked) },
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

        Text(
            text = "Enter a URL for your profile picture",
            style = IssueSpotTypography.bodySmall,
            color = IssueSpotColors.OnSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

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
                unfocusedContainerColor = IssueSpotColors.SurfaceVariant,
                focusedContainerColor = IssueSpotColors.SurfaceVariant,
                unfocusedBorderColor = IssueSpotColors.Outline,
                focusedBorderColor = IssueSpotColors.Primary
            )
        )

        Spacer(Modifier.height(24.dp))

        // Location Section
        Text(
            text = "Location",
            style = IssueSpotTypography.titleSmall,
            color = IssueSpotColors.OnBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(16.dp))

        // Locality/Area
        Text(
            text = "Locality/Area",
            style = IssueSpotTypography.bodyMedium,
            color = IssueSpotColors.OnSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.locality,
            onValueChange = { onIntent(EditProfileIntent.LocalityChanged(it)) },
            placeholder = { Text("e.g., Downtown") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = IssueSpotColors.SurfaceVariant,
                focusedContainerColor = IssueSpotColors.SurfaceVariant,
                unfocusedBorderColor = IssueSpotColors.Outline,
                focusedBorderColor = IssueSpotColors.Primary
            )
        )

        Spacer(Modifier.height(16.dp))

        // District/City
        Text(
            text = "District/City",
            style = IssueSpotTypography.bodyMedium,
            color = IssueSpotColors.OnSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.district,
            onValueChange = { onIntent(EditProfileIntent.DistrictChanged(it)) },
            placeholder = { Text("e.g., Mumbai Central") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = IssueSpotColors.SurfaceVariant,
                focusedContainerColor = IssueSpotColors.SurfaceVariant,
                unfocusedBorderColor = IssueSpotColors.Outline,
                focusedBorderColor = IssueSpotColors.Primary
            )
        )

        Spacer(Modifier.height(16.dp))

        // State/Province
        Text(
            text = "State/Province",
            style = IssueSpotTypography.bodyMedium,
            color = IssueSpotColors.OnSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.state,
            onValueChange = { onIntent(EditProfileIntent.StateChanged(it)) },
            placeholder = { Text("e.g., Maharashtra") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = IssueSpotColors.SurfaceVariant,
                focusedContainerColor = IssueSpotColors.SurfaceVariant,
                unfocusedBorderColor = IssueSpotColors.Outline,
                focusedBorderColor = IssueSpotColors.Primary
            )
        )

        Spacer(Modifier.height(16.dp))

        // Country
        Text(
            text = "Country",
            style = IssueSpotTypography.bodyMedium,
            color = IssueSpotColors.OnSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.country,
            onValueChange = { onIntent(EditProfileIntent.CountryChanged(it)) },
            placeholder = { Text("e.g., India") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = IssueSpotColors.SurfaceVariant,
                focusedContainerColor = IssueSpotColors.SurfaceVariant,
                unfocusedBorderColor = IssueSpotColors.Outline,
                focusedBorderColor = IssueSpotColors.Primary
            )
        )

        Spacer(Modifier.height(32.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Reset Button
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

            // Save Button
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

        Spacer(Modifier.height(24.dp))
    }
}

@Preview
@Composable
fun EditProfileContentPreview() {
    IssueSpotTheme {
        EditProfileContent(
            state = EditProfileState(
                name = "John Doe",
                imageUrl = "",
                locality = "Downtown",
                district = "Mumbai Central",
                state = "Maharashtra",
                country = "India",
                isLoadingImage = false,
                isSaving = false
            ),
            onIntent = {}
        )
    }
}
