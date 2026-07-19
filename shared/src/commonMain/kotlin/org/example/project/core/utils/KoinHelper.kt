package org.example.project.core.utils

import org.example.project.core.data.repository.PostRepository
import org.example.project.core.datastore.UserPreferencesRepository
import org.example.project.feature.auth.viewmodel.AuthViewModel
import org.example.project.feature.auth.viewmodel.LocationFetchViewModel
import org.example.project.feature.auth.viewmodel.NameCaptureViewModel
import org.example.project.feature.createPost.viewmodel.CreatePostViewModel
import org.example.project.feature.home.viewmodel.HomeViewModel
import org.example.project.feature.profile.viewmodel.EditProfileViewModel
import org.example.project.feature.profile.viewmodel.ProfileViewModel
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform.getKoin

object KoinHelper {
    fun getAuthViewModel(): AuthViewModel = getKoin().get()

    fun getLocationFetchViewModel(): LocationFetchViewModel = getKoin().get()

    fun getNameCaptureViewModel(email: String): NameCaptureViewModel = getKoin().get { parametersOf(email) }

    fun getCreatePostViewModel(): CreatePostViewModel = getKoin().get()

    fun getHomeViewModel(): HomeViewModel = getKoin().get()

    fun getEditProfileViewModel(): EditProfileViewModel = getKoin().get()

    fun getProfileViewModel(): ProfileViewModel = getKoin().get()

    fun getUserPreferencesRepository(): UserPreferencesRepository = getKoin().get()

    fun getPostRepository(): PostRepository = getKoin().get()
}
