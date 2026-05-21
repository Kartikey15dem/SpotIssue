package org.example.project.core.utils

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    apiCall: suspend () -> T
): DataState<T> {
    return withContext(dispatcher) {
        try {
            DataState.Success(apiCall())
        } catch (e: ClientRequestException) {
            val errorMessage = try {
                e.response.bodyAsText()
            } catch (inner: Exception) {
                "Invalid request"
            }
            DataState.Error(Exception(errorMessage))
        } catch (e: IOException) {
            DataState.Error(Exception("Network error: ${e.message ?: "Please check your connection"}"))
        } catch (e: ServerResponseException) {
            DataState.Error(Exception("Server error: ${e.message ?: "Please try again later"}"))
        } catch (e: Exception) {
            DataState.Error(Exception(e.message ?: "Something went wrong", e))
        }
    }
}