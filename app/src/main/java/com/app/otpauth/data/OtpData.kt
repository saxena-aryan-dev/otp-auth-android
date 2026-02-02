package com.app.otpauth.data

data class OtpData(
    val code: String,
    val createdAt: Long,
    val expiresAt: Long,
    val attempts: Int = 0
)
