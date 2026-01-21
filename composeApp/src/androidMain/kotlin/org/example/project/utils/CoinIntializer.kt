package org.example.project.utils

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.util.DebugLogger
import okio.Path.Companion.toPath

fun createImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25) // Use 25% of app's available memory
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(getCacheDirectory(context).resolve("image_cache"))
                .maxSizeBytes(50L * 1024 * 1024) // 50MB
                .build()
        }
        .crossfade(true)
        .networkCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .logger(DebugLogger()) // Remove in production
        .build()
}

