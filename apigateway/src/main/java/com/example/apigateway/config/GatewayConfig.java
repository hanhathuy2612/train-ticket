package com.example.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    /**
     * WebClient for making HTTP requests to Keycloak
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    /**
     * ObjectMapper for JSON processing
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Rate limiter key resolver based on user ID from JWT token
     * Falls back to IP address if user ID is not available
     * Marked as @Primary to resolve ambiguity when multiple KeyResolver beans exist
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst(Constants.USER_ID_HEADER);
            if (userId != null && !userId.isEmpty()) {
                return Mono.just("user:" + userId);
            }
            // Fallback to IP address for unauthenticated requests
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : Constants.UNKNOWN;
            return Mono.just("ip:" + ip);
        };
    }

    /**
     * Rate limiter key resolver based on IP address
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : Constants.UNKNOWN;
            return Mono.just(ip);
        };
    }
}
