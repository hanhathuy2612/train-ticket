package com.example.shared.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.shared.security.TokenVerificationInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Auto-configuration for token verification in services using shared module
 * <p>
 * This configuration is automatically loaded when shared module is included.
 * It automatically:
 * 1. Creates WebClient and ObjectMapper beans if not already present
 * 2. Registers TokenVerificationInterceptor with proper exclude paths
 * 3. Reads token.verification.enabled from application.yml
 * <p>
 * Services can override this by:
 * - Creating their own WebMvcConfigurer (will take precedence)
 * - Excluding this auto-configuration: @SpringBootApplication(exclude =
 * WebConfig.class)
 * - Setting token.verification.enabled=false to disable
 * <p>
 * To disable token verification (trust gateway headers):
 * - Set: token.verification.enabled=false in application.yml
 * - Or set environment variable: TOKEN_VERIFICATION_ENABLED=false
 * - Or set system property: -Dtoken.verification.enabled=false
 */
@AutoConfiguration
@ConditionalOnClass({ WebMvcConfigurer.class, TokenVerificationInterceptor.class })
@ConditionalOnProperty(name = "token.verification.enabled", havingValue = "true", matchIfMissing = true)
public class WebConfig implements WebMvcConfigurer {

    @Autowired(required = false)
    private TokenVerificationInterceptor tokenVerificationInterceptor;

    @Value("${token.verification.enabled:true}")
    private boolean verificationEnabled;

    /**
     * WebClient bean for TokenVerificationUtility
     * Only created if not already present in the application context
     */
    @Bean
    @ConditionalOnMissingBean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    /**
     * ObjectMapper bean for TokenVerificationUtility
     * Only created if not already present in the application context
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Only add interceptor if it's available
        if (tokenVerificationInterceptor != null) {
            // Set enabled from application.yml
            tokenVerificationInterceptor.setEnabled(verificationEnabled);

            registry.addInterceptor(tokenVerificationInterceptor)
                    .addPathPatterns("/**")
                    .excludePathPatterns(
                            "/actuator/**",
                            "/health",
                            // OpenAPI/Swagger endpoints
                            "/v3/api-docs",
                            "/v3/api-docs/**",
                            "/api-docs/**", // Legacy support
                            "/swagger-ui/**",
                            "/swagger-ui.html");
        }
    }
}
