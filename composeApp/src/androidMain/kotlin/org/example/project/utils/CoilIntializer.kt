package org.example.project.utils

import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.util.DebugLogger
import okio.Path.Companion.toOkioPath

fun createImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader
        .Builder(context)
        .memoryCache {
            MemoryCache
                .Builder()
                .maxSizePercent(context, 0.25) // Use 25% of app's available memory
                .build()
        }.diskCache {
            DiskCache
                .Builder()
                .directory(getCacheDirectory(context).resolve("image_cache"))
                .maxSizeBytes(50L * 1024 * 1024) // 50MB
                .build()
        }.crossfade(true)
        .networkCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .logger(DebugLogger()) // Remove in production
        .build()

fun getCacheDirectory(context: PlatformContext): okio.Path = (context as Context).cacheDir.toOkioPath()
