package org.example.project.auth.presentation.screens

import androidx.compose.material3.Snackbar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.example.project.auth.presentation.viewmodel.AuthEffect
import org.example.project.auth.presentation.viewmodel.AuthIntent
import org.example.project.auth.presentation.viewmodel.AuthUiState
import org.example.project.auth.presentation.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onNavigateToOtp: () -> Unit,
    viewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AuthEffect.NavigateToOtpScreen -> onNavigateToOtp()
                is AuthEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                androidx.compose.material3.Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF323232),
                    contentColor = Color.White,
                    actionColor = Color(0xFF4A6CF7)
                )
            }
        },
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

//@Composable
//fun AuthDialogs(
//    dialogState: AuthUiState.DialogState?,
//    onDismiss: () -> Unit
//) {
//    when (dialogState) {
//        is AuthUiState.DialogState.Error -> {
//            AlertDialog(
//                onDismissRequest = onDismiss,
//                title = { Text(text = "Authentication Error") },
//                text = { Text(text = dialogState.message) },
//                confirmButton = {
//                    TextButton(onClick = onDismiss) {
//                        Text("OK", color = Color(0xFF4A6CF7))
//                    }
//                },
//                containerColor = Color.White
//            )
//        }
//        else -> Unit // Loading is handled directly on the button for better UX
//    }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginContent(
    uiState: AuthUiState,
    onAction: (AuthIntent) -> Unit
) {
    val isLoading = uiState.dialogState == AuthUiState.DialogState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = R.drawable.logo_issue),
            contentDescription = "Company Logo",
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White),
            contentScale = ContentScale.Crop
        )

        Text(
            text = "IssueSpot",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(top = 20.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = { onAction(AuthIntent.EmailChanged(it)) },
            label = { Text("Email Address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4A6CF7),
                focusedLabelColor = Color(0xFF4A6CF7)
            ),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "We will use your email address for verification\npurpose. An OTP will be sent to your email.",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onAction(AuthIntent.SendOtpClicked) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6CF7)),
            shape = RoundedCornerShape(8.dp),
            enabled = uiState.email.contains("@") && uiState.email.contains(".") && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Login/Sign up",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
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