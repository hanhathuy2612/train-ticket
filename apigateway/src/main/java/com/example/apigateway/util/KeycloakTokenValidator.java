package com.example.apigateway.util;

import com.example.apigateway.config.KeycloakConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Keycloak JWT Token Validator for API Gateway
 * <p>
 * This utility validates JWT tokens issued by Keycloak by:
 * 1. Fetching public keys from Keycloak JWKS endpoint
 * 2. Verifying token signature
 * 3. Validating issuer, audience, and expiration
 * 4. Caching public keys to avoid repeated fetches
 */
@Slf4j
@Component
public class KeycloakTokenValidator {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final KeycloakConfig keycloakConfig;

    // Cache for public keys (key: kid, value: PublicKey)
    private final ConcurrentMap<String, PublicKey> publicKeyCache = new ConcurrentHashMap<>();

    public KeycloakTokenValidator(WebClient webClient, ObjectMapper objectMapper, KeycloakConfig keycloakConfig) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.keycloakConfig = keycloakConfig;
    }

    /**
     * Validates a JWT token from Keycloak
     *
     * @param token The JWT token to validate
     * @return Mono<Claims> containing the token claims if valid, or Mono.empty() if invalid
     */
    public Mono<Claims> validateToken(String token) {
        try {
            // Parse token to get header
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.debug("Invalid token format");
                return Mono.empty();
            }

            // Decode header to get key ID (kid)
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode header = objectMapper.readTree(headerJson);
            JsonNode kidNode = header.get("kid");
            if (kidNode == null) {
                log.debug("Token missing kid in header");
                return Mono.empty();
            }
            String kid = kidNode.asText();

            // Get public key (from cache or fetch from Keycloak)
            return getPublicKey(kid)
                .flatMap(publicKey -> {
                    if (publicKey == null) {
                        log.warn("Could not retrieve public key for kid: {}", kid);
                        return Mono.empty();
                    }

                    try {
                        // Verify token signature and parse claims
                        Claims claims = Jwts.parser()
                            .verifyWith(publicKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();

                        // Validate issuer
                        String issuer = claims.getIssuer();
                        String expectedIssuer = keycloakConfig.getRealmPublicKeyUrl();
                        if (!issuer.equals(expectedIssuer)) {
                            log.warn("Token issuer mismatch. Expected: {}, Got: {}", expectedIssuer, issuer);
                            return Mono.empty();
                        }

                        // Validate audience (client ID)
                        String audience = claims.getAudience().stream()
                            .findFirst()
                            .orElse("");
                        if (!audience.equals(keycloakConfig.getClientId())) {
                            log.warn("Token audience mismatch. Expected: {}, Got: {}",
                                keycloakConfig.getClientId(), audience);
                            return Mono.empty();
                        }

                        // Check expiration
                        if (claims.getExpiration() != null &&
                            claims.getExpiration().before(new java.util.Date())) {
                            log.debug("Token has expired");
                            return Mono.empty();
                        }

                        return Mono.just(claims);

                    } catch (ExpiredJwtException e) {
                        log.debug("Token expired: {}", e.getMessage());
                        return Mono.empty();
                    } catch (SecurityException e) {
                        log.debug("Token signature validation failed: {}", e.getMessage());
                        return Mono.empty();
                    } catch (Exception e) {
                        log.error("Error validating token", e);
                        return Mono.empty();
                    }
                });

        } catch (Exception e) {
            log.error("Error parsing token", e);
            return Mono.empty();
        }
    }

    /**
     * Gets public key from cache or fetches from Keycloak JWKS endpoint
     *
     * @param kid Key ID from JWT header
     * @return Mono<PublicKey> containing the public key, or Mono.empty() if not found
     */
    private Mono<PublicKey> getPublicKey(String kid) {
        // Check cache first
        PublicKey cachedKey = publicKeyCache.get(kid);
        if (cachedKey != null) {
            return Mono.just(cachedKey);
        }

        // Fetch from Keycloak
        String jwksUrl = keycloakConfig.getJwksUrl();
        log.debug("Fetching public key from Keycloak JWKS endpoint: {}", jwksUrl);

        return webClient.get()
            .uri(jwksUrl)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .flatMap(jwks -> {
                JsonNode keys = jwks.get("keys");
                if (keys == null || !keys.isArray()) {
                    log.error("Invalid JWKS response: no keys array");
                    return Mono.empty();
                }

                for (JsonNode key : keys) {
                    JsonNode kidNode = key.get("kid");
                    if (kidNode != null && kid != null && kidNode.asText().equals(kid)) {
                        try {
                            PublicKey publicKey = buildPublicKey(key);
                            // Cache the key
                            publicKeyCache.put(kid, publicKey);
                            log.debug("Successfully fetched and cached public key for kid: {}", kid);
                            return Mono.just(publicKey);
                        } catch (Exception e) {
                            log.error("Error building public key", e);
                            return Mono.empty();
                        }
                    }
                }

                log.warn("Key with kid {} not found in JWKS", kid);
                return Mono.<PublicKey>empty();
            })
            .onErrorResume(e -> {
                log.error("Error fetching public key from Keycloak", e);
                return Mono.empty();
            });
    }

    /**
     * Builds RSA public key from JWK
     *
     * @param keyNode JSON node containing the key information
     * @return PublicKey instance
     */
    private PublicKey buildPublicKey(JsonNode keyNode) {
        try {
            String modulus = keyNode.get("n").asText();
            String exponent = keyNode.get("e").asText();

            byte[] nBytes = Base64.getUrlDecoder().decode(modulus);
            byte[] eBytes = Base64.getUrlDecoder().decode(exponent);

            BigInteger n = new BigInteger(1, nBytes);
            BigInteger e = new BigInteger(1, eBytes);

            RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Error building public key", e);
        }
    }

    /**
     * Clears the public key cache (useful for testing or key rotation)
     */
    public void clearCache() {
        publicKeyCache.clear();
        log.info("Public key cache cleared");
    }
}
