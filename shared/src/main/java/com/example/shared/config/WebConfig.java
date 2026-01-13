package com.example.shared.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.shared.security.TokenVerificationInterceptor;

/**
 * Web configuration for services to enable token verification interceptor
 * 
 * This is an example configuration. Services can:
 * 1. Extend this class and add their own interceptors
 * 2. Or create their own WebMvcConfigurer
 * 3. Or disable token verification if in trusted network
 * 
 * To disable token verification (trust gateway headers):
 * - Set system property: -Dtoken.verification.enabled=false
 * - Or set environment variable: TOKEN_VERIFICATION_ENABLED=false
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired(required = false)
    private TokenVerificationInterceptor tokenVerificationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Only add interceptor if it's available and enabled
        if (tokenVerificationInterceptor != null) {
            registry.addInterceptor(tokenVerificationInterceptor)
                    .addPathPatterns("/**")
                    .excludePathPatterns(
                            "/actuator/**",
                            "/health",
                            "/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html");
        }
    }
}
