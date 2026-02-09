package org.example.project.core.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun PdfPreviewContent(
    pdfUri: Uri,
    onFullscreenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pdfPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load PDF in background
    LaunchedEffect(pdfUri) {
        isLoading = true
        pdfPages = pdfToBitmaps(context, pdfUri)
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square Aspect Ratio as requested
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Gray.copy(alpha = 0.1f))
            .clickable { onFullscreenClick() } // Click anywhere to open overlay
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            // Horizontal Scrollable List of Pages
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(pdfPages) { pageBitmap ->
                    Image(
                        bitmap = pageBitmap.asImageBitmap(),
                        contentDescription = "PDF Page",
                        contentScale = ContentScale.FillHeight, // Fill height, scroll width
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .shadow(2.dp)
                    )
                }
            }

            // Optional: "PDF" Badge in corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Red, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("PDF", color = Color.White, style = IssueSpotTypography.labelSmall)
            }
        }
    }
}