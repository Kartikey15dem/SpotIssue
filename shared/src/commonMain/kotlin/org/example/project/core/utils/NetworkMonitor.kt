package org.example.project.core.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}

fun <T> NetworkMonitor.withNetworkCheck(
    upstream: Flow<DataState<T>>,
): Flow<DataState<T>> = combine(isOnline, upstream) { isOnline, dataState ->
    when {
        dataState is DataState.Success -> dataState
        dataState is DataState.Loading -> dataState
        !isOnline -> DataState.Error(Exception(NETWORK_ERROR_MESSAGE))
        else -> dataState
    }
}

