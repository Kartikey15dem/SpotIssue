package org.example.project.createPost.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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
import coil3.compose.AsyncImage
import org.example.project.core.components.LocalOverlayController
import org.example.project.core.components.VideoPreviewPlayer
import org.example.project.home.domain.models.MediaType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CreatePostScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    viewModel: CreatePostViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            // Get actual MIME type from ContentResolver
            val mimeType = context.contentResolver.getType(it)
            viewModel.setVisualMedia(it.toString(), mimeType)
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
@OptIn(ExperimentalFoundationApi::class)
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



// 2. THE BOX
                // 1. STATE VARIABLES
                var boxHeightPx by remember { mutableIntStateOf(0) }
                var cursorYInText by remember { mutableFloatStateOf(0f) }
                var textLayoutResultState by remember { mutableStateOf<TextLayoutResult?>(null) }

// [NEW] Get Keyboard Height dynamically
                val density = LocalDensity.current
                val imeHeightPx = WindowInsets.ime.getBottom(density)

// 2. THE BOX (Container)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .background(Color.Transparent, RoundedCornerShape(12.dp))
                        .border(1.dp, IssueSpotColors.OnSecondaryContainer, RoundedCornerShape(12.dp))
                        .onGloballyPositioned { coordinates ->
                            boxHeightPx = coordinates.size.height
                        }
                ) {
                    val scrollState = rememberScrollState()

                    // 3. CALCULATE SCROLL NEEDED (Live Logic)
                    // We use a derived state to constantly check if the keyboard is covering the cursor
                    val amountToScroll by remember(imeHeightPx, cursorYInText, boxHeightPx, scrollState.value) {
                        derivedStateOf {
                            // A. Where is the cursor visually on screen?
                            val cursorScreenY = cursorYInText - scrollState.value

                            // B. How much space is below the cursor?
                            val distanceToBottom = boxHeightPx - cursorScreenY

                            // C. Compare: Is the keyboard taller than that space?
                            val overlap = imeHeightPx - distanceToBottom

                            // If overlap is positive, we need to scroll that much to reveal the cursor
                            if (overlap > 0) overlap else 0f
                        }
                    }

                    // 4. PERFORM THE SCROLL
                    LaunchedEffect(amountToScroll) {
                        if (amountToScroll > 0) {
                            // "scrollBy" jumps instantly. "animateScrollBy" slides slowly.
                            // Using scrollBy makes it feel like the keyboard pushed it up natively.
                            scrollState.scrollBy(amountToScroll)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            // [IMPORTANT] Add padding at the bottom equal to keyboard height.
                            // This acts as a "buffer" so you can always scroll up high enough.
                            .padding(bottom = with(density) { imeHeightPx.toDp() })
                    ) {
                        // Local wrapper for text + cursor
                        var textFieldValue by remember { mutableStateOf(TextFieldValue(state.description)) }

                        // Sync with ViewModel
                        LaunchedEffect(state.description) {
                            if (textFieldValue.text != state.description) {
                                textFieldValue = textFieldValue.copy(text = state.description)
                            }
                        }

                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                textFieldValue = newValue
                                onIntent(CreatePostIntent.DescriptionChanged(newValue.text))

                                // [FIX] Update cursor position on clicks/typing
                                val layoutResult = textLayoutResultState
                                if (layoutResult != null) {
                                    val cursorIndex = newValue.selection.start
                                    if (cursorIndex <= layoutResult.layoutInput.text.length) {
                                        val cursorRect = layoutResult.getCursorRect(cursorIndex)
                                        cursorYInText = cursorRect.bottom
                                    }
                                }
                            },

                            // [FIX] Update cursor position on layout changes (paste/initial load)
                            onTextLayout = { result ->
                                textLayoutResultState = result
                                val cursorIndex = textFieldValue.selection.start
                                if (cursorIndex <= result.layoutInput.text.length) {
                                    val cursorRect = result.getCursorRect(cursorIndex)
                                    cursorYInText = cursorRect.bottom
                                }
                            },

                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 150.dp)
                                .padding(12.dp),

                            textStyle = IssueSpotTypography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),

                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (textFieldValue.text.isEmpty()) {
                                        Text(
                                            text = "Describe the issue you want to report...",
                                            style = IssueSpotTypography.bodyMedium,
                                            color = IssueSpotColors.OnSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // Media Preview
                        if (state.selectedMediaUri != null) {
                            MediaPreviewContent(
                                mediaUri = state.selectedMediaUri,
                                mediaType = state.selectedMediaType,
                                onRemove = { onIntent(CreatePostIntent.RemoveMedia) }
                            )
                        }
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
            if(state.selectedMediaUri == null) {

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
}

@Composable
fun MediaPreviewContent(
    mediaUri: String,
    mediaType: MediaType?,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val overlayController = LocalOverlayController.current
    // 1. Change main container to Column to stack items vertically
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // 2. Close Button (Appears above the content)
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.End) // Align to the right
                .clip(CircleShape)
                .background(Color.Black)
                .size(28.dp) // Standard touch target size

        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Remove media",
                modifier = Modifier.size(18.dp),
                tint = Color.White // Black Icon
            )
        }
        Spacer(modifier = modifier.height(6.dp))
        // 3. Media Content (Image or Video)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        ) {
            when (mediaType) {
                MediaType.IMAGE -> {
                    AsyncImage(
                        model = mediaUri.toUri(),
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize()
                            .clickable(onClick = {
                                overlayController.show(
                                    type = MediaType.IMAGE,
                                    url = mediaUri
                                )
                            }),
                        contentScale = ContentScale.Crop
                    )
                }
                MediaType.VIDEO -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        VideoPreviewPlayer(
                            videoUri = mediaUri,
                            modifier = Modifier.fillMaxSize(),
                            onFullscreenClick = {
                                // Trigger the global overlay when fullscreen icon is clicked
                                overlayController.show(
                                    type = MediaType.VIDEO,
                                    url = mediaUri
                                )
                            }
                        )
                    }
                }
                else -> {}
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







