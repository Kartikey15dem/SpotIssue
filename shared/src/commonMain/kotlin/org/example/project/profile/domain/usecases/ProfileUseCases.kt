package org.example.project.profile.domain.usecases

import org.example.project.home.domain.models.Post
import org.example.project.profile.domain.models.Profile
import org.example.project.profile.domain.repository.ProfileRepository

/**
 * Get user profile information
 */
class GetProfileUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(userId: String? = null): Result<Profile> {
        return profileRepository.getProfile(userId)
    }
}

/**
 * Update user profile
 */
class UpdateProfileUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(profile: Profile): Result<Profile> {
        return profileRepository.updateProfile(profile)
    }
}

/**
 * Get posts created by user
 */
class GetUserPostsUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(userId: String? = null): Result<List<Post>> {
        return profileRepository.getUserPosts(userId)
    }
}

/**
 * Get posts liked by user
 */
class GetLikedPostsUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(userId: String? = null): Result<List<Post>> {
        return profileRepository.getLikedPosts(userId)
    }
}

/**
 * Refresh profile data
 */
class RefreshProfileUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(userId: String? = null): Result<Profile> {
        return profileRepository.refreshProfile(userId)
    }
}

