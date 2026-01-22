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
import org.example.project.core.navigation.Route

@Composable
fun AuthNavigation(
    modifier : Modifier = Modifier,
    onLocationFetched : () -> Unit
) {
    val authBackStack = rememberNavBackStack(Route.Auth.Login)

    NavDisplay(
        backStack = authBackStack,
        modifier = modifier,
        onBack = { authBackStack.removeLastOrNull()},
        entryProvider = entryProvider {
            entry<Route.Auth.Login> {
                LoginScreen(
                    onLoginClick = { email ->
                        authBackStack.removeLastOrNull()
                        authBackStack.add(Route.Auth.Otp(email = email))
                    }
                )
            }
            entry<Route.Auth.Otp> { key ->
                OTPScreen(
                    email = key.email,
                    onVerifyClick = {
                        authBackStack.removeLastOrNull()
                        authBackStack.add(Route.Auth.NameCapture(email = key.email))
                    },
                    onResendClick = {
                        // Handle resend
                    }
                )
            }
            entry<Route.Auth.NameCapture> { key ->
                NameCaptureScreen(
                    email = key.email,
                    onNameConfirmed = { name ->
                        authBackStack.removeLastOrNull()
                        authBackStack.add(Route.Auth.LocationFetch(name = name, email = key.email))
                    }
                )
            }
            entry<Route.Auth.LocationFetch> { key ->
                LocationFetchScreenWithPermissions(
                    name = key.name,
                    email = key.email,
                    onLocationFetched = onLocationFetched
                )
            }
        }
    )

}
