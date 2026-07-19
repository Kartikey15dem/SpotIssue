package org.example.project.home.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTheme
import org.example.project.theme.IssueSpotTypography

@Composable
fun HomeFeedFooter(
    state: FooterState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = IssueSpotTheme.spacing
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 72.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is FooterState.Loading -> {
                CircularProgressIndicator(color = IssueSpotColors.Primary)
            }
            is FooterState.Error -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(spacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.throwable.message ?: "An error occurred",
                        color = IssueSpotColors.OnBackground,
                        style = IssueSpotTypography.bodyMedium,
                    )
                    Spacer(Modifier.height(spacing.small))
                    Button(
                        onClick = onRetry,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = IssueSpotColors.Primary,
                                contentColor = IssueSpotColors.OnPrimary,
                            ),
                    ) {
                        Text("Retry")
                    }
                }
            }
            is FooterState.EndReached -> {
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(spacing.medium),
                    text = "No more posts",
                    color = IssueSpotColors.OnBackground,
                    style = IssueSpotTypography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            is FooterState.Hidden -> {
                // Empty state, box remains 72dp
            }
        }
    }
}
