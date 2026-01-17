package com.example.paymentservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.paymentservice.security.TokenVerificationInterceptor;
import com.example.paymentservice.security.TokenVerificationUtility;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Security configuration for token verification
 *
 * This enables hybrid security approach:
 * - API Gateway validates token fully
 * - Service verifies token signature (lightweight check)
 *
 * To disable token verification (trust gateway headers):
 * - Set: token.verification.enabled=false in application.yml
 * - Or set environment variable: TOKEN_VERIFICATION_ENABLED=false
 */
@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${token.verification.enabled:true}")
    private boolean verificationEnabled;

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public TokenVerificationUtility tokenVerificationUtility(WebClient webClient, ObjectMapper objectMapper) {
        return new TokenVerificationUtility(webClient, objectMapper);
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // Get beans lazily to avoid circular dependency
        try {
            TokenVerificationUtility utility = applicationContext.getBean(TokenVerificationUtility.class);
            TokenVerificationInterceptor interceptor = new TokenVerificationInterceptor(
                    utility, verificationEnabled);
            registry.addInterceptor(interceptor)
                    .addPathPatterns("/**")
                    .excludePathPatterns(
                            "/actuator/**",
                            "/health",
                            "/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html");
        } catch (Exception e) {
            // Beans not available, skip registration
        }
    }
}
