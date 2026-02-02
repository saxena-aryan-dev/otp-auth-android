package com.app.otpauth.analytics

import timber.log.Timber

object AnalyticsLogger {

    private const val TAG = "Analytics"

    fun logOtpGenerated(email: String) {
        Timber.tag(TAG).i("[Analytics] OTP generated for: %s", email)
    }

    fun logOtpSuccess(email: String) {
        Timber.tag(TAG).i("[Analytics] OTP verification successful for: %s", email)
    }

    fun logOtpFailure(email: String, reason: String) {
        Timber.tag(TAG).w("[Analytics] OTP verification failed for: %s | Reason: %s", email, reason)
    }

    fun logLogout(email: String) {
        Timber.tag(TAG).i("[Analytics] User logged out: %s", email)
    }
}
