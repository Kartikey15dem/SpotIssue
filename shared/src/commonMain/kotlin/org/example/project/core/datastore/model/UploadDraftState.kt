package org.example.project.core.datastore.model

import kotlinx.serialization.Serializable

@Serializable
enum class UploadStatus {
    IDLE, UPLOADING, ERROR, SUCCESS
}

@Serializable
data class UploadDraftState(
    val status: UploadStatus = UploadStatus.IDLE,
    val postText: String = "",
    val postLevel: String = "LOCALITY",
    val mediaUris: List<String> = emptyList(),
    val mediaType: String = "IMAGE",
    val errorMessage: String? = null
) {
    companion object {
        val DEFAULT = UploadDraftState()
    }
}
