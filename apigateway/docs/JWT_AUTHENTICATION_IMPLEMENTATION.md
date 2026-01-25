# JWT Authentication Implementation

## 📋 Tổng quan

Đã implement `JwtAuthenticationFilter` và `KeycloakTokenValidator` để thêm authentication layer vào API Gateway.

## ✅ Đã Implement

### 1. KeycloakTokenValidator
**File:** `apigateway/src/main/java/com/example/apigateway/util/KeycloakTokenValidator.java`

**Chức năng:**
- Fetch public keys từ Keycloak JWKS endpoint
- Verify JWT token signature với public key
- Validate issuer (realm URL)
- Validate audience (client ID)
- Check token expiration
- Cache public keys để tránh fetch lặp lại

**Sử dụng:**
```java
@Autowired
private KeycloakTokenValidator tokenValidator;

Mono<Claims> claims = tokenValidator.validateToken(token);
```

### 2. JwtAuthenticationFilter
**File:** `apigateway/src/main/java/com/example/apigateway/filter/JwtAuthenticationFilter.java`

**Chức năng:**
- Extract JWT token từ `Authorization: Bearer <token>` header
- Validate token với `KeycloakTokenValidator`
- Extract user info từ JWT claims:
  - `userId` từ `sub` claim
  - `username` từ `preferred_username` claim
  - `roles` từ `realm_access.roles` và `resource_access.*.roles`
- Add headers vào request:
  - `X-User-Id`: User ID
  - `X-User-Name`: Username
  - `X-User-Roles`: Comma-separated roles
  - `X-Auth-Token`: JWT token (forwarded to microservice)
- Block unauthorized requests (401)
- Skip authentication cho public endpoints

**Filter Order:** -100
- Sau LoggingFilter (-200)
- Sau RateLimitFilter (-150)
- Trước routing

### 3. Constants Updates
**File:** `apigateway/src/main/java/com/example/apigateway/config/Constants.java`

**Thêm:**
- `USER_NAME_HEADER = "X-User-Name"`
- `USER_ROLES_HEADER = "X-User-Roles"`

## 🔄 Flow Security Mới

```
Request → Gateway
  │
  ├─ ✅ Spring Security WebFlux (CORS)
  ├─ ✅ GlobalErrorFilter (Order: -1)
  ├─ ✅ LoggingFilter (Order: -200)
  ├─ ✅ RateLimitFilter (Order: -150)
  ├─ ✅ JwtAuthenticationFilter (Order: -100) ← MỚI
  │     ├─ Extract token từ Authorization header
  │     ├─ Validate với KeycloakTokenValidator
  │     ├─ Extract user info (userId, username, roles)
  │     ├─ Add headers: X-User-Id, X-User-Name, X-User-Roles, X-Auth-Token
  │     └─ Block nếu invalid (401)
  │
  └─ → Route to microservice (CÓ user info headers)
```

## 📊 Request Headers Flow

### Request đến Gateway:
```
Authorization: Bearer <JWT_TOKEN>
Origin: http://localhost:4200
```

### Request từ Gateway đến Microservice:
```
X-User-Id: <userId>                    ✅ Từ JWT claims
X-User-Name: <username>                 ✅ Từ JWT claims
X-User-Roles: <roles>                   ✅ Từ JWT claims
X-Auth-Token: <JWT_TOKEN>                ✅ Forward token
X-Correlation-Id: <uuid>                 ✅ Từ LoggingFilter
```

## 🔐 Public Endpoints

Các endpoints sau **KHÔNG cần authentication**:

- `/api/users/register`
- `/api/users/login`
- `/api/users/forgot-password`
- `/api/users/reset-password`
- `/api/users/refresh-token`
- `/api/inventory/health`
- `/api/tickets/health`
- `/api/payments/health`
- `/api/notifications/health`
- `/api/inventory/schedules/availability`
- `/actuator/**`
- `/swagger-ui.html`
- `/v3/api-docs`
- `/fallback/**`

Tất cả các endpoints khác **YÊU CẦU authentication**.

## ⚙️ Configuration

### Keycloak Configuration (application.yml)
```yaml
keycloak:
  server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8080}
  realm: ${KEYCLOAK_REALM:train-ticket}
  client-id: ${KEYCLOAK_CLIENT_ID:train-ticket-gateway}
```

