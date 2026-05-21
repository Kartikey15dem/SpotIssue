package org.example.project.core.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed class DataState<out T> {
    abstract val data: T?

    data object Loading : DataState<Nothing>() {
        override val data: Nothing? get() = null
    }

    data class Success<T>(
        override val data: T,
    ) : DataState<T>()

    data class Error<T>(
        val exception: Throwable,
        override val data: T? = null,
    ) : DataState<T>() {
        val message = exception.message.toString()
    }
}

fun <T> Flow<T>.asDataStateFlow(): Flow<DataState<T>> =
    map<T, DataState<T>> { DataState.Success(it) }
        .onStart { emit(DataState.Loading) }
        .catch { emit(DataState.Error(it, null)) }
