package com.nakpom.features.auth.models.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request body for POST /api/v1/auth/reset-password.
 *
 * The user supplies the token from their email link and their chosen new password.
 */
data class ResetPasswordRequest(

    @field:NotBlank(message = "Token is required")
    val token: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    val newPassword: String
)
