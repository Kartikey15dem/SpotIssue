package org.example.project.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import androidx.core.graphics.createBitmap

// Helper to render PDF pages to Bitmaps
suspend fun pdfToBitmaps(context: Context, uri: Uri): List<Bitmap> {
    return withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        var tempFile: File? = null
        try {
            android.util.Log.d("PdfToBitmap", "Loading PDF from URI: $uri, scheme: ${uri.scheme}")
            val fileDescriptor: ParcelFileDescriptor? = if (uri.scheme == "http" || uri.scheme == "https") {
                tempFile = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
                android.util.Log.d("PdfToBitmap", "Downloading remote PDF to ${tempFile.absolutePath}")
                
                val connection = URL(uri.toString()).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()
                
                if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                    android.util.Log.e("PdfToBitmap", "HTTP Error: ${connection.responseCode} ${connection.responseMessage}")
                    throw Exception("HTTP Error ${connection.responseCode}")
                }
                
                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                android.util.Log.d("PdfToBitmap", "Download complete. File size: ${tempFile.length()}")
                ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                android.util.Log.d("PdfToBitmap", "Opening local PDF")
                context.contentResolver.openFileDescriptor(uri, "r")
            }

            fileDescriptor?.use { fd ->
                val renderer = PdfRenderer(fd)
                android.util.Log.d("PdfToBitmap", "PDF opened successfully. Page count: ${renderer.pageCount}")

                // 2. Loop through all pages
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)

                    // 3. Create a bitmap (A4 size scaled to screen density helps)
                    // We use a fixed width for consistency
                    val width = 1080
                    val height = (width.toFloat() / page.width * page.height).toInt()

                    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    
                    // Fill with white background (PDFs often have transparent backgrounds)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    // 4. Render the page onto the bitmap
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)

                    page.close()
                }
                renderer.close()
            }
        } catch (e: Exception) {
            android.util.Log.e("PdfToBitmap", "Error rendering PDF: ${e.message}", e)
            e.printStackTrace()
        } finally {
            try {
                tempFile?.delete()
                android.util.Log.d("PdfToBitmap", "Deleted temp file")
            } catch (e: Exception) {
                // Ignore
            }
        }
        bitmaps
    }
}