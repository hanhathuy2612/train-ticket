package com.example.ticketservice.security;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SecurityException;
import reactor.core.publisher.Mono;

/**
 * Utility class for lightweight token signature verification
 * 
 * This is used by services to verify that tokens forwarded from API Gateway
 * are valid. It caches public keys from Keycloak to avoid repeated fetches.
 * 
 * Note: This is a lightweight verification - it only checks signature and expiration.
 * Full validation (issuer, audience) should be done at the gateway level.
 */
@Component
public class TokenVerificationUtility {

    private static final Logger logger = LoggerFactory.getLogger(TokenVerificationUtility.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // Cache for public keys (key: kid, value: PublicKey)
    private final ConcurrentMap<String, PublicKey> publicKeyCache = new ConcurrentHashMap<>();

    @Value("${keycloak.server-url:http://localhost:8080}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm:train-ticket}")
    private String realm;

    public TokenVerificationUtility(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Lightweight token verification - only checks signature and expiration
     * 
     * @param token The JWT token to verify
     * @return true if token signature is valid and not expired, false otherwise
     */
    public boolean verifyTokenSignature(String token) {
        try {
            // Parse token to get header
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                logger.debug("Invalid token format");
                return false;
            }

            // Decode header to get key ID (kid)
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode header = objectMapper.readTree(headerJson);
            JsonNode kidNode = header.get("kid");
            if (kidNode == null) {
                logger.debug("Token missing kid in header");
                return false;
            }
            String kid = kidNode.asText();

            // Get public key (from cache or fetch from Keycloak)
            PublicKey publicKey = getPublicKey(kid);
            if (publicKey == null) {
                logger.warn("Could not retrieve public key for kid: {}", kid);
                return false;
            }

            // Verify token signature
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Check expiration
            if (claims.getExpiration() != null && claims.getExpiration().before(new java.util.Date())) {
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
            logger.error("Error verifying token signature", e);
            return false;
        }
    }

    /**
     * Gets public key from cache or fetches from Keycloak JWKS endpoint
     */
    private PublicKey getPublicKey(String kid) {
        // Check cache first
        PublicKey cachedKey = publicKeyCache.get(kid);
        if (cachedKey != null) {
            return cachedKey;
        }

        // Fetch from Keycloak
        try {
            String jwksUrl = String.format("%s/realms/%s/protocol/openid-connect/certs",
                    keycloakServerUrl, realm);

            PublicKey publicKey = webClient.get()
                    .uri(jwksUrl)
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
                    .block();

            if (publicKey != null) {
                // Cache the key
                publicKeyCache.put(kid, publicKey);
            }

            return publicKey;

        } catch (Exception e) {
            logger.error("Error fetching public key from Keycloak", e);
            return null;
        }
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

    /**
     * Clears the public key cache (useful for testing or key rotation)
     */
    public void clearCache() {
        publicKeyCache.clear();
        logger.info("Public key cache cleared");
    }
}
