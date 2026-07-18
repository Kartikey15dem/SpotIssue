package org.example.project.auth.presentation.screens

import org.example.project.core.components.AppErrorDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.R
import org.example.project.feature.auth.viewmodel.AuthEffect
import org.example.project.feature.auth.viewmodel.AuthIntent
import org.example.project.feature.auth.viewmodel.AuthUiState
import org.example.project.feature.auth.viewmodel.AuthViewModel
import org.example.project.theme.IssueSpotColors
import org.example.project.theme.IssueSpotTheme
import org.example.project.theme.IssueSpotTypography

@Composable
fun LoginScreen(
    onNavigateToOtp: () -> Unit,
    viewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AuthEffect.NavigateToOtpScreen -> onNavigateToOtp()
                is AuthEffect.ShowDialog -> {
                    errorDialogMessage = effect.message
                }
                else -> Unit
            }
        }
    }

    
    errorDialogMessage?.let { message ->
        AppErrorDialog(
            message = message,
            onDismiss = { errorDialogMessage = null }
        )
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LoginContent(
                uiState = uiState,
                onAction = { intent -> viewModel.handleIntent(intent) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginContent(
    uiState: AuthUiState,
    onAction: (AuthIntent) -> Unit
) {
    val isLoading = uiState.isLoading
    val spacing = IssueSpotTheme.spacing
    val shapes = MaterialTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IssueSpotColors.Surface)
            .padding(spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(spacing.huge))

        Image(
            painter = painterResource(id = R.drawable.logo_issue),
            contentDescription = "Company Logo",
            modifier = Modifier
                .size(160.dp)
                .clip(shapes.large)
                .background(IssueSpotColors.Surface),
            contentScale = ContentScale.Crop
        )

        Text(
            text = "IssueSpot",
            style = IssueSpotTypography.headlineLarge,
            color = IssueSpotColors.OnSurface,
            modifier = Modifier.padding(top = spacing.medium)
        )

        Spacer(modifier = Modifier.height(spacing.huge))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = { onAction(AuthIntent.EmailChanged(it)) },
            label = { Text("Email Address", style = IssueSpotTypography.bodyMedium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IssueSpotColors.Primary,
                focusedLabelColor = IssueSpotColors.Primary,
                unfocusedBorderColor = IssueSpotColors.Outline,
                cursorColor = IssueSpotColors.Primary
            ),
            textStyle = IssueSpotTypography.bodyLarge,
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(spacing.large))

        Text(
            text = "We will use your email address for verification\npurpose. An OTP will be sent to your email.",
            style = IssueSpotTypography.bodySmall,
            color = IssueSpotColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(spacing.extraLarge))

        Button(
            onClick = { onAction(AuthIntent.SendOtpClicked) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = IssueSpotColors.Primary,
                contentColor = IssueSpotColors.OnPrimary
            ),
            shape = shapes.medium,
            enabled = uiState.email.contains("@") && uiState.email.contains(".") && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = IssueSpotColors.OnPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Login / Sign up",
                    style = IssueSpotTypography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = spacing.extraSmall)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        LoginContent(
            uiState = AuthUiState(),
            onAction = {}
        )
    }
}