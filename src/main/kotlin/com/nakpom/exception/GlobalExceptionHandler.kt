package com.nakpom.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * Handles Bean Validation errors from @Valid DTOs.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        val response = mapOf(
            "status" to HttpStatus.BAD_REQUEST.value(),
            "error" to "Validation Failed",
            "details" to errors,
            "timestamp" to System.currentTimeMillis()
        )
        return ResponseEntity.badRequest().body(response)
    }

    /**
     * Handles duplicate email registration attempts.
     */
    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailAlreadyExists(ex: EmailAlreadyExistsException): ResponseEntity<Map<String, Any>> {
        val response = mapOf(
            "status" to HttpStatus.CONFLICT.value(),
            "error" to "Email Already Exists",
            "message" to (ex.message ?: "Email is already registered"),
            "timestamp" to System.currentTimeMillis()
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
    }

    /**
     * Handles invalid login credentials.
     */
    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ResponseEntity<Map<String, Any>> {
        val response = mapOf(
            "status" to HttpStatus.UNAUTHORIZED.value(),
            "error" to "Authentication Failed",
            "message" to (ex.message ?: "Invalid email or password"),
            "timestamp" to System.currentTimeMillis()
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response)
    }

    /**
     * Handles invalid or expired password-reset tokens.
     */
    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidToken(ex: InvalidTokenException): ResponseEntity<Map<String, Any>> {
        val response = mapOf(
            "status" to HttpStatus.BAD_REQUEST.value(),
            "error" to "Invalid Token",
            "message" to (ex.message ?: "Password reset token is invalid or has expired"),
            "timestamp" to System.currentTimeMillis()
        )
        return ResponseEntity.badRequest().body(response)
    }

    /**
     * Handles resource lookups that return no result (e.g. unknown invite code).
     */
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException): ResponseEntity<Map<String, Any>> {
        val response = mapOf(
            "status" to HttpStatus.NOT_FOUND.value(),
            "error" to "Not Found",
            "message" to (ex.message ?: "The requested resource was not found"),
            "timestamp" to System.currentTimeMillis()
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    /**
     * Handles attempts to join a family the user already belongs to.
     */
    @ExceptionHandler(MembershipAlreadyExistsException::class)
    fun handleMembershipAlreadyExists(ex: MembershipAlreadyExistsException): ResponseEntity<Map<String, Any>> {
        val response = mapOf(
            "status" to HttpStatus.CONFLICT.value(),
            "error" to "Membership Already Exists",
            "message" to (ex.message ?: "You are already a member of this family"),
            "timestamp" to System.currentTimeMillis()
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
    }

    /**
     * Catch-all handler for unexpected exceptions.
     */
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<Map<String, Any>> {
        val response = mapOf(
            "status" to HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "error" to "Internal Server Error",
            "message" to (ex.message ?: "An unexpected error occurred"),
            "timestamp" to System.currentTimeMillis()
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}
