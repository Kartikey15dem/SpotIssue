package org.example.project.createPost.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import org.example.project.R
import org.example.project.createPost.presentation.viewmodel.CreatePostIntent
import org.example.project.createPost.presentation.viewmodel.CreatePostSideEffect
import org.example.project.createPost.presentation.viewmodel.CreatePostState
import org.example.project.createPost.presentation.viewmodel.CreatePostViewModel
import org.example.project.home.presentation.components.PostHeader
import org.example.project.profile.presentation.viewmodel.EditProfileSideEffect
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTheme
import org.example.project.theme.IssueSpotTypography
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CreatePostScreen(
    modifier: Modifier = Modifier,
    initialState: CreatePostState = CreatePostState(),
    onNavigateBack: () -> Unit = {},
    viewModel: CreatePostViewModel = koinViewModel()
) {
    var state by remember { mutableStateOf(initialState) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.sideEffects.collectLatest { effect ->
            when (effect) {
                CreatePostSideEffect.NavigateBack -> onNavigateBack
                CreatePostSideEffect.ShowPhotoPicker -> {}
                CreatePostSideEffect.ShowVideoPicker -> {}
                is CreatePostSideEffect.ShowError ->{
                    snackbarHostState.showSnackbar(effect.message)
                }
                is CreatePostSideEffect.PostCreated -> {
                    // Show success message or navigate
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

@Composable
fun CreatePostScreenContent(
    modifier: Modifier = Modifier,
    state: CreatePostState,
    onIntent: (CreatePostIntent) -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = IssueSpotColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Create New Issue Post",
                    style = IssueSpotTypography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { onIntent(CreatePostIntent.CloseClicked) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Close",
                        tint = IssueSpotColors.OnBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User info header
            PostHeader(
                userName = state.userName,
                timeAgo = null, // No time for new post
                postLevel = state.selectedPostLevel,
                location = state.location
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Description TextField
            OutlinedTextField(
                value = state.description,
                onValueChange = { onIntent(CreatePostIntent.DescriptionChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                placeholder = {
                    Text(
                        text = "Describe the issue you want to report...",
                        color = IssueSpotColors.OnSurfaceVariant
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = IssueSpotColors.Background,
                    unfocusedContainerColor = IssueSpotColors.Background,
                    focusedBorderColor = IssueSpotColors.Primary,
                    unfocusedBorderColor = IssueSpotColors.OnSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = IssueSpotTypography.bodyMedium,
                isError = state.error != null
            )

            if (state.error != null) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = IssueSpotTypography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Add Photo and Video buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onIntent(CreatePostIntent.AddPhotoClicked) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = IssueSpotColors.Surface
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_photo),
                        contentDescription = "Add Photo",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Photo",
                        style = IssueSpotTypography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { onIntent(CreatePostIntent.AddVideoClicked) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = IssueSpotColors.Surface
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_video),
                        contentDescription = "Add Video",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Video",
                        style = IssueSpotTypography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel and Post buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onIntent(CreatePostIntent.CancelClicked) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = IssueSpotColors.Surface
                    )
                ) {
                    Text(
                        text = "Cancel",
                        style = IssueSpotTypography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = { onIntent(CreatePostIntent.PostIssueClicked) },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IssueSpotColors.PostButtonBackground,
                        contentColor = IssueSpotColors.PostButtonText
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = IssueSpotColors.OnPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Post Issue",
                            style = IssueSpotTypography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun CreatePostScreenPreview() {
    IssueSpotTheme {
        CreatePostScreenContent(
            state = CreatePostState()
        )
    }
}
