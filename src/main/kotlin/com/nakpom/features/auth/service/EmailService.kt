package com.nakpom.features.auth.service

import com.resend.Resend
import com.resend.core.exception.ResendException
import com.resend.services.emails.model.CreateEmailOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Sends transactional password-reset emails using the Resend Java SDK.
 *
 * Required configuration (environment variables or .env imported by Spring):
 *   RESEND_API_KEY      — your Resend secret key (re_xxxx...)
 *   RESEND_FROM_EMAIL   — verified sender address (defaults to Resend sandbox)
 */
@Service
class EmailService(
    @Value("\${RESEND_API_KEY}")
    private val apiKey: String,

    @Value("\${RESEND_FROM_EMAIL:onboarding@resend.dev}")
    private val fromEmail: String
) {

    private val resend = Resend(apiKey)

    // --------------------------------------------------------------------------
    // Public API
    // --------------------------------------------------------------------------

    /**
     * Dispatches a password-reset email to [recipientEmail].
     *
     * The [secureToken] is embedded in a reset link that expires in 15 minutes.
     * Throws [RuntimeException] if the Resend API call fails.
     */
    fun sendResetEmail(recipientEmail: String, secureToken: String) {
        val resetLink = "https://nakpom.com/reset-password?token=$secureToken"

        val request = CreateEmailOptions.builder()
            .from(fromEmail)
            .to(recipientEmail)
            .subject("Reset Your NakPom Password")
            .html(buildResetHtml(resetLink))
            .build()

        try {
            val response = resend.emails().send(request)
            println("✅ Password-reset email dispatched (id=${response.id}) to $recipientEmail")
        } catch (e: ResendException) {
            e.printStackTrace()
            throw RuntimeException("Failed to send password-reset email to $recipientEmail.", e)
        }
    }

    // --------------------------------------------------------------------------
    // Internal helpers
    // --------------------------------------------------------------------------

    private fun buildResetHtml(resetLink: String): String = """
        <div style="font-family:sans-serif;max-width:480px;margin:auto">
          <h2 style="color:#1a1a1a">Reset Your NakPom Password</h2>
          <p>Click the button below to reset your family space password.</p>
          <a href="$resetLink"
             style="display:inline-block;padding:12px 24px;background:#6366f1;
                    color:#fff;border-radius:8px;text-decoration:none;font-weight:600">
            Reset Password
          </a>
          <p style="color:#666;font-size:13px;margin-top:24px">
            This link expires in <strong>15 minutes</strong>.<br>
            If you did not request this, you can safely ignore this email.
          </p>
          <hr style="border:none;border-top:1px solid #eee">
          <p style="color:#999;font-size:12px">The NakPom Team</p>
        </div>
    """.trimIndent()

}
