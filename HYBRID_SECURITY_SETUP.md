# Hybrid Security Setup Guide

## Tổng quan

Hybrid Security Approach kết hợp:
- **API Gateway**: Validate token đầy đủ với Keycloak
- **Services**: Verify token signature (lightweight check) để đảm bảo security

## Kiến trúc

```
Client → API Gateway (Full Validation) → Services (Signature Verification)
         ↓                                    ↓
    Validate với Keycloak              Verify signature only
    Extract user info                  Trust headers if verified
    Forward token + headers
```

## Implementation

### 1. API Gateway (Đã hoàn thành)

Gateway đã được cấu hình để:
- Validate token với Keycloak
- Forward token trong header `X-Auth-Token`
- Forward user context: `X-User-Id`, `X-User-Name`, `X-User-Roles`

### 2. Services (Example: Ticket Service)

#### Dependencies đã thêm:
```gradle
// JWT for token verification
implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
implementation 'io.jsonwebtoken:jjwt-impl:0.12.3'
implementation 'io.jsonwebtoken:jjwt-jackson:0.12.3'

// WebClient for fetching Keycloak public keys
implementation 'org.springframework.boot:spring-boot-starter-webflux'
```

#### Files đã tạo:
1. **`TokenVerificationUtility.java`**: Verify token signature với Keycloak public keys
2. **`TokenVerificationInterceptor.java`**: Interceptor để verify token cho mỗi request
3. **`SecurityConfig.java`**: Configuration để enable interceptor

#### Configuration trong `application.yml`:
```yaml
keycloak:
  server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8080}
  realm: ${KEYCLOAK_REALM:train-ticket}

token:
  verification:
    enabled: ${TOKEN_VERIFICATION_ENABLED:true}  # Set false to trust gateway
```

## Cách sử dụng cho các services khác

### Option 1: Copy implementation từ ticket-service

1. Copy các files từ `ticket-service/src/main/java/com/example/ticketservice/security/`:
   - `TokenVerificationUtility.java`
   - `TokenVerificationInterceptor.java`
   - `SecurityConfig.java`

2. Update package name cho service của bạn

3. Thêm dependencies vào `build.gradle`:
```gradle
implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
implementation 'io.jsonwebtoken:jjwt-impl:0.12.3'
implementation 'io.jsonwebtoken:jjwt-jackson:0.12.3'
implementation 'org.springframework.boot:spring-boot-starter-webflux'
```

4. Thêm config vào `application.yml`:
```yaml
keycloak:
  server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8080}
  realm: ${KEYCLOAK_REALM:train-ticket}

token:
  verification:
    enabled: ${TOKEN_VERIFICATION_ENABLED:true}
```

### Option 2: Sử dụng shared module (nếu được setup)

Nếu shared module được publish như một library:
```gradle
implementation project(':shared')
```

## Configuration Options

### Enable/Disable Token Verification

**Enable (default)**: Services verify token signature
```yaml
token:
  verification:
    enabled: true
```

**Disable**: Trust headers from gateway (for internal network)
```yaml
token:
  verification:
    enabled: false
```

Hoặc dùng environment variable:
```bash
TOKEN_VERIFICATION_ENABLED=false
```

## Security Levels

### Level 1: Trust Gateway (Fastest)
- Set `token.verification.enabled=false`
- Services trust `X-User-Id` header
- **Use case**: Internal network, mTLS enabled

### Level 2: Lightweight Verification (Recommended)
- Set `token.verification.enabled=true`
- Services verify token signature only
- **Use case**: Production with good network security

### Level 3: Full Verification
- Services validate token fully (issuer, audience, expiration)
- **Use case**: External-facing services or high security requirements

## Testing

### Test với token verification enabled:
```bash
# Get token from Keycloak
TOKEN=$(curl -X POST http://localhost:8080/realms/train-ticket/protocol/openid-connect/token \
  -d "client_id=train-ticket-gateway" \
  -d "username=user" \
  -d "password=password" \
  -d "grant_type=password" | jq -r '.access_token')

# Call service through gateway
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/tickets/user/1
```

### Test với fake header (should fail if verification enabled):
```bash
# This should fail if token verification is enabled
curl -H "X-User-Id: 999" http://localhost:8083/tickets/user/1
```

## Performance Considerations

- **Public key caching**: Keys are cached to avoid repeated fetches
- **Lightweight check**: Only signature verification, no full validation
- **Optional**: Can be disabled for internal services

## Best Practices

1. **Enable verification** cho production services
2. **Disable verification** cho internal services trong trusted network
3. **Use mTLS** giữa gateway và services để đảm bảo requests đến từ gateway
4. **Monitor** token verification failures để detect security issues

## Troubleshooting

### Token verification fails:
- Check Keycloak server URL và realm
- Verify token is valid (check expiration)
- Check network connectivity to Keycloak

### Performance issues:
- Public keys are cached, first request might be slower
- Consider disabling verification for internal services

### Services can't verify tokens:
- Ensure Keycloak is running and accessible
- Check `keycloak.server-url` và `keycloak.realm` in application.yml
