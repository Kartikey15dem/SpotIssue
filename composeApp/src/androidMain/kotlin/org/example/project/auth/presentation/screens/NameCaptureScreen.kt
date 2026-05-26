package org.example.project.auth.presentation.screens

import androidx.compose.material3.Snackbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.auth.presentation.viewmodel.AuthEffect
import org.example.project.auth.presentation.viewmodel.NameCaptureEffect
import org.example.project.auth.presentation.viewmodel.NameCaptureIntent
import org.example.project.auth.presentation.viewmodel.NameCaptureUiState
import org.example.project.auth.presentation.viewmodel.NameCaptureViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NameCaptureScreen(
    viewModel: NameCaptureViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NameCaptureEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
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
            NameCaptureContent(
                uiState = uiState,
                onAction = { intent -> viewModel.handleIntent(intent) }
            )
        }
    }
}

//@Composable
//fun NameCaptureDialogs(
//    dialogState: NameCaptureUiState.DialogState?,
//    onDismiss: () -> Unit
//) {
//    when (dialogState) {
//        is NameCaptureUiState.DialogState.Error -> {
//            AlertDialog(
//                onDismissRequest = onDismiss,
//                title = { Text(text = "Profile Error") },
//                text = { Text(text = dialogState.message) },
//                confirmButton = {
//                    TextButton(onClick = onDismiss) {
//                        Text("OK", color = Color(0xFF4A6CF7))
//                    }
//                },
//                containerColor = Color.White
//            )
//        }
//        else -> Unit
//    }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameCaptureContent(
    uiState: NameCaptureUiState,
    onAction: (NameCaptureIntent) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val isLoading = uiState.dialogState == NameCaptureUiState.DialogState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "Tell us the name by which you want to post issues",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))


        OutlinedTextField(
            value = uiState.name,
            onValueChange = { onAction(NameCaptureIntent.NameChanged(it)) },
            label = { Text("Full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4A6CF7),
                focusedLabelColor = Color(0xFF4A6CF7)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                focusManager.clearFocus()
                onAction(NameCaptureIntent.SubmitClicked)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A6CF7)),
            shape = RoundedCornerShape(8.dp),
            enabled = uiState.name.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Continue",
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