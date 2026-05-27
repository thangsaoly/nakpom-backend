package com.nakpom.features.auth.models.dto

import java.time.LocalDateTime

data class AuthResponse(
    val userId: Int,
    val email: String,
    val fullName: String,
    val familyId: Int? = null,
    val familyName: String? = null,
    val inviteCode: String? = null,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
