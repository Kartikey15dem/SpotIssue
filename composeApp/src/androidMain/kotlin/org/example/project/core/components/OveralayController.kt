package org.example.project.core.components

import androidx.compose.runtime.*
import org.example.project.home.domain.models.MediaType

// 1. The Controller Class
class OverlayController {
    // If this is null, the overlay is hidden.
    // If it has data, the overlay is visible.
    var currentOverlay by mutableStateOf<OverlayData?>(null)
        private set

    fun show(type: MediaType, url: String) {
        currentOverlay = OverlayData(type, url)
    }

    fun hide() {
        currentOverlay = null
    }
}

val LocalOverlayController = staticCompositionLocalOf<OverlayController> {
    error("No OverlayController provided")
}


data class OverlayData(
    val type: MediaType,
    val url: String
)