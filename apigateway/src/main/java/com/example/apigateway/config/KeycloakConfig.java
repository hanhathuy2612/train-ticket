package com.example.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Keycloak configuration properties
 * Maps to keycloak.* properties in application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Data
public class KeycloakConfig {

    /**
     * Keycloak server URL (e.g., http://localhost:8080)
     */
    private String serverUrl;

    /**
     * Realm name in Keycloak
     */
    private String realm;

    /**
     * Client ID configured in Keycloak
     */
    private String clientId;

    /**
     * Get the realm public key URL
     */
    public String getRealmPublicKeyUrl() {
        return String.format("%s/realms/%s", serverUrl, realm);
    }

    /**
     * Get the JWKS (JSON Web Key Set) URL
     */
    public String getJwksUrl() {
        return String.format("%s/realms/%s/protocol/openid-connect/certs", serverUrl, realm);
    }
}
