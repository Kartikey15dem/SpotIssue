package org.example.project.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// IssueSpot Typography - Based on the screenshot
val IssueSpotTypography =
    Typography(
        // App Title "IssueSpot"
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp, // Decreased from 22.sp
                lineHeight = 24.sp, // Adjusted from 26.sp
                letterSpacing = 0.sp,
            ),
        // App Subtitle "Community Issue Reporting"
        headlineSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp, // Decreased from 14.sp
                lineHeight = 16.sp, // Adjusted from 18.sp
                letterSpacing = 0.25.sp,
            ),
        // Section headers like "Local Issues"
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp, // Decreased from 18.sp
                lineHeight = 20.sp, // Adjusted from 22.sp
                letterSpacing = 0.sp,
            ),
        // User names like "Sarah Wilson", "John Doe"
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, // Decreased from 16.sp
                lineHeight = 18.sp, // Adjusted from 20.sp
                letterSpacing = 0.15.sp,
            ),
        // Post content text
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp, // Decreased from 16.sp
                lineHeight = 18.sp, // Adjusted from 20.sp
                letterSpacing = 0.5.sp,
            ),
        // General body text
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp, // Decreased from 14.sp
                lineHeight = 16.sp, // Adjusted from 18.sp
                letterSpacing = 0.25.sp,
            ),
        // Timestamps like "6d ago", "10d ago" and location text
        bodySmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp, // Decreased from 12.sp
                lineHeight = 12.sp, // Adjusted from 14.sp
                letterSpacing = 0.4.sp,
            ),
        // Badge text like "Locality"
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp, // Decreased from 12.sp
                lineHeight = 12.sp, // Adjusted from 14.sp
                letterSpacing = 0.1.sp,
            ),
        // Button text like "Post" button
        labelMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp, // Decreased from 14.sp
                lineHeight = 16.sp, // Adjusted from 18.sp
                letterSpacing = 0.1.sp,
            ),
        // Bottom navigation labels
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp, // Decreased from 11.sp
                lineHeight = 12.sp, // Adjusted from 14.sp
                letterSpacing = 0.5.sp,
            ),
    )

// Extension object for custom typography styles specific to IssueSpot
object IssueSpotTextStyles {
    // App title in top bar
    val AppTitle =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp, // Decreased from 20.sp
            lineHeight = 20.sp, // Adjusted from 22.sp
            letterSpacing = 0.sp,
        )

    // App subtitle in top bar
    val AppSubtitle =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp, // Decreased from 12.sp
            lineHeight = 12.sp, // Adjusted from 14.sp
            letterSpacing = 0.25.sp,
        )

    // Issue count text like "2 active issues"
    val IssueCount =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp, // Decreased from 12.sp
            lineHeight = 12.sp, // Adjusted from 14.sp
            letterSpacing = 0.4.sp,
        )

    // Location text under user names
    val LocationText =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp, // Decreased from 12.sp
            lineHeight = 12.sp, // Adjusted from 14.sp
            letterSpacing = 0.4.sp,
        )

    // Post interaction numbers (like count, comment count)
    val InteractionCount =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp, // Decreased from 14.sp
            lineHeight = 16.sp, // Adjusted from 18.sp
            letterSpacing = 0.25.sp,
        )

    // Section description like "Issues in your immediate area"
    val SectionDescription =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp, // Decreased from 13.sp
            lineHeight = 16.sp, // Adjusted from 18.sp
            letterSpacing = 0.25.sp,
        )
}
