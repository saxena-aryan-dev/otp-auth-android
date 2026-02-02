package com.app.otpauth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.otpauth.analytics.AnalyticsLogger
import com.app.otpauth.data.OtpManager
import com.app.otpauth.data.OtpValidationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val otpManager = OtpManager()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.EmailEntry())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var sessionTimerJob: Job? = null

    fun onEmailChanged(email: String) {
        val current = _uiState.value
        if (current is AuthUiState.EmailEntry) {
            _uiState.value = current.copy(email = email, error = null)
        }
    }

    fun sendOtp() {
        val current = _uiState.value
        if (current !is AuthUiState.EmailEntry) return

        val email = current.email.trim()
        if (!isValidEmail(email)) {
            _uiState.value = current.copy(error = "Please enter a valid email address")
            return
        }

        _uiState.value = current.copy(isLoading = true, error = null)

        val otp = otpManager.generateOtp(email)
        AnalyticsLogger.logOtpGenerated(email)

        _uiState.value = AuthUiState.OtpEntry(
            email = email,
            otpDisplay = otp,
            remainingSeconds = 60,
            attemptsLeft = 3
        )

        startCountdown(email)
    }

    fun verifyOtp(code: String) {
        val current = _uiState.value
        if (current !is AuthUiState.OtpEntry) return

        when (val result = otpManager.validateOtp(current.email, code)) {
            is OtpValidationResult.Success -> {
                AnalyticsLogger.logOtpSuccess(current.email)
                countdownJob?.cancel()
                val now = System.currentTimeMillis()
                _uiState.value = AuthUiState.Session(
                    email = current.email,
                    sessionStartMillis = now,
                    elapsedSeconds = 0
                )
                startSessionTimer()
            }
            is OtpValidationResult.Expired -> {
                AnalyticsLogger.logOtpFailure(current.email, "OTP expired")
                _uiState.value = current.copy(
                    error = "OTP has expired. Please resend.",
                    isExpired = true
                )
            }
            is OtpValidationResult.InvalidCode -> {
                AnalyticsLogger.logOtpFailure(current.email, "Invalid code, ${result.attemptsLeft} attempts left")
                _uiState.value = current.copy(
                    error = "Incorrect OTP. ${result.attemptsLeft} attempt(s) remaining.",
                    attemptsLeft = result.attemptsLeft
                )
            }
            is OtpValidationResult.MaxAttemptsReached -> {
                AnalyticsLogger.logOtpFailure(current.email, "Max attempts reached")
                countdownJob?.cancel()
                _uiState.value = current.copy(
                    error = "Maximum attempts reached. Please resend OTP.",
                    attemptsLeft = 0
                )
            }
            is OtpValidationResult.NotFound -> {
                AnalyticsLogger.logOtpFailure(current.email, "OTP not found")
                _uiState.value = current.copy(error = "No OTP found. Please resend.")
            }
        }
    }

    fun resendOtp() {
        val current = _uiState.value
        if (current !is AuthUiState.OtpEntry) return

        countdownJob?.cancel()

        val otp = otpManager.generateOtp(current.email)
        AnalyticsLogger.logOtpGenerated(current.email)

        _uiState.value = AuthUiState.OtpEntry(
            email = current.email,
            otpDisplay = otp,
            remainingSeconds = 60,
            attemptsLeft = 3,
            error = null,
            isExpired = false
        )

        startCountdown(current.email)
    }

    fun logout() {
        val current = _uiState.value
        if (current is AuthUiState.Session) {
            AnalyticsLogger.logLogout(current.email)
        }

        countdownJob?.cancel()
        sessionTimerJob?.cancel()
        _uiState.value = AuthUiState.EmailEntry()
    }

    private fun startCountdown(email: String) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = otpManager.getRemainingSeconds(email)
            while (remaining > 0) {
                val current = _uiState.value
                if (current is AuthUiState.OtpEntry) {
                    _uiState.value = current.copy(remainingSeconds = remaining)
                }
                delay(1000)
                remaining = otpManager.getRemainingSeconds(email)
            }
            val current = _uiState.value
            if (current is AuthUiState.OtpEntry) {
                _uiState.value = current.copy(
                    remainingSeconds = 0,
                    isExpired = true
                )
            }
        }
    }

    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value
                if (current is AuthUiState.Session) {
                    val elapsed = (System.currentTimeMillis() - current.sessionStartMillis) / 1000
                    _uiState.value = current.copy(elapsedSeconds = elapsed)
                } else {
                    break
                }
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        sessionTimerJob?.cancel()
    }
}
