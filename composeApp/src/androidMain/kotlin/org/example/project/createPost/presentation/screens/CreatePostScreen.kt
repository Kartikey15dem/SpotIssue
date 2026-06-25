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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import org.example.project.R
import org.example.project.feature.createPost.viewmodel.CreatePostIntent
import org.example.project.feature.createPost.viewmodel.CreatePostSideEffect
import org.example.project.feature.createPost.viewmodel.CreatePostState
import org.example.project.feature.createPost.viewmodel.CreatePostViewModel
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTheme
import org.example.project.theme.IssueSpotTypography
import org.koin.compose.viewmodel.koinViewModel
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.example.project.createPost.work.PostUploadWorker
import org.example.project.core.components.LocalOverlayController
import org.example.project.core.components.PdfPreviewContent
import org.example.project.core.components.VideoPreviewPlayer
import org.example.project.core.model.home.MediaType
import org.example.project.core.model.home.SelectedMediaItem

import androidx.compose.material3.Snackbar
import androidx.compose.material3.CircularProgressIndicator

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
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val mediaData = uris.map { uri ->
                val mimeType = context.contentResolver.getType(uri)
                Pair(uri.toString(), mimeType)
            }
            viewModel.setVisualMedia(mediaData)
        }
    }
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
                    // Do nothing
                }
                CreatePostSideEffect.StartBackgroundUpload -> {
                    val workRequest = OneTimeWorkRequestBuilder<PostUploadWorker>().build()
                    WorkManager.getInstance(context).enqueue(workRequest)
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
        containerColor = IssueSpotColors.Surface
    ) { padding ->
        CreatePostScreenContent(
            modifier = modifier.padding(padding),
            state = state,
            onIntent = viewModel::onIntent
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CreatePostScreenContent(
    modifier: Modifier = Modifier,
    state: CreatePostState,
    onIntent: (CreatePostIntent) -> Unit = {}
) {
    val spacing = IssueSpotTheme.spacing
    val shapes = MaterialTheme.shapes

    Surface(
        modifier = modifier.fillMaxSize(),
        color = IssueSpotColors.Surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.smallMedium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {

                    IconButton(
                        onClick = { onIntent(CreatePostIntent.CloseClicked) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close",
                            tint = IssueSpotColors.OnSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(spacing.small))

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(IssueSpotColors.SurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val userImageUrl = state.userImageUrl
                        if (!userImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userImageUrl.toUri(),
                                contentDescription = "avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = painterResource(R.drawable.ic_user_avatar),
                                fallback = painterResource(R.drawable.ic_user_avatar)
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.ic_user_avatar),
                                contentDescription = "avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(spacing.smallMedium))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = state.userName,
                            style = IssueSpotTypography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = state.location,
                            style = IssueSpotTypography.bodySmall,
                            color = IssueSpotColors.OnSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(spacing.smallMedium))

                    Button(
                        onClick = { onIntent(CreatePostIntent.PostIssueClicked) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IssueSpotColors.PostButtonBackground,
                            contentColor = IssueSpotColors.PostButtonText
                        ),
                        shape = shapes.extraLarge,
                        contentPadding = PaddingValues(
                            horizontal = spacing.medium,
                            vertical = spacing.small
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = "Post",
                            style = IssueSpotTypography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = spacing.smallMedium),
                    color = IssueSpotColors.Outline.copy(alpha = 0.15f)
                )

                Spacer(modifier = Modifier.height(spacing.smallMedium))

                var boxHeightPx by remember { mutableIntStateOf(0) }
                var cursorYInText by remember { mutableFloatStateOf(0f) }
                var textLayoutResultState by remember { mutableStateOf<TextLayoutResult?>(null) }
                


                val density = LocalDensity.current
                val imeHeightPx = WindowInsets.ime.getBottom(density)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .background(Color.Transparent, shapes.medium)
                        .border(1.dp, IssueSpotColors.Outline, shapes.medium)
                        .onGloballyPositioned { coordinates ->
                            boxHeightPx = coordinates.size.height
                        }
                ) {
                    val scrollState = rememberScrollState()

                    val amountToScroll by remember(imeHeightPx, cursorYInText, boxHeightPx, scrollState.value) {
                        derivedStateOf {
                            val cursorScreenY = cursorYInText - scrollState.value
                            val distanceToBottom = boxHeightPx - cursorScreenY
                            val overlap = imeHeightPx - distanceToBottom
                            if (overlap > 0) overlap else 0f
                        }
                    }

                    LaunchedEffect(amountToScroll) {
                        if (amountToScroll > 0) {
                            scrollState.scrollBy(amountToScroll)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(bottom = with(density) { imeHeightPx.toDp() })
                    ) {
                        var textFieldValue by remember { mutableStateOf(TextFieldValue(state.description)) }

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

                                val layoutResult = textLayoutResultState
                                if (layoutResult != null) {
                                    val cursorIndex = newValue.selection.start
                                    if (cursorIndex <= layoutResult.layoutInput.text.length) {
                                        val cursorRect = layoutResult.getCursorRect(cursorIndex)
                                        cursorYInText = cursorRect.bottom
                                    }
                                }
                            },
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
                                .padding(spacing.smallMedium),
                            textStyle = IssueSpotTypography.bodyLarge.copy(
                                color = IssueSpotColors.OnSurface
                            ),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (textFieldValue.text.isEmpty()) {
                                        Text(
                                            text = "Describe the issue you want to report...",
                                            style = IssueSpotTypography.bodyLarge,
                                            color = IssueSpotColors.OnSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        val selectedMedia = state.selectedMedia
                        if (selectedMedia != null) {
                            MediaPreviewContent(
                                mediaItems = selectedMedia,
                                onRemove = { onIntent(CreatePostIntent.RemoveMedia) },
                                onRemoveImage = { onIntent(CreatePostIntent.RemoveImage(it)) }
                            )
                        }
                    }
                }
            }


            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)).clickable(enabled = false) {}, 
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = IssueSpotColors.Primary)
                }
            }

            if(state.selectedMedia == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .imePadding()
                        .background(Color.Transparent)
                        .padding(bottom = spacing.large, start = spacing.extraSmall, top = spacing.extraSmall, end = spacing.large),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small, Alignment.End)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = IssueSpotColors.OnSurfaceVariant.copy(alpha = 0.1f),
                                shape = shapes.small
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

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = IssueSpotColors.OnSurfaceVariant.copy(alpha = 0.1f),
                                shape = shapes.small
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
    mediaItems: List<SelectedMediaItem>,
    onRemove: () -> Unit,
    onRemoveImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val overlayController = LocalOverlayController.current
    var aspectRatio by remember { mutableFloatStateOf(1f) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // 2. Close Button (Appears above the content)
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.End)
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
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(8.dp))
        ) {
            when (mediaItems.first().type) {
                MediaType.IMAGE -> {

                    ImageGrid(
                        images = mediaItems,
                        onRemove = onRemoveImage,
                        onImageClick = { clickedIndex ->

                            overlayController.show(
                                type = MediaType.IMAGE,
                                urls = mediaItems.map { it.uri },
                                initialIndex = clickedIndex
                            )
                        }
                    )

                }
                MediaType.VIDEO -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        VideoPreviewPlayer(
                            videoUri = mediaItems.first().uri,
                            modifier = Modifier.fillMaxSize(),
                            onFullscreenClick = {
                                // Trigger the global overlay when fullscreen icon is clicked
                                overlayController.show(
                                    type = MediaType.VIDEO,
                                    urls = mediaItems.map { it.uri },
                                )
                            },
                            onAspectRatioAvailable = { newRatio ->
                                aspectRatio = newRatio
                            }
                        )
                    }
                }

                MediaType.PDF -> {
                    PdfPreviewContent(
                        pdfUri = mediaItems.first().uri.toUri(),
                        onFullscreenClick = {
                            overlayController.show(MediaType.PDF, mediaItems.map { it.uri })
                        }
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
fun ImageGrid(
    images: List<SelectedMediaItem>,
    onRemove: (String) -> Unit,
    onImageClick: (Int) -> Unit
) {
    val count = images.size
    val gridHeight = 300.dp // Fixed height for multi-image grids to look uniform
    val spacing = 4.dp

    Box(modifier = Modifier.fillMaxWidth()) {
        when (count) {
            1 -> {
                // Single Image - Original logic
                var singleAspectRatio by remember { mutableFloatStateOf(1f) }
                GridImageItem(
                    uri = images[0].uri,
                    onRemove = { onRemove(images[0].uri) },
                    onClick = { onImageClick(0) },
                    modifier = Modifier.fillMaxWidth().aspectRatio(singleAspectRatio),
                    contentScale = ContentScale.Fit,
                    onSuccess = { state ->
                        val width = state.painter.intrinsicSize.width
                        val height = state.painter.intrinsicSize.height
                        if (height != 0f) {
                            singleAspectRatio = width / height
                            if (singleAspectRatio < 1f) singleAspectRatio = 1f
                        }
                    }
                )
            }
            2 -> {
                // Two Images - Side by side
                Row(modifier = Modifier.fillMaxWidth().height(gridHeight), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    GridImageItem(uri = images[0].uri, onRemove = { onRemove(images[0].uri) }, onClick = { onImageClick(0) }, modifier = Modifier.weight(1f).fillMaxHeight())
                    GridImageItem(uri = images[1].uri, onRemove = { onRemove(images[1].uri) }, onClick = { onImageClick(1) }, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
            3 -> {
                // Three Images - 1 Large Left, 2 Small Right vertically stacked
                Row(modifier = Modifier.fillMaxWidth().height(gridHeight), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    GridImageItem(uri = images[0].uri, onRemove = { onRemove(images[0].uri) }, onClick = { onImageClick(0) }, modifier = Modifier.weight(1f).fillMaxHeight())
                    Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                        GridImageItem(uri = images[1].uri, onRemove = { onRemove(images[1].uri) }, onClick = { onImageClick(1) }, modifier = Modifier.weight(1f).fillMaxWidth())
                        GridImageItem(uri = images[2].uri, onRemove = { onRemove(images[2].uri) }, onClick = { onImageClick(2) }, modifier = Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
            else -> {
                // Four or More Images - 2x2 Grid with +N overlay on the 4th
                Column(modifier = Modifier.fillMaxWidth().height(gridHeight), verticalArrangement = Arrangement.spacedBy(spacing)) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        GridImageItem(uri = images[0].uri, onRemove = { onRemove(images[0].uri) }, onClick = { onImageClick(0) }, modifier = Modifier.weight(1f).fillMaxHeight())
                        GridImageItem(uri = images[1].uri, onRemove = { onRemove(images[1].uri) }, onClick = { onImageClick(1) }, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        GridImageItem(uri = images[2].uri, onRemove = { onRemove(images[2].uri) }, onClick = { onImageClick(2) }, modifier = Modifier.weight(1f).fillMaxHeight())

                        // 4th Image with Overlay
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            GridImageItem(uri = images[3].uri, onRemove = { onRemove(images[3].uri) }, onClick = { onImageClick(3) }, modifier = Modifier.fillMaxSize())

                            if (count > 4) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable { onImageClick(3) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+${count - 4}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GridImageItem(
    uri: String,
    onRemove: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onSuccess: ((coil3.compose.AsyncImagePainter.State.Success) -> Unit)? = null
) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        AsyncImage(
            model = uri.toUri(),
            contentDescription = "Selected Image",
            onSuccess = onSuccess,
            modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
            contentScale = contentScale,
            error = painterResource(R.drawable.img_post_placeholder),
            fallback = painterResource(R.drawable.img_post_placeholder)
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .size(24.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Remove",
                modifier = Modifier.size(14.dp),
                tint = Color.White
            )
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




