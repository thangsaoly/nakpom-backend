package com.nakpom.features.auth.models.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * Request body for POST /api/v1/auth/forgot-password.
 *
 * The user supplies only their email address.
 * The backend handles token generation and email dispatch internally.
 */
data class ForgotPasswordRequest(

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be a valid email address")
    val email: String
)
