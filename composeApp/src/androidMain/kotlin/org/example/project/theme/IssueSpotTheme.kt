package org.example.project.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val IssueSpotColorScheme =
    lightColorScheme(
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
    spacing: IssueSpotSpacing = IssueSpotSpacing(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    CompositionLocalProvider(
        LocalIssueSpotSpacing provides spacing,
    ) {
        MaterialTheme(
            colorScheme = IssueSpotColorScheme,
            typography = IssueSpotTypography,
            shapes = IssueSpotShapes,
            content = content,
        )
    }
}

/**
 * Accessor object for the IssueSpot theme properties.
 */
object IssueSpotTheme {
    val spacing: IssueSpotSpacing
        @Composable
        get() = LocalIssueSpotSpacing.current
}
