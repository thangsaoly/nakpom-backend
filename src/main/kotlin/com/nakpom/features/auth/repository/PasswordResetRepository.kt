package com.nakpom.features.auth.repository

import com.nakpom.features.auth.models.PasswordReset
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PasswordResetRepository : JpaRepository<PasswordReset, Int> {
    fun findByToken(token: String): Optional<PasswordReset>
    fun findByEmail(email: String): List<PasswordReset>
    fun deleteByEmail(email: String)
}
