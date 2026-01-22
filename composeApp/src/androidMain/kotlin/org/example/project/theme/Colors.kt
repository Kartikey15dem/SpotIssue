package org.example.project.theme

import androidx.compose.ui.graphics.Color

object IssueSpotColors {
    // Primary Colors
    val Primary = Color(0xFF00C853) // Green color from the "Locality" badge
    val OnPrimary = Color(0xFFFFFFFF) // White text on primary
    val PrimaryContainer = Color(0xFFE8F5E8) // Light green container
    val OnPrimaryContainer = Color(0xFF004F0E) // Dark green on light container // Changed to a lighter shade

    // Surface Colors
    val Surface = Color(0xFFFFFFFF) // White background of cards and main areas
    val OnSurface = Color(0xFF1C1B1F) // Dark text (user names, main content)
    val OnSurfaceVariant = Color(0xFF6B6B6B) // Gray text (timestamps, secondary info)
    val SurfaceLowest = Color(0xFFF8F8F8) // Very light gray for the main background
    val SurfaceVariant = Color(0xFFF5F5F5) // Light gray for subtle backgrounds

    // Background Colors
    val Background = Color(0xFFF8F8F8) // Main app background (light gray)
    val OnBackground = Color(0xFF1C1B1F) // Text on background

    // Secondary Colors
    val Secondary = Color(0xFF625B71) // Used for secondary elements
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFE8DEF8)
    val OnSecondaryContainer = Color(0xFF1E192B)

    // Tertiary Colors (for additional accents)
    val Tertiary = Color(0xFF7D5260)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFFFD8E4)
    val OnTertiaryContainer = Color(0xFF31111D)

    // Specific UI Element Colors
    val CardBackground = Color(0xFFFFFFFF) // White card backgrounds
    val DividerColor = Color(0xFFE0E0E0) // Light gray for dividers
    val IconTint = Color(0xFF6B6B6B) // Gray for inactive icons
    val ActiveIconTint = Color(0xFF00C853) // Green for active/selected icons

    // Action Colors
    val LikeColor = Color(0xFF6B6B6B) // Gray for unselected like button
    val LikeActiveColor = Color(0xFFE91E63) // Pink/Red for liked posts
    val CommentColor = Color(0xFF6B6B6B) // Gray for comment icon
    val ShareColor = Color(0xFF6B6B6B) // Gray for share icon

    // Level Badge Colors
    val LocalityBadge = Color(0xFF00C853) // Green for locality badge
    val DistrictBadge = Color(0xFF2196F3) // Blue for district
    val StateBadge = Color(0xFFFF9800) // Orange for state
    val NationalBadge = Color(0xFFE91E63) // Pink for national

    // Post Button Colors
    val PostButtonBackground = Color(0xFF1C1B1F) // Dark background for "+ Post" button
    val PostButtonText = Color(0xFFFFFFFF) // White text on post button

    // Issue Count Colors
    val IssueCountBackground = Color(0xFFF0F0F0) // Light gray for "2 active issues" background
    val IssueCountText = Color(0xFF6B6B6B) // Gray text for issue count

    // Outline/Border Colors
    val Outline = Color(0xFFE0E0E0) // Light gray for borders and outlines

    // Dark red (error)
    val Error = Color(0xFFBB0000)
    val OnError = Color(0xFFFFFFFF)

    // Lighter error container (was too dark)
    val ErrorContainer = Color(0xFF5A1414)     // lighter than 0xFF3B0A0A
    val OnErrorContainer = Color(0xFFFFEDEC)
}