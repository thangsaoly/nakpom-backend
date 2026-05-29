package com.nakpom

import com.nakpom.features.auth.service.EmailService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

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
class EmailSmokeTest {

    companion object {
        /**
         * Load .env manually before any test runs.
         * Spring Boot normally does this via spring.config.import, but plain
         * JUnit tests have no Spring context — so we parse the file ourselves.
         */
        @JvmStatic
        @BeforeAll
        fun loadEnv() {
            val envFile = File(".env")
            if (!envFile.exists()) {
                println("⚠️  No .env file found at ${envFile.absolutePath} — skipping env load.")
                return
            }
            envFile.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isBlank() || trimmed.startsWith("#")) return@forEachLine
                val (key, value) = trimmed.split("=", limit = 2)
                // Only set if not already set by the OS environment
                if (System.getenv(key) == null) {
                    // ProcessEnvironment is not directly mutable in Java, but we can
                    // use a reflective hack as a last resort — cleaner alternative:
                    // pass via -DSMTP_KEY=... JVM args. Here we set system properties
                    // as a fallback and update EmailService to check both.
                    System.setProperty(key.trim(), value.trim())
                }
            }
        }
    }

    @Test
    fun `send password reset email via Resend`() {
        println("📧 Sending test password-reset email via Resend...")
        EmailService().sendResetEmail(
            recipientEmail = "lovepapa987@gmail.com",
            secureToken    = "TEST123"
        )
        println("✅ Done. Check your inbox at lovepapa987@gmail.com.")
    }
}
