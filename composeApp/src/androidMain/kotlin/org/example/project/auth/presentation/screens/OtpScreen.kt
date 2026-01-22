package org.example.project.auth.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.example.project.auth.presentation.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OTPScreen(
    email: String,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    viewModel: AuthViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // derive digit list from viewmodel otp (survives config changes)
    val otpString = uiState.otp

    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current

    // Auto-focus first field on screen load (safe access)
    LaunchedEffect(Unit) {
        viewModel.onEmailChange(email)
        kotlinx.coroutines.delay(300)
        focusRequesters.getOrNull(0)?.requestFocus()
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    OTPContent(
        email = email,
        otp = otpString,
        isLoading = uiState.isLoading,
        error = uiState.error,
        onOtpChange = { viewModel.onOtpChange(it) },
        onVerifyClick = { viewModel.verifyOtp(onVerifyClick) },
        onResendClick = onResendClick,
        focusRequesters = focusRequesters,
        focusManager = focusManager
    )
}

@Composable
fun OTPContent(
    email: String,
    otp: String,
    isLoading: Boolean,
    error: String?,
    onOtpChange: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    focusRequesters: List<FocusRequester>,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    val otpDigits = remember(otp) {
        List(6) { index -> otp.getOrNull(index)?.toString() ?: "" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "Enter verification code",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We have sent you a 6 digit verification\ncode to $email",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // OTP input fields
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            repeat(6) { index ->
                val digit = otpDigits.getOrNull(index) ?: ""
                OTPDigitField(
                    value = digit,
                    onValueChange = { newValue ->
                        // build new otp from current digits and update viewmodel
                        val list = MutableList(6) { i -> otpDigits.getOrNull(i) ?: "" }
                        list[index] = newValue
                        val combined = list.joinToString(separator = "") { it }
                        onOtpChange(combined)
                    },
                    onNext = {
                        focusRequesters.getOrNull(index + 1)?.requestFocus() ?: focusManager.clearFocus()
                    },
                    onPrevious = {
                        focusRequesters.getOrNull(index - 1)?.requestFocus()
                    },
                    focusRequester = focusRequesters.getOrNull(index) ?: FocusRequester(),
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
        if (error != null) {
            Text(
                text = error,
                fontSize = 12.sp,
                color = Color.Red,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Verify button - enabled when viewmodel otp has 6 digits
        Button(
            onClick = onVerifyClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4A6CF7)
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = otp.length == 6 && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Verify OTP",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resend option
        TextButton(
            onClick = onResendClick,
            enabled = !isLoading
        ) {
            Text(
                text = "Resend Code",
                color = Color(0xFF4A6CF7),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPDigitField(
    value: String,
    onValueChange: (String) -> Unit,
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    enabled: Boolean = true
) {
    var textFieldValue by remember(value) {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }

    // Keep a copy of previous text to detect deletions
    var previousText by remember { mutableStateOf(value) }

    // Sync when value changes externally
    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        }
        previousText = value
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newTextFieldValue ->
            val newText = newTextFieldValue.text.filter { it.isDigit() }

            when {
                newText.length == 1 -> {
                    // user entered a digit
                    textFieldValue = TextFieldValue(text = newText, selection = TextRange(1))
                    onValueChange(newText)
                    onNext()
                }
                newText.length > 1 -> {
                    // paste or multi-digit input: keep the first digit here and let caller propagate rest
                    val firstDigit = newText.first().toString()
                    textFieldValue = TextFieldValue(text = firstDigit, selection = TextRange(1))
                    onValueChange(firstDigit)
                    onNext()
                }
                newText.isEmpty() && previousText.isNotEmpty() -> {
                    // backspace deleted the character
                    textFieldValue = TextFieldValue(text = "", selection = TextRange(0))
                    onValueChange("")
                    onPrevious()
                }
                else -> {
                    // nothing or unsupported change
                    textFieldValue = TextFieldValue(text = newText, selection = TextRange(newText.length))
                }
            }

            previousText = newText
        },
        modifier = modifier
            .size(50.dp)
            .border(
                width = 1.dp,
                color = if (value.isEmpty()) Color.Gray.copy(alpha = 0.5f) else Color(0xFF4A6CF7),
                shape = RoundedCornerShape(8.dp)
            )
            .background(Color.White)
            .focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        singleLine = true,
        enabled = enabled,
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        ),
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (value.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                    )
                }
                innerTextField()
            }
        }
    )
}

@Preview
@Composable
fun OTPScreenPreview() {
    OTPScreen(
        email = "test@example.com",
        onVerifyClick = {},
        onResendClick = {}
    )
}
