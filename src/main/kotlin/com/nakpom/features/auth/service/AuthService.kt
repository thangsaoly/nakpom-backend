package com.nakpom.features.auth.service

import com.nakpom.exception.EmailAlreadyExistsException
import com.nakpom.exception.InvalidCredentialsException
import com.nakpom.exception.InvalidTokenException
import com.nakpom.features.auth.models.Family
import com.nakpom.features.auth.models.FamilyMembership
import com.nakpom.features.auth.models.PasswordReset
import com.nakpom.features.auth.models.User
import com.nakpom.features.auth.models.dto.AuthResponse
import com.nakpom.features.auth.models.dto.LoginRequest
import com.nakpom.features.auth.models.dto.RegisterRequest
import com.nakpom.features.auth.repository.FamilyMembershipRepository
import com.nakpom.features.auth.repository.FamilyRepository
import com.nakpom.features.auth.repository.PasswordResetRepository
import com.nakpom.features.auth.repository.UserRepository
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val familyRepository: FamilyRepository,
    private val familyMembershipRepository: FamilyMembershipRepository,
    private val passwordResetRepository: PasswordResetRepository,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    private val secureRandom = SecureRandom()

    /**
     * Registers a new user and automatically creates a "Krousa Me" family space.
     *
     * Flow:
     * 1. Validate email uniqueness
     * 2. Hash password with BCrypt
     * 3. Save User
     * 4. Generate invite code (NP-XXXXXX)
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
     * Step 1 of password reset: validate email exists, generate a token,
     * save it with a 15-minute expiry, then dispatch the reset email.
     *
     * Security note: we return the same generic success message whether or
     * not the email is registered — this prevents user enumeration attacks.
     */
    @Transactional
    fun requestPasswordReset(email: String) {
        val normalizedEmail = email.trim().lowercase()

        // Only proceed if the account actually exists
        if (!userRepository.existsByEmail(normalizedEmail)) {
            // Intentionally silent: do not reveal whether the email is registered
            logger.info("Password reset requested for unknown email (suppressed): {}", normalizedEmail)
            return
        }

        // Delete any previous tokens for this email so there is only ever one active at a time
        passwordResetRepository.deleteByEmail(normalizedEmail)

        // Generate a cryptographically secure 32-character hex token
        val tokenBytes = ByteArray(16)
        secureRandom.nextBytes(tokenBytes)
        val token = tokenBytes.joinToString("") { "%02x".format(it) }

        // Save the token with a 15-minute expiry window
        passwordResetRepository.save(
            PasswordReset(
                email = normalizedEmail,
                token = token,
                expiresAt = LocalDateTime.now().plusMinutes(15)
            )
        )
        logger.info("Password reset token generated for email={}", normalizedEmail)

        // Dispatch the email with the embedded token link
        emailService.sendResetEmail(normalizedEmail, token)
    }

    /**
     * Step 2 of password reset: validate the token, hash the new password,
     * update the user's record, then delete the token to prevent re-use.
     */
    @Transactional
    fun resetPassword(token: String, newPassword: String) {
        // 1. Look up the token
        val resetRecord = passwordResetRepository.findByToken(token)
            .orElseThrow { InvalidTokenException() }

        // 2. Check it has not expired
        if (LocalDateTime.now().isAfter(resetRecord.expiresAt)) {
            passwordResetRepository.delete(resetRecord) // clean up expired token
            throw InvalidTokenException()
        }

        // 3. Find the user and update their password hash
        val user = userRepository.findByEmail(resetRecord.email)
            .orElseThrow { InvalidTokenException() } // token points to a deleted user — treat as invalid

        val updatedUser = user.copy(passwordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(12)))
        userRepository.save(updatedUser)
        logger.info("Password reset successful for userId={}", user.userId)

        // 4. Delete the token so it cannot be reused
        passwordResetRepository.delete(resetRecord)
    }

    /**
     * Generates a unique invite code in the format NP-XXXXXX (alphanumeric).
     * Retries if a collision is detected.
     */
    private fun generateUniqueInviteCode(): String {
        var attempts = 0
        while (attempts < 10) {
            val code = "NP-" + (1..INVITE_CODE_LENGTH)
                .map { INVITE_CODE_CHARS[secureRandom.nextInt(INVITE_CODE_CHARS.length)] }
                .joinToString("")
            if (!familyRepository.existsByInviteCode(code)) {
                return code
            }
            attempts++
        }
        error("Unable to generate a unique invite code after $attempts attempts")
    }

    companion object {
        private const val INVITE_CODE_LENGTH = 6
        private const val INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    }
}
