package com.nakpom.features.auth.service

import com.nakpom.exception.EmailAlreadyExistsException
import com.nakpom.exception.InvalidCredentialsException
import com.nakpom.features.auth.models.Family
import com.nakpom.features.auth.models.FamilyMembership
import com.nakpom.features.auth.models.User
import com.nakpom.features.auth.models.dto.AuthResponse
import com.nakpom.features.auth.models.dto.LoginRequest
import com.nakpom.features.auth.models.dto.RegisterRequest
import com.nakpom.features.auth.repository.FamilyMembershipRepository
import com.nakpom.features.auth.repository.FamilyRepository
import com.nakpom.features.auth.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val familyRepository: FamilyRepository,
    private val familyMembershipRepository: FamilyMembershipRepository
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    /**
     * Registers a new user and automatically creates a "Krousa Me" family space.
     *
     * Flow:
     * 1. Validate email uniqueness
     * 2. Hash password with BCrypt
     * 3. Save User
     * 4. Generate invite code (NP-XXXX)
     * 5. Create Family("Krousa Me")
     * 6. Create FamilyMembership(role = "owner")
     * 7. Return AuthResponse with family details
     */
    @Transactional
    fun registerUser(request: RegisterRequest): AuthResponse {
        // 1. Check email uniqueness
        if (userRepository.existsByEmail(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }

        // 2. Hash password
        val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt(12))

        // 3. Save user
        val user = userRepository.save(
            User(
                email = request.email.trim().lowercase(),
                passwordHash = hashedPassword,
                fullName = request.fullName.trim()
            )
        )
        logger.info("User registered: id={}, email={}", user.userId, user.email)

        // 4. Generate unique invite code
        val inviteCode = generateUniqueInviteCode()

        // 5. Create default "Krousa Me" family space
        val family = familyRepository.save(
            Family(
                familyName = "Krousa Me",
                inviteCode = inviteCode
            )
        )
        logger.info("Family created: id={}, inviteCode={}", family.familyId, family.inviteCode)

        val userId = user.userId ?: error("Saved user is missing generated id")
        val familyId = family.familyId ?: error("Saved family is missing generated id")

        // 6. Link user to family as owner
        familyMembershipRepository.save(
            FamilyMembership(
                userId = userId,
                familyId = familyId,
                role = "owner"
            )
        )
        logger.info("Membership created: userId={}, familyId={}, role=owner", userId, familyId)

        // 7. Return response
        return AuthResponse(
            userId = userId,
            email = user.email,
            fullName = user.fullName,
            familyId = familyId,
            familyName = family.familyName,
            inviteCode = family.inviteCode,
            message = "Registration successful. Your family space 'Krousa Me' has been created."
        )
    }

    /**
     * Authenticates a user by verifying email and password.
     */
    fun loginUser(request: LoginRequest): AuthResponse {
        // Find user by email
        val user = userRepository.findByEmail(request.email.trim().lowercase())
            .orElseThrow { InvalidCredentialsException() }

        // Verify password
        if (!BCrypt.checkpw(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        logger.info("User logged in: id={}, email={}", user.userId, user.email)

        val userId = user.userId ?: throw InvalidCredentialsException()

        // Fetch user's primary family (first owned family)
        val memberships = familyMembershipRepository.findByUserId(userId)
        val primaryMembership = memberships.firstOrNull { it.role == "owner" } ?: memberships.firstOrNull()
        val family = primaryMembership?.let { familyRepository.findById(it.familyId).orElse(null) }

        return AuthResponse(
            userId = userId,
            email = user.email,
            fullName = user.fullName,
            familyId = family?.familyId,
            familyName = family?.familyName,
            inviteCode = family?.inviteCode,
            message = "Login successful"
        )
    }

    /**
     * Generates a unique invite code in the format NP-XXXX (alphanumeric).
     * Retries if a collision is detected.
     */
    private fun generateUniqueInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Omits ambiguous chars: 0/O, 1/I
        var attempts = 0
        while (attempts < 10) {
            val code = "NP-" + (1..4).map { chars.random() }.joinToString("")
            if (!familyRepository.existsByInviteCode(code)) {
                return code
            }
            attempts++
        }
        // Fallback: longer code to avoid collision
        val code = "NP-" + (1..6).map { chars.random() }.joinToString("")
        return code
    }
}
