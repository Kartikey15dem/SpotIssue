package org.example.project.home.domain.models

typealias Post = org.example.project.core.model.home.Post
typealias MediaType = org.example.project.core.model.home.MediaType
typealias PostLevel = org.example.project.core.model.home.PostLevel

fun PostLevel.getText(): String {
    return when (this) {
        PostLevel.LOCALITY -> "Issues in your immediate area"
        PostLevel.DISTRICT -> "District-wide concerns and problems"
        PostLevel.STATE -> "State-level issues affecting your region"
        PostLevel.NATIONAL -> "Nationwide issues and concerns"
    }
}

