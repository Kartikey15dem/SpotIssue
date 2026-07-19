package org.example.project.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

// Helper to render PDF pages to Bitmaps
suspend fun pdfToBitmaps(
    context: Context,
    uri: Uri,
): List<Bitmap> =
    withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        var tempFile: File? = null
        try {
            /* WHY THIS CONDITIONAL LOGIC EXISTS:
             * Android's `PdfRenderer` natively requires a seekable `ParcelFileDescriptor`.
             * It absolutely cannot read a remote stream (http/https) directly.
             * Therefore, if the URI is a remote network URL, we MUST download it to a
             * local temporary cache file first, get a file descriptor for that local file,
             * and then pass it to the PdfRenderer.
             */
            val fileDescriptor: ParcelFileDescriptor? =
                if (uri.scheme == "http" || uri.scheme == "https") {
                    tempFile = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")

                    val connection = URL(uri.toString()).openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.connect()

                    if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                        throw Exception("HTTP Error ${connection.responseCode}")
                    }

                    connection.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                } else {
                    context.contentResolver.openFileDescriptor(uri, "r")
                }

            fileDescriptor?.use { fd ->
                val renderer = PdfRenderer(fd)

                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)

                    val width = 1080
                    val height = (width.toFloat() / page.width * page.height).toInt()

                    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)

                    /* WHY DRAW A WHITE BACKGROUND:
                     * By default, `createBitmap` has a transparent alpha channel.
                     * Many PDFs do not explicitly define a background color (they assume white paper).
                     * If we render directly, the transparent background often shows up as solid black
                     * in Android ImageViews, hiding dark text. Drawing white first fixes this.
                     */
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)

                    page.close()
                }
                renderer.close()
            }
        } catch (e: Exception) {
        } finally {
            /*
             * Crucial cleanup step. We must delete the temporary file after rendering
             * the Bitmaps into memory so the device's cache folder doesn't bloat.
             */
            try {
                tempFile?.delete()
            } catch (e: Exception) {
                // Ignore
            }
        }
        bitmaps
    }
