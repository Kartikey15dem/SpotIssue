package org.example.project.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.graphics.createBitmap

// Helper to render PDF pages to Bitmaps
suspend fun pdfToBitmaps(context: Context, uri: Uri): List<Bitmap> {
    return withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            // 1. Open the file descriptor
            val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")

            fileDescriptor?.use { fd ->
                val renderer = PdfRenderer(fd)

                // 2. Loop through all pages
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)

                    // 3. Create a bitmap (A4 size scaled to screen density helps)
                    // We use a fixed width for consistency
                    val width = 1080
                    val height = (width.toFloat() / page.width * page.height).toInt()

                    val bitmap = createBitmap(width, height)

                    // 4. Render the page onto the bitmap
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)

                    page.close()
                }
                renderer.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bitmaps
    }
}