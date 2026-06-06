package org.example.project.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class EmailChangeRequest(val newEmail: String)

@Serializable
data class EmailChangeVerifyRequest(val newEmail: String, val code: String)
