package org.example.project.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * Android implementation of VideoPicker
 * This class handles video selection from gallery and camera
 *
 * Note: The actual video picking is handled by Compose launchers in the UI layer
 * This class provides utility methods for permissions and URI creation
 */
class AndroidVideoPicker(private val context: Context) {

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasGalleryPermission(): Boolean {
        // On Android 13+, need READ_MEDIA_VIDEO permission
        // On older versions, need READ_EXTERNAL_STORAGE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun createVideoUri(): Uri {
        val video = File(context.filesDir, "camera_video_${System.currentTimeMillis()}.mp4")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            video
        )
    }

    fun getVideoPermission(): String {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}
