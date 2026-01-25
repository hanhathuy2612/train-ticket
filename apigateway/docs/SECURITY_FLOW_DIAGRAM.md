# Security Flow Diagram - API Gateway

## 🔐 Flow Security Hiện Tại (THIẾU Authentication)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CLIENT (Browser/Mobile)                              │
│                                                                              │
│  Request: GET /api/tickets/bookings                                         │
│  Headers:                                                                    │
│    - Authorization: Bearer <JWT_TOKEN>  ⚠️ Gateway KHÔNG validate            │
│    - Origin: http://localhost:4200                                          │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │
                                │ HTTP Request
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    API GATEWAY (Port 8189)                                  │
│                    Spring Cloud Gateway (WebFlux)                           │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  FILTER CHAIN - EXECUTION ORDER                                       │  │
│  │                                                                        │  │
│  │  ┌──────────────────────────────────────────────────────────────┐   │  │
│  │  │ 1. Spring Security WebFlux Filter Chain                      │   │  │
│  │  │    Order: Highest (runs first)                                │   │  │
│  │  │                                                                │   │  │
│  │  │    ✅ CORS Filter                                             │   │  │
│  │  │       - Check Origin header                                   │   │  │
│  │  │       - Add CORS response headers                              │   │  │
│  │  │       - Handle OPTIONS preflight requests                      │   │  │
│  │  │                                                                │   │  │
│  │  │    ❌ CSRF Filter: DISABLED                                   │   │  │
│  │  │    ❌ HTTP Basic Auth: DISABLED                                │   │  │
│  │  │    ❌ Form Login: DISABLED                                     │   │  │
│  │  │    ❌ OAuth2 Resource Server: NOT ENABLED                     │   │  │
│  │  │                                                                │   │  │
│  │  │    ⚠️ Authorization: permitAll()                               │   │  │
│  │  │       - TẤT CẢ requests đều được cho phép                     │   │  │
│  │  │       - KHÔNG kiểm tra authentication                         │   │  │
│  │  │       - KHÔNG validate JWT token                              │   │  │
│  │  └──────────────────────────────────────────────────────────────┘   │  │
│  │                            │                                           │  │
│  │                            ▼                                           │  │
│  │  ┌──────────────────────────────────────────────────────────────┐   │  │
│  │  │ 2. GlobalErrorFilter                                          │   │  │
│  │  │    Order: -1                                                  │   │  │
│  │  │                                                                │   │  │
│  │  │    - Catch exceptions từ filters                               │   │  │
│  │  │    - Format error response                                    │   │  │
│  │  │    - Add CORS headers to error response                       │   │  │
│  │  └──────────────────────────────────────────────────────────────┘   │  │
│  │                            │                                           │  │
│  │                            ▼                                           │  │
│  │  ┌──────────────────────────────────────────────────────────────┐   │  │
│  │  │ 3. LoggingFilter                                             │   │  │
│  │  │    Order: -200                                                │   │  │
│  │  │                                                                │   │  │
│  │  │    ✅ Generate Correlation ID                                 │   │  │
│  │  │    ✅ Log incoming request                                     │   │  │
│  │  │    ✅ Log outgoing response                                    │   │  │
│  │  │    ✅ Add X-Correlation-Id header                             │   │  │
│  │  │                                                                │   │  │
│  │  │    Request Headers After:                                      │   │  │
│  │  │      - X-Correlation-Id: <uuid>                                │   │  │
│  │  │      - Authorization: Bearer <JWT_TOKEN>  ⚠️ Chưa validate      │   │  │
│  │  └──────────────────────────────────────────────────────────────┘   │  │
│  │                            │                                           │  │
│  │                            ▼                                           │  │
│  │  ┌──────────────────────────────────────────────────────────────┐   │  │
│  │  │ 4. RateLimitFilter                                           │   │  │
│  │  │    Order: -150                                                │   │  │
│  │  │                                                                │   │  │
│  │  │    ✅ Check rate limit                                        │   │  │
│  │  │       - Key: "user:<userId>" (nếu có X-User-Id header)        │   │  │
│  │  │       - Key: "ip:<ip>" (fallback nếu không có userId)        │   │  │
│  │  │       - ⚠️ KHÔNG có X-User-Id vì chưa có JWT filter          │   │  │
│  │  │       - → Dùng IP address làm key                             │   │  │
│  │  │                                                                │   │  │
│  │  │    ✅ Add rate limit headers:                                 │   │  │
│  │  │       - X-RateLimit-Limit: 100                                │   │  │
│  │  │       - X-RateLimit-Remaining: 99                             │   │  │
│  │  │       - X-RateLimit-Reset: 60                                  │   │  │
│  │  └──────────────────────────────────────────────────────────────┘   │  │
│  │                            │                                           │  │
│  │                            ▼                                           │  │
│  │  ┌──────────────────────────────────────────────────────────────┐   │  │
│  │  │ 5. ❌ JwtAuthenticationFilter - KHÔNG TỒN TẠI              │   │  │
│  │  │    Order: -100 (should be here)                              │   │  │
│  │  │                                                                │   │  │
│  │  │    ❌ KHÔNG extract JWT token                                 │   │  │
│  │  │    ❌ KHÔNG validate token với Keycloak                       │   │  │
│  │  │    ❌ KHÔNG extract user info (userId, username, roles)      │   │  │
│  │  │    ❌ KHÔNG add headers: X-User-Id, X-User-Name, X-User-Roles│   │  │
│  │  │    ❌ KHÔNG forward token qua X-Auth-Token header            │   │  │
│  │  │    ❌ KHÔNG block unauthorized requests                       │   │  │
│  │  │                                                                │   │  │
│  │  │    ⚠️ Tất cả requests đều pass through                       │   │  │
│  │  └──────────────────────────────────────────────────────────────┘   │  │
│  │                            │                                           │  │
│  │                            ▼                                           │  │
│  │  ┌──────────────────────────────────────────────────────────────┐   │  │
│  │  │ 6. Route Matching & Circuit Breaker                         │   │  │
│  │  │                                                                │   │  │
│  │  │    ✅ Match route: /api/tickets/** → ticket-service           │   │  │
│  │  │    ✅ Apply Circuit Breaker (Resilience4j)                   │   │  │
│  │  │    ✅ Strip prefix: /api/tickets → /tickets                   │   │  │
│  │  │                                                                │   │  │
│  │  │    ⚠️ Forward request WITHOUT:                               │   │  │
│  │  │       - X-User-Id header                                      │   │  │
│  │  │       - X-User-Name header                                    │   │  │
│  │  │       - X-User-Roles header                                   │   │  │
│  │  │       - X-Auth-Token header                                   │   │  │
│  │  │                                                                │   │  │
│  │  │    ✅ Forward headers:                                        │   │  │
│  │  │       - X-Correlation-Id                                      │   │  │
│  │  │       - Authorization: Bearer <JWT_TOKEN>  (original)          │   │  │
│  │  └──────────────────────────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │
                                │ HTTP Request to Microservice
                                │ (via Eureka Service Discovery)
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    TICKET SERVICE (Microservice)                            │
│                    Spring Boot (Spring MVC)                                  │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ TokenVerificationInterceptor                                         │  │
│  │    (Spring MVC Interceptor)                                          │  │
│  │                                                                        │  │
│  │    ⚠️ Check header: X-Auth-Token                                     │  │
│  │       - ❌ KHÔNG có header này vì gateway không forward              │  │
│  │       - → Token verification SKIPPED                                  │  │
│  │                                                                        │  │
│  │    ⚠️ Check header: Authorization: Bearer <token>                     │  │
│  │       - ✅ Có header này (original từ client)                       │  │
│  │       - → Verify token signature (nếu enabled)                        │  │
│  │                                                                        │  │
│  │    ⚠️ KHÔNG có user info từ gateway headers                          │  │
│  │       - ❌ X-User-Id: null                                           │  │
│  │       - ❌ X-User-Name: null                                          │  │
│  │       - ❌ X-User-Roles: null                                         │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                            │                                                 │
│                            ▼                                                 │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ Controller                                                            │  │
│  │    - Xử lý business logic                                            │  │
│  │    - ⚠️ Phải tự extract user info từ JWT token                       │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## ✅ Flow Security Đề Xuất (CÓ Authentication)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CLIENT (Browser/Mobile)                              │
│                                                                              │
│  Request: GET /api/tickets/bookings                                         │
│  Headers:                                                                    │
│    - Authorization: Bearer <JWT_TOKEN>  ✅ Gateway SẼ validate              │
│    - Origin: http://localhost:4200                                          │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │
                                │ HTTP Request
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    API GATEWAY (Port 8189)                                  │
│                    Spring Cloud Gateway (WebFlux)                           │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │  FILTER CHAIN - EXECUTION ORDER                                       │  │
│  │                                                                        │  │
│  │  ┌──────────────────────────────────────────────────────────────┐   │  │
│  │  │ 1. Spring Security WebFlux Filter Chain                      │   │  │
│  │  │    Order: Highest (runs first)                                │   │  │
│  │  │                                                                │   │  │
│  │  │    ✅ CORS Filter                                             │   │  │
│  │  │       - Check Origin header                                   │   │  │
│  │  │       - Add CORS response headers                              │   │  │
│  │  │       - Handle OPTIONS preflight requests                      │   │  │
│  │  │                                                                │   │  │
│  │  │    ⚠️ Authorization: permitAll()                              │   │  │
│  │  │       - Cho phép tất cả requests qua filter chain             │   │  │
│  │  │       - JwtAuthenticationFilter sẽ xử lý authentication       │   │  │
│  │  └──────────────────────────────────────────────────────────────┘   │  │
│  │                            │                                           │  │
│  │                            ▼                                           │  │
│  │  ┌──────────────────────────────────────────────────────────────┐   │  │
│  │  │ 2. GlobalErrorFilter                                          │   │  │
│  │  │    Order: -1                                                  │   │  │
│  │  │    - Catch exceptions, format error, add CORS headers         │   │  │
│  │  └──────────────────────────────────────────────────────────────┘   │  │
│  │                            │                                           │  │
│  │                            ▼                                           │  │
│  │  ┌──────────────────────────────────────────────────────────────┐   │  │
│  │  │ 3. LoggingFilter                                             │   │  │
│  │  │    Order: -200                                                │   │  │
│  │  │    ✅ Generate Correlation ID, log request/response            │   │  │
│  │  └──────────────────────────────────────────────────────────────┘   │  │
│  │                            │                                           │  │
│  │                            ▼                                           │  │
│  │  ┌──────────────────────────────────────────────────────────────┐   │  │
│  │  │ 4. RateLimitFilter                                           │   │  │
│  │  │    Order: -150                                                │   │  │
│  │  │    ✅ Check rate limit (sẽ có X-User-Id sau JWT filter)      │   │  │
│  │  └──────────────────────────────────────────────────────────────┘   │  │
│  │                            │                                           │  │
│  │                            ▼                                           │  │
│  │  ┌──────────────────────────────────────────────────────────────┐   │  │
│  │  │ 5. ✅ JwtAuthenticationFilter                                 │   │  │
│  │  │    Order: -100                                                │   │  │
│  │  │                                                                │   │  │
│  │  │    ✅ Extract JWT token từ Authorization header               │   │  │
│  │  │       Authorization: Bearer <JWT_TOKEN>                        │   │  │
│  │  │       → Extract: <JWT_TOKEN>                                  │   │  │
│  │  │                                                                │   │  │
│  │  │    ✅ Validate token với Keycloak                             │   │  │
│  │  │       - Fetch JWKS từ Keycloak JWKS endpoint                  │   │  │
│  │  │       - Verify token signature                                │   │  │
│  │  │       - Validate issuer (realm)                              │   │  │
│  │  │       - Validate audience (client-id)                        │   │  │
│  │  │       - Check expiration                                      │   │  │
│  │  │                                                                │   │  │
│  │  │    ✅ Extract user info từ JWT claims                         │   │  │
│  │  │       - userId: sub (subject)                                 │   │  │
│  │  │       - username: preferred_username                           │   │  │
│  │  │       - roles: realm_access.roles + resource_access            │   │  │
│  │  │                                                                │   │  │
│  │  │    ✅ Add headers to request:                                 │   │  │
│  │  │       - X-User-Id: <userId>                                   │   │  │
│  │  │       - X-User-Name: <username>                               │   │  │
│  │  │       - X-User-Roles: <roles>                                 │   │  │
│  │  │       - X-Auth-Token: <JWT_TOKEN>                             │   │  │
│  │  │                                                                │   │  │
│  │  │    ✅ Block unauthorized requests                             │   │  │
│  │  │       - Missing token → 401 Unauthorized                      │   │  │
│  │  │       - Invalid token → 401 Unauthorized                      │   │  │
│  │  │       - Expired token → 401 Unauthorized                      │   │  │
│  │  │                                                                │   │  │
│  │  │    ✅ Skip authentication for public endpoints                 │   │  │
│  │  │       - /api/users/register                                   │   │  │
│  │  │       - /api/users/login                                      │   │  │
│  │  │       - /actuator/**                                          │   │  │
│  │  │       - /swagger-ui/**                                        │   │  │
│  │  └──────────────────────────────────────────────────────────────┘   │  │
│  │                            │                                           │  │
│  │                            ▼                                           │  │
│  │  ┌──────────────────────────────────────────────────────────────┐   │  │
│  │  │ 6. Route Matching & Circuit Breaker                         │   │  │
│  │  │                                                                │   │  │
│  │  │    ✅ Match route: /api/tickets/** → ticket-service           │   │  │
│  │  │    ✅ Apply Circuit Breaker                                   │   │  │
│  │  │    ✅ Strip prefix: /api/tickets → /tickets                   │   │  │
│  │  │                                                                │   │  │
│  │  │    ✅ Forward request WITH headers:                           │   │  │
│  │  │       - X-User-Id: <userId>                                   │   │  │
│  │  │       - X-User-Name: <username>                               │   │  │
│  │  │       - X-User-Roles: <roles>                                 │   │  │
│  │  │       - X-Auth-Token: <JWT_TOKEN>                             │   │  │
│  │  │       - X-Correlation-Id: <uuid>                                │   │  │
│  │  └──────────────────────────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────────────────┘
                                │
                                │ HTTP Request to Microservice
                                │ Headers: X-User-Id, X-Auth-Token, etc.
                                ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    TICKET SERVICE (Microservice)                            │
│                    Spring Boot (Spring MVC)                                  │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ TokenVerificationInterceptor                                         │  │
│  │    (Spring MVC Interceptor)                                          │  │
│  │                                                                        │  │
│  │    ✅ Check header: X-Auth-Token                                     │  │
│  │       - ✅ Có header này từ gateway                                  │  │
│  │       - → Verify token signature (nếu enabled)                        │  │
│  │       - → Token đã được validate ở gateway, chỉ verify signature      │  │
│  │                                                                        │  │
│  │    ✅ Extract user info từ gateway headers                            │  │
│  │       - ✅ X-User-Id: <userId>                                        │  │
│  │       - ✅ X-User-Name: <username>                                    │  │
│  │       - ✅ X-User-Roles: <roles>                                      │  │
│  │       - → Không cần parse JWT token nữa                               │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                            │                                                 │
│                            ▼                                                 │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ Controller                                                            │  │
│  │    - Sử dụng X-User-Id, X-User-Name từ gateway headers              │  │
│  │    - Không cần parse JWT token                                       │  │
│  │    - Xử lý business logic                                            │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 So sánh Request Headers

### ❌ Hiện tại (KHÔNG có JWT Filter)

**Request đến Gateway:**
```
Authorization: Bearer <JWT_TOKEN>
Origin: http://localhost:4200
```

**Request từ Gateway đến Microservice:**
```
Authorization: Bearer <JWT_TOKEN>  (original, không validate)
X-Correlation-Id: <uuid>
```

**Thiếu:**
- ❌ X-User-Id
- ❌ X-User-Name
- ❌ X-User-Roles
- ❌ X-Auth-Token

---

### ✅ Đề xuất (CÓ JWT Filter)

**Request đến Gateway:**
```
Authorization: Bearer <JWT_TOKEN>
Origin: http://localhost:4200
```

**Request từ Gateway đến Microservice:**
```
X-User-Id: <userId>                    ✅ Từ JWT claims
X-User-Name: <username>                ✅ Từ JWT claims
X-User-Roles: <roles>                  ✅ Từ JWT claims
X-Auth-Token: <JWT_TOKEN>               ✅ Forward token
X-Correlation-Id: <uuid>                ✅ Từ LoggingFilter
```

**Gateway đã validate:**
- ✅ Token signature
- ✅ Token expiration
- ✅ Token issuer
- ✅ Token audience

---

## 🔑 Keycloak Integration

```
┌─────────────────────────────────────────────────────────────────┐
│                    KEYCLOAK SERVER                              │
│                    http://localhost:8080                        │
│                                                                  │
│  Realm: train-ticket                                            │
│  Client: train-ticket-gateway                                   │
│                                                                  │
│  Endpoints:                                                      │
│    - JWKS: /auth/realms/train-ticket/protocol/openid-connect/   │
│            certs                                                 │
│    - Token: /auth/realms/train-ticket/protocol/openid-connect/  │
│             token                                                │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                │ HTTP Request
                                │ GET /auth/realms/train-ticket/
                                │     protocol/openid-connect/certs
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│              JwtAuthenticationFilter                           │
│              (trong API Gateway)                                │
│                                                                  │
│  1. Extract JWT token từ request                                │
│  2. Parse JWT header để lấy "kid" (Key ID)                      │
│  3. Fetch JWKS từ Keycloak                                      │
│  4. Find public key matching "kid"                              │
│  5. Verify token signature với public key                       │
│  6. Validate claims (issuer, audience, expiration)              │
│  7. Extract user info từ claims                                │
│  8. Add headers và forward request                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Kết luận

**Hiện tại:** Gateway **KHÔNG có authentication layer**, tất cả requests đều pass through.

**Đề xuất:** Implement `JwtAuthenticationFilter` để:
- ✅ Validate JWT token ở gateway level
- ✅ Extract và forward user info
- ✅ Block unauthorized requests
- ✅ Centralize authentication logic
