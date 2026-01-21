package org.example.project.home.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.home.presentation.models.PostLevel

class CurrentLevelManager {
    private val _currentLevel = MutableStateFlow(PostLevel.LOCALITY)
    val currentLevel: StateFlow<PostLevel> = _currentLevel.asStateFlow()

    fun updateLevel(level: PostLevel) {
        _currentLevel.value = level
    }

    fun getCurrentLevelValue(): PostLevel = _currentLevel.value
}
