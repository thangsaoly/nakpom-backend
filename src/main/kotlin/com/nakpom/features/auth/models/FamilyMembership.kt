package com.nakpom.features.auth.models

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "family_memberships", uniqueConstraints = [
    UniqueConstraint(columnNames = ["user_id", "family_id"])
])
data class FamilyMembership(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val membershipId: Int? = null,
    
    @Column(name = "user_id", nullable = false)
    val userId: Int,
    
    @Column(name = "family_id", nullable = false)
    val familyId: Int,
    
    @Column(length = 20)
    val role: String = "member",
    
    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
