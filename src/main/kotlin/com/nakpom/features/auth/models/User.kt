package com.nakpom.features.auth.models

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val userId: Int? = null,
    
    @Column(nullable = false, unique = true, length = 100)
    val email: String,
    
    @Column(nullable = false)
    val passwordHash: String,
    
    @Column(nullable = false, length = 100)
    val fullName: String,
    
    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
