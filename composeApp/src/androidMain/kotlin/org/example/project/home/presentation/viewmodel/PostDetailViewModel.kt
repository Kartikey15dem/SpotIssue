package org.example.project.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.data.repository.PostRepository
import org.example.project.core.model.home.Post
import org.example.project.core.utils.DataState
import org.example.project.core.model.home.Comment
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import androidx.paging.cachedIn

data class PostDetailState(
    val post: Post? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val commentsFlow: Flow<PagingData<Comment>>? = null
)

class PostDetailViewModel(
    private val postId: String,
    private val postRepository: PostRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PostDetailState())
    val uiState: StateFlow<PostDetailState> = _uiState.asStateFlow()

    init {
        loadPost()
        loadComments()
    }

    private fun loadPost() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = postRepository.getPost(postId)) {
                is DataState.Success -> {
                    _uiState.update { it.copy(post = result.data, isLoading = false) }
                }
                is DataState.Error -> {
                    _uiState.update { it.copy(error = result.exception.message ?: "Failed to load post", isLoading = false) }
                }
                DataState.Loading -> {}
            }
        }
    }

    private fun loadComments() {
        val flow = postRepository.getPagedComments(postId).cachedIn(viewModelScope)
        _uiState.update { it.copy(commentsFlow = flow) }
    }
}
