package com.app.otpauth.viewmodel

sealed interface AuthUiState {

    data class EmailEntry(
        val email: String = "",
        val isLoading: Boolean = false,
        val error: String? = null
    ) : AuthUiState

    data class OtpEntry(
        val email: String,
        val otpDisplay: String = "",
        val remainingSeconds: Int = 60,
        val attemptsLeft: Int = 3,
        val error: String? = null,
        val isExpired: Boolean = false
    ) : AuthUiState

    data class Session(
        val email: String,
        val sessionStartMillis: Long,
        val elapsedSeconds: Long = 0
    ) : AuthUiState
}
