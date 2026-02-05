package org.example.project.createPost.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.flow.collectLatest
import org.example.project.R
import org.example.project.createPost.presentation.viewmodel.CreatePostIntent
import org.example.project.createPost.presentation.viewmodel.CreatePostSideEffect
import org.example.project.createPost.presentation.viewmodel.CreatePostState
import org.example.project.createPost.presentation.viewmodel.CreatePostViewModel
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTheme
import org.example.project.theme.IssueSpotTypography
import org.koin.compose.viewmodel.koinViewModel
import androidx.core.net.toUri
import org.example.project.home.domain.models.MediaType
import org.example.project.home.presentation.components.PostLevelChip

@Composable
fun CreatePostScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    viewModel: CreatePostViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setVisualMedia(it.toString(), null)
        }
    }

    // PDF/Document picker launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setDocumentUrl(it.toString())
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collectLatest { effect ->
            when (effect) {
                CreatePostSideEffect.NavigateBack -> onNavigateBack()
                CreatePostSideEffect.ShowMediaPicker -> {
                    mediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                }
                CreatePostSideEffect.ShowPdfPicker -> {
                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                }
                is CreatePostSideEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is CreatePostSideEffect.PostCreated -> {
                    snackbarHostState.showSnackbar("Post created successfully!")
                }
            }
        }
    }

    CreatePostScreenContent(
        modifier = modifier,
        state = state,
        onIntent = viewModel::onIntent
    )
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreenContent(
    modifier: Modifier = Modifier,
    state: CreatePostState,
    onIntent: (CreatePostIntent) -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = IssueSpotColors.Surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onIntent(CreatePostIntent.CloseClicked) }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "Close",
                                tint = IssueSpotColors.OnBackground
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(IssueSpotColors.SurfaceVariant),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_user_avatar),
                                contentDescription = "s avatar",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column {
                            Row(
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Current User",
                                    style = IssueSpotTypography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )

                            }
                            Row(
                                modifier = Modifier.padding(start = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_location_on),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = IssueSpotColors.OnSurfaceVariant
                                )
                                Text(
                                    modifier = Modifier.padding(start = 4.dp),
                                    text = "Tere Bhai ki Road",
                                    style = IssueSpotTypography.bodySmall,
                                    color = IssueSpotColors.OnSurfaceVariant
                                )
                            }
                        }
                    }

                    // Right-aligned: Post button
                    Button(
                        onClick = { onIntent(CreatePostIntent.PostIssueClicked) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IssueSpotColors.PostButtonBackground,
                            contentColor = IssueSpotColors.PostButtonText
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 0.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text("Post", style = IssueSpotTypography.bodyLarge)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Combined Description + Media Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false) // Takes remaining space
                        .background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = IssueSpotColors.OnSecondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(), // This still helps with keyboard detection
                        reverseLayout = true // <--- THIS is the "Anchor to Bottom" constraint
                    ) {
                        item {
                            if (state.selectedMediaUri != null) {
                            MediaPreviewContent(
                                mediaUri = state.selectedMediaUri,
                                mediaType = state.selectedMediaType,
                                onRemove = { onIntent(CreatePostIntent.RemoveMedia) }
                            )
                        }
                            // Description TextField
                            OutlinedTextField(
                                value = state.description,
                                onValueChange = { onIntent(CreatePostIntent.DescriptionChanged(it)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 150.dp),
                                placeholder = {
                                    Text(
                                        text = "Describe the issue you want to report...",
                                        color = IssueSpotColors.OnSurfaceVariant
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(0.dp),
                                textStyle = IssueSpotTypography.bodyMedium,
                                isError = state.error != null
                            )

                            // Media Preview (seamlessly below text)

                        }
                    }

                    if (state.error != null) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            style = IssueSpotTypography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            }

            // STICKY BOTTOM BUTTONS - Attached to keyboard (Icon-only, transparent)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .background(Color.Transparent)
                    .padding(
                        bottom = 24.dp,
                        start = 2.dp,
                        top = 2.dp,
                        end = 24.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                // Media Icon Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = IssueSpotColors.OnSurfaceVariant.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { onIntent(CreatePostIntent.AddMediaClicked) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_photo),
                            contentDescription = "Add Photo or Video",
                            modifier = Modifier.size(24.dp),
                            tint = IssueSpotColors.OnSurfaceVariant
                        )
                    }
                }

                // PDF Icon Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = IssueSpotColors.OnSurfaceVariant.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { onIntent(CreatePostIntent.AddPdfClicked) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "Add PDF",
                            modifier = Modifier.size(24.dp),
                            tint = IssueSpotColors.OnSurfaceVariant
                        )
                    }
                }
            }


        }
    }
}

@Composable
fun MediaPreviewContent(
    mediaUri: String,
    mediaType: MediaType?,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        when (mediaType) {
            MediaType.IMAGE -> {
                Image(
                    painter = rememberAsyncImagePainter(model = mediaUri.toUri()),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            IssueSpotColors.OnSecondaryContainer,
                            RoundedCornerShape(8.dp)
                        ),
                    contentScale = ContentScale.Fit
                )
            }
            MediaType.VIDEO -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            IssueSpotColors.OnSecondaryContainer,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_video),
                        contentDescription = "Video Selected",
                        modifier = Modifier.size(64.dp),
                        tint = IssueSpotColors.Primary
                    )
                }
            }
            else -> {}
        }

        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .size(40.dp)
                .background(
                    IssueSpotColors.Surface.copy(alpha = 0.8f),
                    CircleShape
                )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Remove media",
                modifier = Modifier.size(24.dp),
                tint = IssueSpotColors.OnBackground
            )
        }
    }
}


