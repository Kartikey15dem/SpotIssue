package org.example.project.home.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import org.example.project.theme.IssueSpotTheme
import org.example.project.theme.IssueSpotTypography

@Composable
fun HomeOfflineBanner(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Color(0xFFE53935))
                .padding(vertical = IssueSpotTheme.spacing.extraSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "You are currently offline. Showing cached posts.",
            style = IssueSpotTypography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}
