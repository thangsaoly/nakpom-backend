package com.nakpom.exception

/**
 * Thrown when a user attempts to join a family they already belong to.
 *
 * Resolves to HTTP 409 Conflict via GlobalExceptionHandler.
 */
class MembershipAlreadyExistsException(
    message: String = "You are already a member of this family"
) : RuntimeException(message)
