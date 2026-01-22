//import org.example.project.core.di.ImagePicker
//
//o package org.example.project.utils
//
//import android.Manifest
//import android.content.Context
//import android.content.pm.PackageManager
//import android.net.Uri
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.runtime.*
//import androidx.core.content.ContextCompat
//import androidx.core.content.FileProvider
//import kotlinx.coroutines.suspendCancellableCoroutine
//import java.io.File
//import kotlin.coroutines.resume
//
///**
// * Android implementation of ImagePicker
// */
//class AndroidImagePicker(private val context: Context) : ImagePicker {
//
//    override suspend fun pickImageFromGallery(): String? {
//        return suspendCancellableCoroutine { continuation ->
//            // This would need to be implemented with proper Activity result handling
//            // For now, return null as placeholder
//            continuation.resume(null)
//        }
//    }
//
//    override suspend fun captureImageFromCamera(): String? {
//        return suspendCancellableCoroutine { continuation ->
//            // This would need to be implemented with proper Activity result handling
//            // For now, return null as placeholder
//            continuation.resume(null)
//        }
//    }
//
//    override fun hasCameraPermission(): Boolean {
//        return ContextCompat.checkSelfPermission(
//            context,
//            Manifest.permission.CAMERA
//        ) == PackageManager.PERMISSION_GRANTED
//    }
//
//    override suspend fun requestCameraPermission(): Boolean {
//        return suspendCancellableCoroutine { continuation ->
//            // This would need to be implemented with proper permission request handling
//            // For now, return false as placeholder
//            continuation.resume(false)
//        }
//    }
//
//    override fun hasGalleryPermission(): Boolean {
//        // On Android 13+, need READ_MEDIA_IMAGES permission
//        // On older versions, need READ_EXTERNAL_STORAGE
//        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
//            ContextCompat.checkSelfPermission(
//                context,
//                Manifest.permission.READ_MEDIA_IMAGES
//            ) == PackageManager.PERMISSION_GRANTED
//        } else {
//            ContextCompat.checkSelfPermission(
//                context,
//                Manifest.permission.READ_EXTERNAL_STORAGE
//            ) == PackageManager.PERMISSION_GRANTED
//        }
//    }
//ot
//    override suspend fun requestGalleryPermission(): Boolean {
//        return suspendCancellableCoroutine { continuation ->
//            // This would need to be implemented with proper permission request handling
//            // For now, return false as placeholder
//            continuation.resume(false)
//        }
//    }
//
//    private fun createImageUri(): Uri {
//        val image = File(context.filesDir, "camera_photo.jpg")
//        return FileProvider.getUriForFile(
//            context,
//            "${context.packageName}.fileprovider",
//            image
//        )
//    }
//}
