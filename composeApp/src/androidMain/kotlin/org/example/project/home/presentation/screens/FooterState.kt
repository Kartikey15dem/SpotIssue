package org.example.project.home.presentation.screens

sealed interface FooterState {
    data object Hidden : FooterState
    data object Loading : FooterState
    data class Error(val throwable: Throwable) : FooterState
    data object EndReached : FooterState
}
