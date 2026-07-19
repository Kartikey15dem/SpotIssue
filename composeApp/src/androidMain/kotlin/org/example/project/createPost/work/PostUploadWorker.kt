package org.example.project.createPost.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.core.data.repository.PostRepository
import org.example.project.core.datastore.UserPreferencesRepository
import org.example.project.core.datastore.model.UploadStatus
import org.example.project.core.model.createPost.CreatePost
import org.example.project.core.model.home.MediaType
import org.example.project.core.utils.DataState
import org.example.project.utils.media.MediaCompressorUtil
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class PostUploadWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams),
    KoinComponent {
    /* ===================================================================================
     * SECTION: RELIABLE BACKGROUND MEDIA UPLOADS
     * ===================================================================================
     * Uses Android's WorkManager to ensure that large media uploads (images, videos, PDFs)
     * complete successfully even if the user minimizes or closes the app.
     *
     * Pipeline:
     * 1. Reads the draft state from UserPreferences (DataStore).
     * 2. Validates file sizes (blocks > 50MB).
     * 3. Compresses Images / Prepares Videos and PDFs locally using MediaCompressorUtil.
     * 4. Triggers the KMP PostRepository multipart upload.
     * 5. Cleans up temporary compressed files locally, regardless of success or failure.
     */

    private val prefRepository: UserPreferencesRepository by inject()
    private val postRepository: PostRepository by inject()

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val compressedPaths = mutableListOf<String>()
            try {
                val draft = prefRepository.userData.value.uploadDraftState

                if (draft.status != UploadStatus.UPLOADING) {
                    return@withContext Result.success()
                }

                // 1. Compress and Prepare Files
                if (!draft.selectedMedia.isNullOrEmpty()) {
                    for (mediaItem in draft.selectedMedia) {
                        val compressedFile: File? =
                            if (mediaItem.type.name == "IMAGE") {
                                MediaCompressorUtil.compressImage(appContext, mediaItem.uri)
                            } else {
                                // Video/PDF copy
                                MediaCompressorUtil.prepareFile(appContext, mediaItem.uri)
                            }
                        compressedFile?.let { compressedPaths.add(it.absolutePath) }
                    }
                }

                var totalCompressedSizeBytes: Long = 0
                for (path in compressedPaths) {
                    val file = File(path)
                    if (file.exists()) {
                        totalCompressedSizeBytes += file.length()
                    }
                }

                if (totalCompressedSizeBytes > 50 * 1024 * 1024) {
                    prefRepository.updateUploadDraftState(
                        draft.copy(
                            status = UploadStatus.ERROR,
                            errorMessage = "The media size is too large.\n\nPlease choose a smaller file.",
                        ),
                    )
                    return@withContext Result.failure()
                }

                // 2. Submit to Backend (Multipart)
                val userLocation = prefRepository.userData.value.userLocation

                val createPostModel =
                    CreatePost(
                        postText = draft.postText,
                        mediaType = draft.selectedMedia?.firstOrNull()?.type ?: MediaType.IMAGE,
                        mediaFilePaths = compressedPaths,
                        location = userLocation,
                    )

                when (val result = postRepository.createPost(createPostModel)) {
                    is DataState.Success -> {
                        prefRepository.updateUploadDraftState(
                            draft.copy(status = UploadStatus.SUCCESS),
                        )
                        Result.success()
                    }
                    is DataState.Error -> {
                        prefRepository.updateUploadDraftState(
                            draft.copy(
                                status = UploadStatus.ERROR,
                                errorMessage = result.exception.message ?: "Failed to create post on server.",
                            ),
                        )
                        Result.retry()
                    }
                    else -> Result.failure()
                }
            } catch (e: Exception) {
                val currentDraft = prefRepository.userData.value.uploadDraftState
                prefRepository.updateUploadDraftState(
                    currentDraft.copy(
                        status = UploadStatus.ERROR,
                        errorMessage = e.message ?: "Unknown error during upload.",
                    ),
                )
                Result.failure()
            } finally {
                compressedPaths.forEach { File(it).delete() }
            }
        }
}
