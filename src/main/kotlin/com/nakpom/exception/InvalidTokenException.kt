package com.nakpom.exception

/**
 * Thrown when a password-reset token is not found in the database
 * or has already expired.
 *
 * Resolves to HTTP 400 Bad Request via GlobalExceptionHandler.
 */
class InvalidTokenException(
    message: String = "Password reset token is invalid or has expired"
) : RuntimeException(message)
