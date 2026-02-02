package com.app.otpauth.ui.otp

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.otpauth.viewmodel.AuthUiState

@Composable
fun OtpScreen(
    state: AuthUiState.OtpEntry,
    onVerifyOtp: (String) -> Unit,
    onResendOtp: () -> Unit
) {
    var otpDigits by rememberSaveable { mutableStateOf(List(6) { "" }) }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val maskedEmail = maskEmail(state.email)

    // Show OTP via Snackbar when a new OTP is generated (since there's no backend)
    LaunchedEffect(state.otpDisplay) {
        if (state.otpDisplay.isNotEmpty()) {
            snackbarHostState.showSnackbar(
                message = "Your OTP: ${state.otpDisplay}",
                withDismissAction = true
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Verify OTP",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter the code sent to $maskedEmail",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Countdown timer with circular progress
        Box(contentAlignment = Alignment.Center) {
            val progress = state.remainingSeconds / 60f
            val timerColor by animateColorAsState(
                targetValue = if (state.remainingSeconds <= 10)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary,
                label = "timerColor"
            )

            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(80.dp),
                color = timerColor,
                strokeWidth = 4.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                text = formatTime(state.remainingSeconds),
                style = MaterialTheme.typography.titleMedium,
                color = timerColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // OTP digit boxes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 6) {
                        OutlinedTextField(
                            value = otpDigits[i],
                            onValueChange = { value ->
                                if (value.length <= 1 && value.all { it.isDigit() }) {
                                    val newDigits = otpDigits.toMutableList()
                                    newDigits[i] = value
                                    otpDigits = newDigits
                                    if (value.isNotEmpty() && i < 5) {
                                        focusRequesters[i + 1].requestFocus()
                                    }
                                    if (value.isEmpty() && i > 0) {
                                        focusRequesters[i - 1].requestFocus()
                                    }
                                    // Auto-submit when all digits are entered
                                    if (newDigits.all { it.isNotEmpty() }) {
                                        focusManager.clearFocus()
                                        onVerifyOtp(newDigits.joinToString(""))
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequesters[i]),
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                textAlign = TextAlign.Center
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = if (i < 5) ImeAction.Next else ImeAction.Done
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Attempts remaining
                if (state.attemptsLeft < 3) {
                    Text(
                        text = "${state.attemptsLeft} attempt(s) remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.attemptsLeft <= 1)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Error message
                if (state.error != null) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Verify button
                Button(
                    onClick = {
                        val code = otpDigits.joinToString("")
                        if (code.length == 6) {
                            onVerifyOtp(code)
                        }
                    },
                    enabled = otpDigits.all { it.isNotEmpty() } && !state.isExpired && state.attemptsLeft > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Verify", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Resend button
                OutlinedButton(
                    onClick = {
                        otpDigits = List(6) { "" }
                        onResendOtp()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (state.isExpired || state.attemptsLeft <= 0)
                            "Resend OTP"
                        else
                            "Resend OTP",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
    } // Scaffold
}

private fun maskEmail(email: String): String {
    val parts = email.split("@")
    if (parts.size != 2) return email
    val name = parts[0]
    val masked = if (name.length <= 2) {
        "${name.first()}***"
    } else {
        "${name.first()}${"*".repeat(name.length - 2)}${name.last()}"
    }
    return "$masked@${parts[1]}"
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
