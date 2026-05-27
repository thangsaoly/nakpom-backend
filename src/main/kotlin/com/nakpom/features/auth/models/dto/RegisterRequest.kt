package com.nakpom.features.auth.models.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @field:Size(max = 100, message = "Email must not exceed 100 characters")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
    val password: String,

    @field:NotBlank(message = "Full name is required")
    @field:Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    val fullName: String
)
