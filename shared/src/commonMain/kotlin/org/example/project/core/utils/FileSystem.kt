package org.example.project.core.utils

expect object FileSystem {
    fun deleteFile(path: String): Boolean
}
