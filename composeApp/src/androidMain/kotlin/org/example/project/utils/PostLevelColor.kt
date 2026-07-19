package org.example.project.utils

import androidx.compose.ui.graphics.Color
import org.example.project.core.model.home.PostLevel

fun PostLevel.getColor(): Color =
    when (this) {
        PostLevel.LOCALITY -> Color(0xFF2E7D32) // Dark Green (matches your image)
        PostLevel.DISTRICT -> Color(0xFF1976D2) // Medium Blue (matches your image)
        PostLevel.STATE -> Color(0xFF7B1FA2) // Purple (matches your image)
        PostLevel.NATIONAL -> Color(0xFFD32F2F) // Red (matches your image)
    }
