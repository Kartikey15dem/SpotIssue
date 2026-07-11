package org.example.project.core.synchronization

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import io.ktor.utils.io.errors.IOException

sealed class SyncResult<T>(
    val data: T? = null,
    val hasMore: Boolean = true
) {
    class Success<T>(data: T, hasMore: Boolean) : SyncResult<T>(data, hasMore)
    sealed class Loading<T>(data: T? = null) : SyncResult<T>(data, true) {
        class Initial<T> : Loading<T>(null)
        class Refresh<T>(data: T) : Loading<T>(data)
        class Paging<T>(data: T) : Loading<T>(data)
    }
    class Error<T>(
        val error: Throwable, 
        data: T? = null, 
        val isOffline: Boolean = false
    ) : SyncResult<T>(data, true)
}

inline fun <ResultType, RequestType> networkBoundResourceFlow(
    crossinline query: () -> Flow<ResultType>,
    crossinline fetch: suspend (ResultType, Boolean) -> RequestType,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: (ResultType) -> Boolean = { true },
    forceRefresh: Boolean = false,
    crossinline hasMoreData: (ResultType, RequestType?) -> Boolean,
    crossinline onFailure: (Throwable) -> Unit = { }
): Flow<SyncResult<ResultType>> = flow {
    var hasMore = true
    var hasFetchedForcefully = false
    emit(SyncResult.Loading.Initial())
    println("[NBR] Loading Initial")
    
    emitAll(
        query().flatMapLatest { data ->
            flow {
                val shouldForce = forceRefresh && !hasFetchedForcefully
                val fetchNeeded = shouldForce || (hasMore && shouldFetch(data))
                
                if (fetchNeeded) {
                    val isLoadingInitial = data == null || (data is Collection<*> && data.isEmpty())
                    if (isLoadingInitial) {
                        emit(SyncResult.Loading.Initial())
                        println("[NBR] Loading Initial")
                    } else if (shouldForce) {
                        emit(SyncResult.Loading.Refresh(data))
                        println("[NBR] Loading Refresh")
                    } else {
                        emit(SyncResult.Loading.Paging(data))
                        println("[NBR] Loading Paging")
                    }
                    
                    try {
                        println("[NBR] Fetch")
                        val response = fetch(data, shouldForce)
                        if (shouldForce) {
                            hasFetchedForcefully = true
                        }
                        hasMore = hasMoreData(data, response)
                        println("[NBR] Save")
                        saveFetchResult(response)
                    } catch (e: Throwable) {
                        println("[NBR] Error: ${e.message}")
                        onFailure(e)
                        
                        // Ktor Network Exception heuristic or generic offline fallback
                        val isOffline = e is IOException 
                            || e.message?.contains("Unable to resolve host") == true 
                            || e.message?.contains("Failed to connect") == true
                            || e.message?.contains("Timeout") == true
                            
                        if (isOffline) {
                            println("[NBR] Offline")
                        }
                        emit(SyncResult.Error(e, data, isOffline = isOffline))
                    }
                } else {
                    println("[NBR] Success")
                    emit(SyncResult.Success(data, hasMore))
                }
            }
        }
    )
}
