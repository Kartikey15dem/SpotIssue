package org.example.project.auth.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.feature.auth.viewmodel.AuthEffect
import org.example.project.feature.auth.viewmodel.AuthIntent
import org.example.project.feature.auth.viewmodel.AuthUiState
import org.example.project.feature.auth.viewmodel.AuthViewModel
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTheme
import org.example.project.theme.IssueSpotTypography

@Composable
fun OTPScreen(
    navigateToNameCapture: (String) -> Unit,
    viewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AuthEffect.NavigateToNameCaptureScreen -> navigateToNameCapture(effect.email)
                is AuthEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OTPContent(
                uiState = uiState,
                onAction = { intent -> viewModel.handleIntent(intent) }
            )
        }
    }
}

@Composable
fun OTPContent(
    uiState: AuthUiState,
    onAction: (AuthIntent) -> Unit
) {
    val isLoading = uiState.isLoading
    val otpString = uiState.otp
    val otpDigits = remember(otpString) {
        List(6) { index -> otpString.getOrNull(index)?.toString() ?: "" }
    }
    val spacing = IssueSpotTheme.spacing
    val shapes = MaterialTheme.shapes

    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IssueSpotColors.Surface)
            .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(spacing.huge * 2))

        Text(
            text = "Enter verification code",
            style = IssueSpotTypography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = IssueSpotColors.OnSurface
        )

        Spacer(modifier = Modifier.height(spacing.small))

        Text(
            text = "We have sent you a 6 digit verification\ncode to ${uiState.email}",
            style = IssueSpotTypography.bodyMedium,
            color = IssueSpotColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(spacing.huge))

        BasicTextField(
            value = otpString,
            onValueChange = { newValue ->
                if (newValue.length <= 6) {
                    onAction(AuthIntent.OtpChanged(newValue))
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium),
            enabled = !isLoading,
            decorationBox = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.smallMedium),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(6) { index ->
                        val char = when {
                            index >= otpString.length -> ""
                            else -> otpString[index].toString()
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (char.isEmpty()) IssueSpotColors.Outline else IssueSpotColors.Primary,
                                    shape = MaterialTheme.shapes.small
                                )
                                .background(IssueSpotColors.Surface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (char.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(IssueSpotColors.Outline, CircleShape)
                                )
                            } else {
                                Text(
                                    text = char,
                                    style = IssueSpotTypography.bodyLarge.copy(
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        color = IssueSpotColors.OnSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(spacing.extraLarge))

        Button(
            onClick = { onAction(AuthIntent.VerifyOtpClicked) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = IssueSpotColors.Primary,
                contentColor = IssueSpotColors.OnPrimary
            ),
            shape = shapes.medium,
            enabled = otpString.length == 6 && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = IssueSpotColors.OnPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Verify OTP",
                    style = IssueSpotTypography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = spacing.extraSmall)
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.medium))

        TextButton(
            onClick = { onAction(AuthIntent.SendOtpClicked) },
            enabled = !isLoading
        ) {
            Text(
                text = "Resend Code",
                style = IssueSpotTypography.labelLarge,
                color = IssueSpotColors.Primary
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview
@Composable
fun OTPScreenPreview() {
    MaterialTheme {
        OTPContent(
            uiState = AuthUiState(),
            onAction = {}
        )
    }
}