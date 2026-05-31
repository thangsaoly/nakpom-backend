package com.nakpom.features.auth.models.dto

/**
 * Response returned after a user successfully joins a family via invite code.
 */
data class JoinFamilyResponse(
    val familyId: Int,
    val familyName: String,
    val inviteCode: String,
    val role: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
