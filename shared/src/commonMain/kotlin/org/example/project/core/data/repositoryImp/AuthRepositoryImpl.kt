package org.example.project.core.data.repositoryImp

import org.example.project.core.data.repository.AuthRepository
import org.example.project.core.datastore.UserPreferencesRepository
import org.example.project.core.network.dto.LoginRequestDto
import org.example.project.core.network.dto.VerifyRequestDto
import org.example.project.core.network.dto.VerifyResponseDto
import org.example.project.core.network.services.AuthenticationService
import org.example.project.core.utils.DataState
import org.example.project.core.utils.safeApiCall

class AuthRepositoryImpl(
    private val authenticationService: AuthenticationService,
    private val userPreferencesRepository: UserPreferencesRepository,
) : AuthRepository {

    override suspend fun requestOtp(email: String): DataState<String> = safeApiCall {
        val request = LoginRequestDto(email)
        authenticationService.requestOtp(request)
    }

    override suspend fun verifyOtp(
        email: String,
        otp: String
    ): DataState<VerifyResponseDto> = safeApiCall {
        val request = VerifyRequestDto(email, otp)
        val response = authenticationService.verifyOtp(request)

        userPreferencesRepository.updateToken(response.token)
        response
    }

}