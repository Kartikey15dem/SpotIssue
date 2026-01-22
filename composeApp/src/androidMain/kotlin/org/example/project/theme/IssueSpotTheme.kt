package org.example.project.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val IssueSpotColorScheme = lightColorScheme(
    primary = IssueSpotColors.Primary,
    onPrimary = IssueSpotColors.OnPrimary,
    primaryContainer = IssueSpotColors.PrimaryContainer,
    onPrimaryContainer = IssueSpotColors.OnPrimaryContainer,

    secondary = IssueSpotColors.Secondary,
    onSecondary = IssueSpotColors.OnSecondary,
    secondaryContainer = IssueSpotColors.SecondaryContainer,
    onSecondaryContainer = IssueSpotColors.OnSecondaryContainer,

    tertiary = IssueSpotColors.Tertiary,
    onTertiary = IssueSpotColors.OnTertiary,
    tertiaryContainer = IssueSpotColors.TertiaryContainer,
    onTertiaryContainer = IssueSpotColors.OnTertiaryContainer,

    background = IssueSpotColors.Background,
    onBackground = IssueSpotColors.OnBackground,

    surface = IssueSpotColors.Surface,
    onSurface = IssueSpotColors.OnSurface,
    onSurfaceVariant = IssueSpotColors.OnSurfaceVariant,
    surfaceVariant = IssueSpotColors.SurfaceVariant,
)

@Composable
fun IssueSpotTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IssueSpotColorScheme,
        typography = IssueSpotTypography,
        content = content
    )
}