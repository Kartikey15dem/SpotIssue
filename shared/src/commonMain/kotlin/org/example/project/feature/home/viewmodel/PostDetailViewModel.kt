package org.example.project.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.data.repository.PostRepository
import org.example.project.core.model.home.Comment
import org.example.project.core.model.home.Post
import org.example.project.core.utils.DataState

class PostDetailViewModel(
    private val postId: String,
    private val postRepository: PostRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PostDetailState())
    val uiState: StateFlow<PostDetailState> = _uiState.asStateFlow()

    val commentsFlow: Flow<PagingData<Comment>> =
        postRepository.getPagedComments(postId).cachedIn(viewModelScope)

    init {
        load()
    }

    fun onIntent(intent: PostDetailIntent) {
        when (intent) {
            PostDetailIntent.Refresh -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.post == null, error = null) }
            when (val result = postRepository.getPost(postId)) {
                is DataState.Success -> _uiState.update {
                    it.copy(post = result.data, isLoading = false)
                }
                is DataState.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.exception.message ?: "Unable to load post")
                }
                DataState.Loading -> Unit
            }
            when (val result = postRepository.getCommentsList(postId)) {
                is DataState.Success -> _uiState.update { it.copy(comments = result.data) }
                is DataState.Error -> Unit
                DataState.Loading -> Unit
            }
        }
    }
}

sealed interface PostDetailIntent {
    data object Refresh : PostDetailIntent
}

data class PostDetailState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
