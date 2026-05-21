package org.example.project.auth.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import org.example.project.auth.presentation.screens.LocationFetchScreenWithPermissions
import org.example.project.auth.presentation.screens.LoginScreen
import org.example.project.auth.presentation.screens.NameCaptureScreen
import org.example.project.auth.presentation.screens.OTPScreen
import org.example.project.auth.presentation.viewmodel.AuthViewModel
import org.example.project.core.navigation.Route
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthNavigation(
    modifier : Modifier = Modifier,
    onLocationFetched : () -> Unit
) {
    val authBackStack = rememberNavBackStack(Route.Auth.Login)
    val authViewModel : AuthViewModel = koinViewModel()

    NavDisplay(
        backStack = authBackStack,
        modifier = modifier,
        onBack = { authBackStack.removeLastOrNull()},
        entryProvider = entryProvider {
            entry<Route.Auth.Login> {
                LoginScreen(
                    onNavigateToOtp = {
                        authBackStack.add(Route.Auth.Otp)
                    },
                    viewModel = authViewModel
                )
            }
            entry<Route.Auth.Otp> {
                OTPScreen(
                    onAuthSuccess = {
                        authBackStack.removeLastOrNull()
                        authBackStack.add(Route.Auth.NameCapture)
                    },
                    viewModel = authViewModel
                )
            }
            entry<Route.Auth.NameCapture> {
                NameCaptureScreen(
                    onNameConfirmed = {
                        authBackStack.removeLastOrNull()
                    }
                )
            }
            entry<Route.Auth.LocationFetch> {
                LocationFetchScreenWithPermissions(
                    onLocationFetched = onLocationFetched
                )
            }
        }
    )

}
