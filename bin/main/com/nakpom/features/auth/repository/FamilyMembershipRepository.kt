package com.nakpom.features.auth.repository

import com.nakpom.features.auth.models.FamilyMembership
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface FamilyMembershipRepository : JpaRepository<FamilyMembership, Int> {
    fun findByUserIdAndFamilyId(userId: Int, familyId: Int): Optional<FamilyMembership>
    fun findByUserId(userId: Int): List<FamilyMembership>
    fun findByFamilyId(familyId: Int): List<FamilyMembership>
    fun existsByUserIdAndFamilyId(userId: Int, familyId: Int): Boolean
}
