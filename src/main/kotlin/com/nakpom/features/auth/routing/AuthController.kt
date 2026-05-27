package com.nakpom.features.auth.routing

import com.nakpom.features.auth.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val authService: AuthService) {
    
    // Placeholder endpoints for future implementation
    
    @PostMapping("/register")
    fun registerUser(@RequestBody request: Map<String, String>): ResponseEntity<Map<String, Any>> {
        // TODO: Implement registration endpoint
        return ResponseEntity.status(501).body(mapOf(
            "message" to "Registration endpoint not yet implemented"
        ))
    }
    
    @PostMapping("/login")
    fun loginUser(@RequestBody request: Map<String, String>): ResponseEntity<Map<String, Any>> {
        // TODO: Implement login endpoint
        return ResponseEntity.status(501).body(mapOf(
            "message" to "Login endpoint not yet implemented"
        ))
    }
}
