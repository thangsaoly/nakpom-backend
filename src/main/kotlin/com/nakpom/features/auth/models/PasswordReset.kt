package com.nakpom.features.auth.models

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "password_resets")
data class PasswordReset(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(nullable = false, length = 100)
    val email: String,

    @Column(nullable = false, unique = true, length = 255)
    val token: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime
)
