package com.nakpom

import com.nakpom.features.auth.service.EmailService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Manual integration smoke test for the Resend email pipeline.
 *
 * This test sends a REAL email — run it on-demand only (not in CI).
 *
 * How to run:
 *   ./gradlew test --tests "com.nakpom.EmailSmokeTest"
 *
 * Expected result: an email with a purple "Reset Password" button
 * arrives in the inbox at the recipient address below.
 */
@SpringBootTest
class EmailSmokeTest @Autowired constructor(
    private val emailService: EmailService
) {

    @Test
    fun `send password reset email via Resend`() {
        println("📧 Sending test password-reset email via Resend...")
        emailService.sendResetEmail(
            recipientEmail = "lovepapa987@gmail.com",
            secureToken    = "TEST123"
        )
        println("✅ Done. Check your inbox at lovepapa987@gmail.com.")
    }
}
