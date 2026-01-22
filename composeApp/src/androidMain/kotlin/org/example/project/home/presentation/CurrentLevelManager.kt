package org.example.project.home.presentation

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.home.domain.models.PostLevel

class CurrentLevelManager {
    private val _currentLevel = MutableStateFlow(PostLevel.LOCALITY)
    val currentLevel: StateFlow<PostLevel> = _currentLevel.asStateFlow()

    fun updateLevel(level: PostLevel) {
        _currentLevel.value = level
    }

    fun getCurrentLevelValue(): PostLevel = _currentLevel.value
}
fun PostLevel.getColor(): Color {
    return when (this) {
        PostLevel.LOCALITY -> Color(0xFF2E7D32)   // Dark Green (matches your image)
        PostLevel.DISTRICT -> Color(0xFF1976D2)   // Medium Blue (matches your image)
        PostLevel.STATE -> Color(0xFF7B1FA2)      // Purple (matches your image)
        PostLevel.NATIONAL -> Color(0xFFD32F2F)   // Red (matches your image)
    }
}
