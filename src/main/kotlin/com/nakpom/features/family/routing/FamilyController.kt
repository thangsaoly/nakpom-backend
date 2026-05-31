package com.nakpom.features.family.routing

import com.nakpom.features.auth.models.dto.JoinFamilyResponse
import com.nakpom.features.family.service.FamilyService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/family")
class FamilyController(private val familyService: FamilyService) {

    /**
     * GET /api/v1/family/join?code=NP-XXXXXX
     *
     * Joins the calling user to a family identified by its invite code.
     *
     * During development, the authenticated user is identified via the
     * X-User-Id request header. This will be replaced by JWT extraction
     * when authentication middleware is added in a later sprint.
     *
     * Responses:
     *   200 OK         — joined successfully, returns family details
     *   404 Not Found  — invite code does not match any family
     *   409 Conflict   — user is already a member of that family
     */
    @GetMapping("/join")
    fun joinFamily(
        @RequestParam code: String,
        @RequestHeader("X-User-Id") userId: Int
    ): ResponseEntity<JoinFamilyResponse> {
        val response = familyService.joinFamilyByCode(userId, code)
        return ResponseEntity.ok(response)
    }

    /**
     * GET /api/v1/family/{familyId}/feed
     *
     * Placeholder for the family activity feed.
     * Protected by FamilyMembershipInterceptor — only members of {familyId} can reach this.
     *
     * This endpoint will be replaced with real feed data in a future sprint.
     */
    @GetMapping("/{familyId}/feed")
    fun getFamilyFeed(
        @PathVariable familyId: Int,
        @RequestHeader("X-User-Id") userId: Int
    ): ResponseEntity<Map<String, Any>> {
        val response = mapOf(
            "familyId" to familyId,
            "message" to "Feed for family $familyId (placeholder — real content coming in Sprint 3)",
            "timestamp" to System.currentTimeMillis()
        )
        return ResponseEntity.ok(response)
    }
}
