package com.nakpom.features.auth.repository

import com.nakpom.features.auth.models.Family
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface FamilyRepository : JpaRepository<Family, Int> {
    fun findByInviteCode(inviteCode: String): Optional<Family>
    fun existsByInviteCode(inviteCode: String): Boolean
}