### Dependencies (build.gradle)
Đã có sẵn:
- `spring-boot-starter-oauth2-resource-server` (không dùng nhưng có thể cần)
- `io.jsonwebtoken:jjwt-*` (dùng để parse JWT)
- `WebClient` (dùng để fetch JWKS từ Keycloak)
- `ObjectMapper` (dùng để parse JSON)

## 🧪 Testing

### Test với valid token:
```bash
curl -X GET http://localhost:8189/api/tickets/bookings \
  -H "Authorization: Bearer <VALID_JWT_TOKEN>"
```

**Expected:**
- Status: 200 OK
- Headers forwarded to microservice:
  - `X-User-Id`: <userId>
  - `X-User-Name`: <username>
  - `X-User-Roles`: <roles>
  - `X-Auth-Token`: <token>

### Test với invalid token:
```bash
curl -X GET http://localhost:8189/api/tickets/bookings \
  -H "Authorization: Bearer invalid_token"
```

**Expected:**
- Status: 401 Unauthorized
- Response:
```json
{
  "success": false,
  "message": "Token is invalid or expired",
  "statusCode": 401,
  "errorCode": "UNAUTHORIZED"
}
```

### Test với missing token:
```bash
curl -X GET http://localhost:8189/api/tickets/bookings
```

**Expected:**
- Status: 401 Unauthorized
- Response:
```json
{
  "success": false,
  "message": "Missing or invalid authorization token",
  "statusCode": 401,
  "errorCode": "UNAUTHORIZED"
}
```

### Test public endpoint:
```bash
curl -X POST http://localhost:8189/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"pass"}'
```

**Expected:**
- Status: 200 OK (hoặc response từ user-service)
- Không có authentication check

## 🔍 JWT Token Structure

Keycloak JWT token có structure:
```json
{
  "header": {
    "kid": "key-id",
    "alg": "RS256"
  },
  "payload": {
    "sub": "f:uuid:username",        // User ID
    "preferred_username": "username",  // Username
    "email": "user@example.com",      // Email
    "iss": "http://localhost:8080/realms/train-ticket",  // Issuer
    "aud": "train-ticket-gateway",   // Audience (client ID)
    "exp": 1234567890,                // Expiration
    "realm_access": {
      "roles": ["user", "admin"]      // Realm roles
    },
    "resource_access": {
      "train-ticket-gateway": {
        "roles": ["client-role"]      // Client roles
      }
    }
  }
}
```

## 🐛 Troubleshooting

### 1. Token validation fails
**Nguyên nhân:**
- Keycloak server không accessible
- JWKS endpoint không đúng
- Token issuer/audience không match

**Giải pháp:**
- Kiểm tra Keycloak server URL trong `application.yml`
- Kiểm tra realm và client-id
- Verify token được issue từ đúng Keycloak realm

### 2. Public key cache issues
**Nguyên nhân:**
- Keycloak rotate keys
- Cache không được clear

**Giải pháp:**
- Call `tokenValidator.clearCache()` để clear cache
- Restart gateway để clear cache

### 3. User info không được extract
**Nguyên nhân:**
- JWT claims structure khác với expected
- Claims không có `preferred_username` hoặc `sub`

**Giải pháp:**
- Check JWT token payload structure
- Update `extractUserId()`, `extractUsername()`, `extractRoles()` methods nếu cần

## 📝 Notes

1. **Token Caching:** Public keys được cache trong memory để tránh fetch lặp lại. Nếu Keycloak rotate keys, cần restart gateway hoặc clear cache.

2. **Error Handling:** Tất cả errors đều return 401 với CORS headers để browser có thể đọc error message.

3. **Performance:** Token validation là async (Mono) để không block thread pool.

4. **Security:** Token được forward qua `X-Auth-Token` header để microservice có thể verify lại nếu cần.

## ✅ Checklist

- [x] KeycloakTokenValidator implemented
- [x] JwtAuthenticationFilter implemented
- [x] Constants updated
- [x] Public endpoints configured
- [x] Error handling with CORS headers
- [x] User info extraction (userId, username, roles)
- [x] Token forwarding to microservice
- [ ] Unit tests
- [ ] Integration tests
- [ ] Documentation updated

## 🚀 Next Steps

1. **Testing:** Test với real Keycloak tokens
2. **Monitoring:** Add metrics cho authentication success/failure
3. **Logging:** Improve logging cho debugging
4. **Authorization:** Implement role-based authorization nếu cần
5. **Token Refresh:** Handle token refresh nếu cần
