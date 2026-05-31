package com.nakpom.config

import com.nakpom.features.family.interceptor.FamilyMembershipInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// Registers Spring MVC interceptors.
// FamilyMembershipInterceptor is scoped to /api/v1/family/** so it only
// runs on family-related routes. Routes without a {familyId} path variable
// (e.g. /family/join) are passed through untouched by the interceptor.
@Configuration
class WebConfig(
    private val familyMembershipInterceptor: FamilyMembershipInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(familyMembershipInterceptor)
            .addPathPatterns("/api/v1/family/**")
    }
}
