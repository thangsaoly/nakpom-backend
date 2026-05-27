package com.nakpom.features.auth.routing

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1")
class HealthController {
    
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now(),
            "service" to "nakpom-backend",
            "version" to "0.0.1-SNAPSHOT"
        ))
    }
}