//@Composable
//fun CreatePostScreenContent(
//    modifier: Modifier = Modifier,
//    state: CreatePostState,
//    onIntent: (CreatePostIntent) -> Unit = {}
//) {
//    Surface(
//        modifier = modifier.fillMaxWidth(),
//        color = IssueSpotColors.Surface
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(20.dp)
//        ) {
//            // Header with close button
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "Create New Issue Post",
//                    style = IssueSpotTypography.headlineLarge,
//                    fontWeight = FontWeight.Bold
//                )
//                IconButton(
//                    onClick = { onIntent(CreatePostIntent.CloseClicked) }
//                ) {
//                    Icon(
//                        painter = painterResource(R.drawable.ic_close),
//                        contentDescription = "Close",
//                        tint = IssueSpotColors.OnBackground
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // User info header
//            PostHeader(
//                userName = state.userName,
//                timeAgo = null, // No time for new post
//                postLevel = state.selectedPostLevel,
//                location = state.location
//            )
//
//            Spacer(modifier = Modifier.height(20.dp))
//
//            // Description TextField
//            OutlinedTextField(
//                value = state.description,
//                onValueChange = { onIntent(CreatePostIntent.DescriptionChanged(it)) },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(200.dp),
//                placeholder = {
//                    Text(
//                        text = "Describe the issue you want to report...",
//                        color = IssueSpotColors.OnSurfaceVariant
//                    )
//                },
//                colors = OutlinedTextFieldDefaults.colors(
//                    focusedContainerColor = IssueSpotColors.Background,
//                    unfocusedContainerColor = IssueSpotColors.Background,
//                    focusedBorderColor = IssueSpotColors.Primary,
//                    unfocusedBorderColor = IssueSpotColors.OnSecondaryContainer
//                ),
//                shape = RoundedCornerShape(12.dp),
//                textStyle = IssueSpotTypography.bodyMedium,
//                isError = state.error != null
//            )
//
//            if (state.error != null) {
//                Text(
//                    text = state.error,
//                    color = MaterialTheme.colorScheme.error,
//                    style = IssueSpotTypography.bodySmall,
//                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
//                )
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            // Media Preview
//            if (state.selectedMediaUri != null) {
//                MediaPreview(
//                    mediaUri = state.selectedMediaUri,
//                    mediaType = state.selectedMediaType,
//                    onRemove = { onIntent(CreatePostIntent.RemoveMedia) }
//                )
//                Spacer(modifier = Modifier.height(12.dp))
//            }
//
//            // Add Photo and Video buttons
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                OutlinedButton(
//                    onClick = { onIntent(CreatePostIntent.AddPhotoClicked) },
//                    colors = ButtonDefaults.outlinedButtonColors(
//                        containerColor = IssueSpotColors.Surface
//                    )
//                ) {
//                    Icon(
//                        painter = painterResource(R.drawable.ic_photo),
//                        contentDescription = "Add Photo",
//                        modifier = Modifier.size(16.dp)
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        text = "Add Photo",
//                        style = IssueSpotTypography.bodyMedium,
//                        fontWeight = FontWeight.Bold
//                    )
//                }
//
//                OutlinedButton(
//                    onClick = { onIntent(CreatePostIntent.AddVideoClicked) },
//                    colors = ButtonDefaults.outlinedButtonColors(
//                        containerColor = IssueSpotColors.Surface
//                    )
//                ) {
//                    Icon(
//                        painter = painterResource(R.drawable.ic_video),
//                        contentDescription = "Add Video",
//                        modifier = Modifier.size(16.dp)
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(
//                        text = "Add Video",
//                        style = IssueSpotTypography.bodyMedium,
//                        fontWeight = FontWeight.Bold
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Cancel and Post buttons
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                OutlinedButton(
//                    onClick = { onIntent(CreatePostIntent.CancelClicked) },
//                    modifier = Modifier.weight(1f),
//                    colors = ButtonDefaults.outlinedButtonColors(
//                        containerColor = IssueSpotColors.Surface
//                    )
//                ) {
//                    Text(
//                        text = "Cancel",
//                        style = IssueSpotTypography.bodyLarge,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                }
//
//                Button(
//                    onClick = { onIntent(CreatePostIntent.PostIssueClicked) },
//                    modifier = Modifier.weight(1f),
//                    enabled = !state.isLoading,
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = IssueSpotColors.PostButtonBackground,
//                        contentColor = IssueSpotColors.PostButtonText
//                    )
//                ) {
//                    if (state.isLoading) {
//                        CircularProgressIndicator(
//                            modifier = Modifier.size(20.dp),
//                            color = IssueSpotColors.OnPrimary,
//                            strokeWidth = 2.dp
//                        )
//                    } else {
//                        Text(
//                            text = "Post Issue",
//                            style = IssueSpotTypography.bodyLarge,
//                            fontWeight = FontWeight.Bold
//                        )
//                    }
//                }
//            }
//        }
//    }
//}

@Preview
@Composable
fun CreatePostScreenPreview() {
    IssueSpotTheme {
        CreatePostScreenContent(
            state = CreatePostState()
        )
    }
}


