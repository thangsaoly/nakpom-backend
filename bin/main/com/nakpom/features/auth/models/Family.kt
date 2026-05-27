package com.nakpom.features.auth.models

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "families")
data class Family(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val familyId: Int? = null,
    
    @Column(nullable = false, length = 100)
    val familyName: String,
    
    @Column(nullable = false, unique = true, length = 10)
    val inviteCode: String,
    
    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
