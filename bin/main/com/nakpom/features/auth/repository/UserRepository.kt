package com.nakpom.features.auth.repository

import com.nakpom.features.auth.models.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, Int> {
    fun findByEmail(email: String): Optional<User>
    fun existsByEmail(email: String): Boolean
}
