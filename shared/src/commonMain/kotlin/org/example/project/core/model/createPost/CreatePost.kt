package org.example.project.core.model.createPost

import org.example.project.core.model.auth.UserLocation
import org.example.project.core.model.home.MediaType

data class CreatePost(
    val postText: String,
    val mediaType: MediaType?,
    val postLevel: String = "LOCALITY",
    val mediaFilePaths: List<String> = emptyList(),
    val location: UserLocation = UserLocation(),
)