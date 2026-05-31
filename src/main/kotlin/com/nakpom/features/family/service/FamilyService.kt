package com.nakpom.features.family.service

import com.nakpom.exception.MembershipAlreadyExistsException
import com.nakpom.exception.ResourceNotFoundException
import com.nakpom.features.auth.models.FamilyMembership
import com.nakpom.features.auth.models.dto.JoinFamilyResponse
import com.nakpom.features.auth.repository.FamilyMembershipRepository
import com.nakpom.features.auth.repository.FamilyRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FamilyService(
    private val familyRepository: FamilyRepository,
    private val familyMembershipRepository: FamilyMembershipRepository
) {
    private val logger = LoggerFactory.getLogger(FamilyService::class.java)

    /**
     * Joins an existing family using a 6-character invite code.
     *
     * Flow:
     * 1. Look up the family by invite code → 404 if not found
     * 2. Check the user isn't already a member → 409 if duplicate
     * 3. Insert a new membership row with role = "member"
     * 4. Return family details in a JoinFamilyResponse
     */
    @Transactional
    fun joinFamilyByCode(userId: Int, code: String): JoinFamilyResponse {
        // 1. Find the family by invite code (case-insensitive trim for safety)
        val family = familyRepository.findByInviteCode(code.trim().uppercase())
            .orElseThrow { ResourceNotFoundException("No family found with invite code: $code") }

        val familyId = family.familyId ?: error("Saved family is missing generated id")

        // 2. Prevent duplicate membership
        if (familyMembershipRepository.existsByUserIdAndFamilyId(userId, familyId)) {
            throw MembershipAlreadyExistsException("You are already a member of '${family.familyName}'")
        }

        // 3. Insert membership row
        familyMembershipRepository.save(
            FamilyMembership(
                userId = userId,
                familyId = familyId,
                role = "member"
            )
        )
        logger.info("User {} joined family {} via invite code {}", userId, familyId, code)

        // 4. Return family details
        return JoinFamilyResponse(
            familyId = familyId,
            familyName = family.familyName,
            inviteCode = family.inviteCode,
            role = "member",
            message = "You have joined '${family.familyName}' successfully."
        )
    }
}
