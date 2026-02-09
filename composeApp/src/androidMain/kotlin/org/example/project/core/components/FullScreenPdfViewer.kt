package org.example.project.core.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.example.project.theme.IssueSpotTypography
import org.example.project.utils.pdfToBitmaps
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import org.example.project.R

@OptIn(ExperimentalFoundationApi::class) // Required for Pager
@Composable
fun FullScreenPdfViewer(uri: Uri) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State
    var pdfPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    val pagerState = rememberPagerState(pageCount = { pdfPages.size })

    LaunchedEffect(uri) {
        pdfPages = pdfToBitmaps(context, uri)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (pdfPages.isEmpty()) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        } else {
            // 1. The Carousel (Pager)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                // Page Content
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = pdfPages[pageIndex].asImageBitmap(),
                        contentDescription = "Page $pageIndex",
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(), // Fit width, adjust height
                        contentScale = ContentScale.FillWidth
                    )
                }
            }

            // 2. Navigation Arrows (Overlay)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // LEFT ARROW (Previous)
                // Only show if not on first page
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back_arrow),
                            contentDescription = "Previous Page",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp)) // Placeholder to keep spacing
                }

                // RIGHT ARROW (Next)
                // Only show if not on last page
                if (pagerState.currentPage < pdfPages.size - 1) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_next_arrow),
                            contentDescription = "Next Page",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            // 3. Page Indicator (Bottom Center)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Page ${pagerState.currentPage + 1} of ${pdfPages.size}",
                    color = Color.White,
                    style = IssueSpotTypography.bodyMedium
                )
            }
        }
    }
}