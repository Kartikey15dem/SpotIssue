package org.example.project.createPost.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.home.domain.models.PostLevel
import org.example.project.home.presentation.components.PostHeader
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTheme
import org.example.project.theme.IssueSpotTypography
import androidx.compose.ui.res.painterResource
import org.example.project.R

// MVI - Intent
sealed interface CreatePostIntent {
    data object CloseClicked : CreatePostIntent
    data class IssueScopeChanged(val postLevel: PostLevel) : CreatePostIntent
    data class DescriptionChanged(val description: String) : CreatePostIntent
    data object AddPhotoClicked : CreatePostIntent
    data object AddVideoClicked : CreatePostIntent
    data object CancelClicked : CreatePostIntent
    data object PostIssueClicked : CreatePostIntent
}

// MVI - State
data class CreatePostState(
    val userName: String = "John Doe",
    val location: String = "Downtown, Mumbai Central",
    val selectedPostLevel: PostLevel = PostLevel.LOCALITY,
    val description: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showIssueScopeDropdown: Boolean = false
)

// MVI - Side Effects
sealed interface CreatePostSideEffect {
    data object NavigateBack : CreatePostSideEffect
    data object ShowPhotoPicker : CreatePostSideEffect
    data object ShowVideoPicker : CreatePostSideEffect
    data class ShowError(val message: String) : CreatePostSideEffect
    data class PostCreated(val postId: String) : CreatePostSideEffect
}

@Composable
fun CreatePostScreen(
    modifier: Modifier = Modifier,
    initialState: CreatePostState = CreatePostState(),
    onNavigateBack: () -> Unit = {}
) {
    var state by remember { mutableStateOf(initialState) }

    fun handleIntent(intent: CreatePostIntent) {
        when (intent) {
            CreatePostIntent.CloseClicked -> onNavigateBack()
            CreatePostIntent.CancelClicked -> onNavigateBack()
            is CreatePostIntent.IssueScopeChanged -> {
                state = state.copy(selectedPostLevel = intent.postLevel, showIssueScopeDropdown = false)
            }
            is CreatePostIntent.DescriptionChanged -> {
                state = state.copy(description = intent.description)
            }
            CreatePostIntent.AddPhotoClicked -> {
                // TODO: Handle photo selection
            }
            CreatePostIntent.AddVideoClicked -> {
                // TODO: Handle video selection
            }
            CreatePostIntent.PostIssueClicked -> {
                if (state.description.isBlank()) {
                    // Show error
                    state = state.copy(error = "Please describe the issue")
                    return
                }
                // TODO: Handle post submission
                state = state.copy(isLoading = true)
                // Simulate posting
                state = state.copy(isLoading = false)
                onNavigateBack()
            }
        }
    }

    CreatePostScreenContent(
        modifier = modifier,
        state = state,
        onIntent = ::handleIntent
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
