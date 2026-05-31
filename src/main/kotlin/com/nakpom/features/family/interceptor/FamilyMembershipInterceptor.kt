package com.nakpom.features.family.interceptor

import com.nakpom.features.auth.repository.FamilyMembershipRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping

// Security interceptor that enforces family membership on protected routes.
// Any request to /api/v1/family/{familyId}/** must carry an X-User-Id header
// identifying a user who is an active member of that family.
// Routes without a {familyId} path variable (e.g. /family/join) pass through freely.
@Component
class FamilyMembershipInterceptor(
    private val familyMembershipRepository: FamilyMembershipRepository
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(FamilyMembershipInterceptor::class.java)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        // Retrieve the URI template variables resolved by Spring MVC
        @Suppress("UNCHECKED_CAST")
        val pathVariables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)
                as? Map<String, String> ?: emptyMap()

        // Only enforce the check when a {familyId} path variable is present
        val familyIdStr = pathVariables["familyId"] ?: return true

        val familyId = familyIdStr.toIntOrNull()
        if (familyId == null) {
            sendForbidden(response, "Invalid family identifier")
            return false
        }

        // X-User-Id is our dev stand-in for the authenticated principal
        val userIdStr = request.getHeader("X-User-Id")
        val userId = userIdStr?.toIntOrNull()
        if (userId == null) {
            sendForbidden(response, "Missing or invalid X-User-Id header")
            return false
        }

        // Core membership check
        val isMember = familyMembershipRepository.existsByUserIdAndFamilyId(userId, familyId)
        if (!isMember) {
            logger.warn(
                "Access denied: userId={} attempted to access familyId={} without membership",
                userId, familyId
            )
            sendForbidden(response, "You are not a member of this family")
            return false
        }

        return true
    }

    // Writes a plain 403 JSON response and stops the request pipeline.
    private fun sendForbidden(response: HttpServletResponse, message: String) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        response.writer.write(
            """{"status":403,"error":"Forbidden","message":"$message","timestamp":${System.currentTimeMillis()}}"""
        )
    }
}
