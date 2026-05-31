package com.nakpom.exception

/**
 * Thrown when a requested resource (e.g. a family by invite code) cannot be found.
 *
 * Resolves to HTTP 404 Not Found via GlobalExceptionHandler.
 */
class ResourceNotFoundException(
    message: String = "The requested resource was not found"
) : RuntimeException(message)
