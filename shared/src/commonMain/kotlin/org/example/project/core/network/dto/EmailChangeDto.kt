package org.example.project.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmailChangeRequest(
    @SerialName("new_email")
    val newEmail: String
)

@Serializable
data class EmailChangeVerifyRequest(
    @SerialName("new_email")
    val newEmail: String,
    val code: String
)
