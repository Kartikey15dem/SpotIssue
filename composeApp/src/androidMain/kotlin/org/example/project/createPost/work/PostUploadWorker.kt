package org.example.project.createPost.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.example.project.core.datastore.UserPreferencesRepository
import org.example.project.core.datastore.model.UploadDraftState
import org.example.project.core.datastore.model.UploadStatus
import org.example.project.core.data.repository.PostRepository
import org.example.project.core.model.createPost.CreatePost
import org.example.project.core.model.home.MediaType
import org.example.project.core.model.home.PostLevel
import org.example.project.utils.media.MediaCompressorUtil
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.core.utils.DataState
import java.io.File

class PostUploadWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val prefRepository: UserPreferencesRepository by inject()
    private val postRepository: PostRepository by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val compressedPaths = mutableListOf<String>()
        try {
            val draft = prefRepository.userData.value.uploadDraftState
            
            if (draft.status != UploadStatus.UPLOADING) {
                return@withContext Result.success()
            }

                        // 0. Check total size (Max 50MB)
            var totalSizeBytes: Long = 0
            for (uriString in draft.mediaUris) {
                appContext.contentResolver.openAssetFileDescriptor(android.net.Uri.parse(uriString), "r")?.use {
                    totalSizeBytes += it.length
                }
            }
            
            if (totalSizeBytes > 50 * 1024 * 1024) {
                prefRepository.updateUploadDraftState(
                    draft.copy(status = UploadStatus.ERROR, errorMessage = "Total media size exceeds 50MB limit.")
                )
                return@withContext Result.failure()
            }

            // 1. Compress and Prepare Files
            if (draft.mediaUris.isNotEmpty()) {
                for (uriString in draft.mediaUris) {
                    val compressedFile: File? = if (draft.mediaType == "IMAGE") {
                        MediaCompressorUtil.compressImage(appContext, uriString)
                    } else {
                        // Video/PDF copy for now
                        MediaCompressorUtil.compressImage(appContext, uriString)
                    }
                    compressedFile?.let { compressedPaths.add(it.absolutePath) }
                }
            }

            // 2. Submit to Backend (Multipart)
            val userLocation = prefRepository.userData.value.userLocation
            
            val createPostModel = CreatePost(
                userId = "", 
                userName = "",
                userUrl = "",
                postLevel = PostLevel.valueOf(draft.postLevel),
                postText = draft.postText,
                mediaType = MediaType.valueOf(draft.mediaType),
                mediaFilePaths = compressedPaths, 
                location = userLocation
            )

            when (val result = postRepository.createPost(createPostModel)) {
                is DataState.Success -> {
                    prefRepository.updateUploadDraftState(
                        draft.copy(status = UploadStatus.SUCCESS)
                    )
                    Result.success()
                }
                is DataState.Error -> {
                    prefRepository.updateUploadDraftState(
                        draft.copy(
                            status = UploadStatus.ERROR, 
                            errorMessage = result.exception.message ?: "Failed to create post on server."
                        )
                    )
                    Result.retry() 
                }
                else -> Result.failure()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            val currentDraft = prefRepository.userData.value.uploadDraftState
            prefRepository.updateUploadDraftState(
                currentDraft.copy(
                    status = UploadStatus.ERROR, 
                    errorMessage = e.message ?: "Unknown error during upload."
                )
            )
            Result.failure()
        } finally {
            compressedPaths.forEach { File(it).delete() }
        }
    }
}
