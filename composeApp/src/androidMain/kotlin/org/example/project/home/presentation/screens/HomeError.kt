package org.example.project.home.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTheme
import org.example.project.theme.IssueSpotTypography

@Composable
fun HomeInitialError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = IssueSpotTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = IssueSpotColors.OnBackground,
            style = IssueSpotTypography.bodyMedium
        )
        Spacer(Modifier.height(spacing.small))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = IssueSpotColors.Primary,
                contentColor = IssueSpotColors.OnPrimary
            )
        ) {
            Text("Retry")
        }
    }
}

@Composable
fun HomeEmptyFeed(modifier: Modifier = Modifier) {
    val spacing = IssueSpotTheme.spacing
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(spacing.medium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No posts found",
            style = IssueSpotTypography.bodyLarge,
            color = IssueSpotColors.OnBackground
        )
    }
}
