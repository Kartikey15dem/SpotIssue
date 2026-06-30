package org.example.project.core.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager

actual object FileSystem {
    @OptIn(ExperimentalForeignApi::class)
    actual fun deleteFile(path: String): Boolean {
        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(path)) {
            return fileManager.removeItemAtPath(path, null)
        }
        return false
    }
}
