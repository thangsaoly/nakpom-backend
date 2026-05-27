package com.nakpom.features.auth.routing

import com.nakpom.features.auth.models.dto.AuthResponse
import com.nakpom.features.auth.models.dto.LoginRequest
import com.nakpom.features.auth.models.dto.RegisterRequest
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
}
