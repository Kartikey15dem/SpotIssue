package org.example.project.core.utils

import co.touchlab.kermit.Logger
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.example.project.core.network.dto.ErrorResponseDto

const val NETWORK_ERROR_MESSAGE = "Check network connection"
private const val FALLBACK_ERROR_MESSAGE = "An error occurred.\n\nPlease try again."

private val safeApiCallLogger = Logger.withTag("SafeApiCall")

private val errorJson =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

suspend fun <T> safeApiCall(
    networkMonitor: NetworkMonitor? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    apiCall: suspend () -> T,
): DataState<T> {
    return withContext(dispatcher) {
        if (networkMonitor?.isOnline?.first() == false) {
            safeApiCallLogger.w { "API call skipped because network is unavailable" }
            return@withContext DataState.Error(Exception(NETWORK_ERROR_MESSAGE))
        }

        try {
            DataState.Success(apiCall())
        } catch (e: ClientRequestException) {
            handleResponseException(e)
        } catch (e: ServerResponseException) {
            handleResponseException(e)
        } catch (e: IOException) {
            safeApiCallLogger.e { "Network error: ${e.message ?: e::class.simpleName}" }
            DataState.Error(Exception(FALLBACK_ERROR_MESSAGE))
        } catch (e: Exception) {
            safeApiCallLogger.e { "Unexpected API error: ${e.message ?: e::class.simpleName}" }
            DataState.Error(Exception(FALLBACK_ERROR_MESSAGE, e))
        }
    }
}

private suspend fun <T> handleResponseException(e: ResponseException): DataState<T> {
    val rawBody =
        try {
            e.response.bodyAsText()
        } catch (inner: Exception) {
            safeApiCallLogger.e { "Failed to read error response: ${inner.message ?: inner::class.simpleName}" }
            null
        }

    val errorBody = rawBody?.parseErrorResponse()
    val developerMessage =
        errorBody?.developerMessage
            ?: rawBody?.takeIf { it.isNotBlank() }
            ?: e.message
            ?: e::class.simpleName
            ?: FALLBACK_ERROR_MESSAGE
    val userMessage = errorBody?.userMessage?.takeIf { it.isNotBlank() } ?: FALLBACK_ERROR_MESSAGE

    safeApiCallLogger.e { "API error ${e.response.status.value}: $developerMessage" }
    return DataState.Error(Exception(userMessage))
}

private fun String.parseErrorResponse(): ErrorResponseDto? =
    try {
        errorJson.decodeFromString<ErrorResponseDto>(this)
    } catch (e: SerializationException) {
        safeApiCallLogger.e { "Failed to parse error response: ${e.message ?: e::class.simpleName}" }
        null
    } catch (e: IllegalArgumentException) {
        safeApiCallLogger.e { "Invalid error response body: ${e.message ?: e::class.simpleName}" }
        null
    }
