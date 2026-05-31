package com.nakpom.features.auth.routing

import com.nakpom.features.auth.models.dto.AuthResponse
import com.nakpom.features.auth.models.dto.ForgotPasswordRequest
import com.nakpom.features.auth.models.dto.LoginRequest
import com.nakpom.features.auth.models.dto.RegisterRequest
import com.nakpom.features.auth.models.dto.ResetPasswordRequest
import com.nakpom.features.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val authService: AuthService) {

    /**
     * POST /api/v1/auth/register
     *
     * Registers a new user and automatically creates a "Krousa Me" family space.
     */
    @PostMapping("/register")
    fun registerUser(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val response = authService.registerUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * POST /api/v1/auth/login
     *
     * Authenticates a user with email and password.
     */
    @PostMapping("/login")
    fun loginUser(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.loginUser(request)
        return ResponseEntity.ok(response)
    }

    /**
     * POST /api/v1/auth/forgot-password
     *
     * Accepts an email address and dispatches a password-reset link.
     * Always returns 200 OK regardless of whether the email is registered
     * (prevents user enumeration).
     */
    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<Map<String, Any>> {
        authService.requestPasswordReset(request.email)
        val response = mapOf(
            "message" to "If that email is registered, a reset link has been sent.",
            "timestamp" to System.currentTimeMillis()
        )
        return ResponseEntity.ok(response)
    }

    /**
     * POST /api/v1/auth/reset-password
     *
     * Accepts a token and new password, validates the token, and updates the user's password.
     */
    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<Map<String, Any>> {
        authService.resetPassword(request.token, request.newPassword)
        val response = mapOf(
            "message" to "Password has been reset successfully.",
            "timestamp" to System.currentTimeMillis()
        )
        return ResponseEntity.ok(response)
    }
}
