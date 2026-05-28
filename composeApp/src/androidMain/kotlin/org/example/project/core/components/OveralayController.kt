package org.example.project.core.components

import android.os.Parcelable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import kotlinx.parcelize.Parcelize
import org.example.project.core.model.home.MediaType

class OverlayController {

    var currentOverlay by mutableStateOf<OverlayData?>(null)
        private set

    fun show(type: MediaType, urls: List<String>,initialIndex : Int = 0) {
        currentOverlay = OverlayData(type, urls,initialIndex)
    }

    fun hide() {
        currentOverlay = null
    }

    companion object {
        val Saver: Saver<OverlayController, *> = mapSaver(
            save = {
                // Save the 'currentOverlay' state to a map
                mapOf("overlay_data" to it.currentOverlay)
            },
            restore = {
                // Restore the class and set the state back
                OverlayController().apply {
                    currentOverlay = it["overlay_data"] as? OverlayData
                }
            }
        )
    }
}

val LocalOverlayController = staticCompositionLocalOf<OverlayController> {
    error("No OverlayController provided")
}
@Parcelize
data class OverlayData(
    val type: MediaType,
    val url: List<String>,
    val initialIndex : Int
) : Parcelable