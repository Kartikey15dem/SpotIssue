package org.example.project.core.utils

import java.io.File

actual object FileSystem {
    actual fun deleteFile(path: String): Boolean =
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
}
