package org.example.project.utils.media

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import id.zelory.compressor.Compressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.provider.OpenableColumns

object MediaCompressorUtil {

    suspend fun compressImage(context: Context, uriString: String): File? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            val file = copyUriToFile(context, uri) ?: return@withContext null
            
            // Limit file size roughly, though compressor handles dimensions
            val compressedImageFile = Compressor.compress(context, file)
            compressedImageFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun copyUriToFile(context: Context, uri: Uri): File? {
        try {
            val contentResolver = context.contentResolver
            var fileName = "temp_media_${System.currentTimeMillis()}"
            
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }

            val tempFile = File(context.cacheDir, fileName)
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
