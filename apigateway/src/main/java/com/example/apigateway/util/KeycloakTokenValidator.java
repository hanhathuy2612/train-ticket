package com.example.apigateway.util;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.apigateway.config.KeycloakConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Validates Keycloak JWT tokens
 * 
 * This class validates tokens by:
 * 1. Fetching public keys from Keycloak's JWKS endpoint
 * 2. Verifying token signature
 * 3. Validating token claims (issuer, audience, expiration)
 */
@Component
@RequiredArgsConstructor
public class KeycloakTokenValidator {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakTokenValidator.class);

    private final KeycloakConfig keycloakConfig;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.realm}")
    private String realm;

    /**
     * Validates a Keycloak JWT token
     * 
     * @param token The JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            // Parse token without verification first to get header
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                logger.debug("Invalid token format");
                return false;
            }

            // Decode header to get key ID (kid)
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode header = objectMapper.readTree(headerJson);
            String kid = header.get("kid").asText();

            // Get public key from Keycloak
            PublicKey publicKey = getPublicKey(kid).block();
            if (publicKey == null) {
                logger.warn("Could not retrieve public key from Keycloak");
                return false;
            }

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
                logger.warn("Token issuer mismatch. Expected: {}, Got: {}", expectedIssuer, issuer);
                return false;
            }

            // Validate audience (client ID)
            String audience = claims.getAudience().stream().findFirst().orElse("");
            if (!audience.equals(keycloakConfig.getClientId())) {
                logger.warn("Token audience mismatch. Expected: {}, Got: {}", keycloakConfig.getClientId(), audience);
                return false;
            }

            // Check expiration
            if (claims.getExpiration().before(new java.util.Date())) {
                logger.debug("Token has expired");
                return false;
            }

            return true;

        } catch (ExpiredJwtException e) {
            logger.debug("Token expired: {}", e.getMessage());
            return false;
        } catch (SecurityException e) {
            logger.debug("Token signature validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Error validating Keycloak token", e);
            return false;
        }
    }

    /**
     * Extracts username (preferred_username) from token
     */
    public String extractUsername(String token) {
        try {
            Map<String, Object> claims = parseTokenPayload(token);
            return (String) claims.get("preferred_username");
        } catch (Exception e) {
            logger.error("Error extracting username from token", e);
            return null;
        }
    }

    /**
     * Extracts user ID (sub) from token
     */
    public String extractUserId(String token) {
        try {
            Map<String, Object> claims = parseTokenPayload(token);
            return (String) claims.get("sub"); // sub claim
        } catch (Exception e) {
            logger.error("Error extracting user ID from token", e);
            return null;
        }
    }

    /**
     * Extracts roles from token
     * Keycloak roles are in realm_access.roles and resource_access.{clientId}.roles
     */
    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(String token) {
        Set<String> roles = new HashSet<>();
        try {
            Map<String, Object> claims = parseTokenPayload(token);

            // Extract realm roles
            Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
            if (realmAccess != null) {
                List<String> realmRoles = (List<String>) realmAccess.get("roles");
                if (realmRoles != null) {
                    roles.addAll(realmRoles);
                }
            }

            // Extract client-specific roles
            Map<String, Object> resourceAccess = (Map<String, Object>) claims.get("resource_access");
            if (resourceAccess != null) {
                Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess
                        .get(keycloakConfig.getClientId());
                if (clientAccess != null) {
                    List<String> clientRoles = (List<String>) clientAccess.get("roles");
                    if (clientRoles != null) {
                        roles.addAll(clientRoles);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error extracting roles from token", e);
        }
        return roles;
    }

    /**
     * Parses token payload as Map (without verification, for extracting claims
     * only)
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseTokenPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid token format");
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing token payload", e);
        }
    }

    /**
     * Gets public key from Keycloak JWKS endpoint
     */
    private Mono<PublicKey> getPublicKey(String kid) {
        return webClient.get()
                .uri(keycloakConfig.getJwksUrl())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(jwks -> {
                    JsonNode keys = jwks.get("keys");
                    if (keys == null || !keys.isArray()) {
                        throw new RuntimeException("Invalid JWKS response: no keys array");
                    }
                    for (JsonNode key : keys) {
                        JsonNode kidNode = key.get("kid");
                        if (kidNode != null && kid != null && kidNode.asText().equals(kid)) {
                            return buildPublicKey(key);
                        }
                    }
                    throw new RuntimeException("Key with kid " + kid + " not found");
                })
                .doOnError(e -> logger.error("Error fetching public key from Keycloak", e));
    }

    /**
     * Builds RSA public key from JWK
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

}
