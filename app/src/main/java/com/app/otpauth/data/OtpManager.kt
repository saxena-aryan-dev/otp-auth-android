package com.app.otpauth.data

import java.security.SecureRandom

sealed interface OtpValidationResult {
    data object Success : OtpValidationResult
    data object Expired : OtpValidationResult
    data class InvalidCode(val attemptsLeft: Int) : OtpValidationResult
    data object MaxAttemptsReached : OtpValidationResult
    data object NotFound : OtpValidationResult
}

class OtpManager {

    private val secureRandom = SecureRandom()
    private val otpStore: MutableMap<String, OtpData> = mutableMapOf()

    companion object {
        private const val OTP_LENGTH = 6
        private const val OTP_VALIDITY_MILLIS = 60_000L
        private const val MAX_ATTEMPTS = 3
    }

    fun generateOtp(email: String): String {
        val code = buildString {
            repeat(OTP_LENGTH) {
                append(secureRandom.nextInt(10))
            }
        }
        val now = System.currentTimeMillis()
        otpStore[email] = OtpData(
            code = code,
            createdAt = now,
            expiresAt = now + OTP_VALIDITY_MILLIS,
            attempts = 0
        )
        return code
    }

    fun validateOtp(email: String, code: String): OtpValidationResult {
        val otpData = otpStore[email] ?: return OtpValidationResult.NotFound

        if (System.currentTimeMillis() > otpData.expiresAt) {
            return OtpValidationResult.Expired
        }

        if (otpData.attempts >= MAX_ATTEMPTS) {
            return OtpValidationResult.MaxAttemptsReached
        }

        return if (otpData.code == code) {
            otpStore.remove(email)
            OtpValidationResult.Success
        } else {
            val updatedAttempts = otpData.attempts + 1
            otpStore[email] = otpData.copy(attempts = updatedAttempts)
            val attemptsLeft = MAX_ATTEMPTS - updatedAttempts
            if (attemptsLeft <= 0) {
                OtpValidationResult.MaxAttemptsReached
            } else {
                OtpValidationResult.InvalidCode(attemptsLeft = attemptsLeft)
            }
        }
    }

    fun getRemainingSeconds(email: String): Int {
        val otpData = otpStore[email] ?: return 0
        val remaining = (otpData.expiresAt - System.currentTimeMillis()) / 1000
        return remaining.coerceAtLeast(0).toInt()
    }
}
