package org.example.project.core.datastore.model

import kotlinx.serialization.Serializable
import org.example.project.core.model.home.SelectedMediaItem

@Serializable
enum class UploadStatus {
    IDLE,
    UPLOADING,
    ERROR,
    SUCCESS,
}

@Serializable
data class UploadDraftState(
    val status: UploadStatus = UploadStatus.IDLE,
    val postText: String = "",
    val selectedMedia: List<SelectedMediaItem>? = null,
    val errorMessage: String? = null,
) {
    companion object {
        val DEFAULT = UploadDraftState()
    }
}
