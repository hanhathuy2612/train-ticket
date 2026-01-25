# Gateway Configuration Audit Report

## 📋 Tổng quan

Báo cáo này phân tích cấu hình Gateway, xác định các config dư thừa, thiếu sót, và vẽ lại flow security hiện tại.

**Ngày kiểm tra:** 2026-01-25  
**Phiên bản:** Spring Cloud Gateway 2025.0.1 (WebFlux)

---

## 🔍 Phân tích GatewayConfig.java

### ✅ Config đang được sử dụng

1. **`userKeyResolver()` - @Primary**
   - ✅ **Được sử dụng:** Spring Cloud Gateway sử dụng cho rate limiting
   - **Mục đích:** Resolve key dựa trên User ID từ header `X-User-Id`, fallback về IP
   - **Order:** -200 (sau LoggingFilter, trước RateLimitFilter)

2. **`corsConfigurationSource()` (trong SecurityConfig)**
   - ✅ **Được sử dụng:** Spring Security WebFlux sử dụng cho CORS
   - **Mục đích:** Cấu hình CORS cho tất cả requests

3. **`securityWebFilterChain()` (trong SecurityConfig)**
   - ✅ **Được sử dụng:** Spring Security WebFlux
   - **Mục đích:** Cấu hình security filter chain

### ⚠️ Config dư thừa (không được sử dụng)

1. **`ipKeyResolver()` - KHÔNG @Primary**
   - ❌ **Không được sử dụng:** Vì `userKeyResolver()` đã được đánh dấu `@Primary`
   - **Đề xuất:** Xóa hoặc giữ lại nếu có kế hoạch sử dụng trong tương lai
   - **Lý do:** Spring Cloud Gateway chỉ sử dụng một KeyResolver, và `@Primary` đã chỉ định `userKeyResolver`

2. **`webClient()` Bean**
   - ❌ **Không được sử dụng:** Không có file nào inject bean này
   - **Đề xuất:** Xóa hoặc giữ lại nếu có kế hoạch implement JwtAuthenticationFilter
   - **Lý do:** Được định nghĩa để dùng cho Keycloak requests nhưng chưa có filter nào sử dụng

3. **`objectMapper()` Bean**
   - ❌ **Không được sử dụng:** Không có file nào inject bean này
   - **Đề xuất:** Xóa hoặc giữ lại nếu có kế hoạch implement JwtAuthenticationFilter
   - **Lý do:** Được định nghĩa để parse JWT token nhưng chưa có filter nào sử dụng

4. **`KeycloakConfig` Class**
   - ❌ **Không được sử dụng:** Không có file nào inject bean này
   - **Đề xuất:** Xóa hoặc giữ lại nếu có kế hoạch implement JwtAuthenticationFilter
   - **Lý do:** Được định nghĩa để lưu Keycloak config nhưng chưa có filter nào sử dụng

### ❌ Config thiếu sót

1. **`JwtAuthenticationFilter` - THIẾU**
   - ❌ **Không tồn tại:** Được đề cập trong `SecurityConfig.java` và docs nhưng không có trong codebase
   - **Tác động:** Gateway hiện tại **KHÔNG có authentication** ở gateway level
   - **Hậu quả:** 
     - Tất cả requests đều được permitAll() trong SecurityConfig
     - Authentication chỉ được thực hiện ở các microservice (user-service, ticket-service, etc.)
     - Gateway không validate JWT token, không extract user info, không forward token qua header `X-Auth-Token`
   - **Đề xuất:** Implement JwtAuthenticationFilter để:
     - Validate JWT token từ Keycloak
     - Extract user info (userId, username, roles) từ token
     - Add headers: `X-User-Id`, `X-User-Name`, `X-User-Roles`, `X-Auth-Token`
     - Block unauthorized requests (401)

2. **Token Validation Utility**
   - ❌ **Thiếu:** Không có utility để validate Keycloak JWT token
   - **Đề xuất:** Implement `KeycloakTokenValidator` hoặc sử dụng shared utility

---

## 🔐 Flow Security Hiện Tại

### Flow hiện tại (KHÔNG có JWT Authentication ở Gateway)

```
┌─────────────────────────────────────────────────────────────────┐
│                    CLIENT REQUEST                                │
│              (Browser/Mobile App)                               │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│              API GATEWAY (Port 8189)                            │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ 1. Spring Security WebFlux Filter Chain                   │ │
│  │    - CORS: ✅ Xử lý CORS headers                           │ │
│  │    - CSRF: ❌ Disabled                                     │ │
│  │    - HTTP Basic: ❌ Disabled                               │ │
│  │    - Form Login: ❌ Disabled                               │ │
│  │    - Authorization: ✅ permitAll() - CHO PHÉP TẤT CẢ       │ │
│  └──────────────────────────────────────────────────────────┘ │
│                            │                                     │
│                            ▼                                     │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ 2. Gateway Global Filters (Order)                        │ │
│  │    a) GlobalErrorFilter (Order: -1)                       │ │
│  │       - Xử lý errors, thêm CORS headers                   │ │
│  │    b) LoggingFilter (Order: -200)                        │ │
│  │       - Log request/response, thêm Correlation ID         │ │
│  │    c) RateLimitFilter (Order: -150)                      │ │
│  │       - Rate limiting dựa trên User ID hoặc IP             │ │
│  │    d) ❌ JwtAuthenticationFilter - KHÔNG TỒN TẠI          │ │
│  └──────────────────────────────────────────────────────────┘ │
│                            │                                     │
│                            ▼                                     │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ 3. Route Matching & Circuit Breaker                      │ │
│  │    - Match route dựa trên path                            │ │
│  │    - Apply Circuit Breaker (Resilience4j)                 │ │
│  │    - Strip prefix, forward to microservice                │ │
│  └──────────────────────────────────────────────────────────┘ │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│              MICROSERVICE (user-service, ticket-service, etc.)  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ TokenVerificationInterceptor (Spring MVC Interceptor)     │ │
│  │    - Kiểm tra header X-Auth-Token                         │ │
│  │    - Verify token signature (nếu enabled)                 │ │
│  │    - ❌ Gateway KHÔNG forward token qua X-Auth-Token     │ │
│  │      vì không có JwtAuthenticationFilter                  │ │
│  └──────────────────────────────────────────────────────────┘ │
│                            │                                     │
│                            ▼                                     │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ Controller                                                │ │
│  │    - Xử lý business logic                                 │ │
│  └──────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### ⚠️ Vấn đề với flow hiện tại

1. **Gateway không validate JWT token**
   - Tất cả requests đều được permitAll()
   - Không có authentication ở gateway level
   - Unauthorized requests vẫn có thể đến microservice

2. **Gateway không forward token**
   - Gateway không extract token từ `Authorization: Bearer <token>`
   - Gateway không forward token qua header `X-Auth-Token`
   - Microservice không nhận được token để verify

3. **Gateway không extract user info**
   - Gateway không extract userId, username, roles từ JWT
   - Gateway không add headers: `X-User-Id`, `X-User-Name`, `X-User-Roles`
   - Rate limiting chỉ dựa trên IP nếu không có header `X-User-Id`

---

## 🔄 Flow Security Đề Xuất (Có JWT Authentication)

```
┌─────────────────────────────────────────────────────────────────┐
│                    CLIENT REQUEST                                │
│              Authorization: Bearer <JWT_TOKEN>                   │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│              API GATEWAY (Port 8189)                            │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ 1. Spring Security WebFlux Filter Chain                   │ │
│  │    - CORS: ✅ Xử lý CORS headers                           │ │
│  │    - CSRF: ❌ Disabled                                     │ │
│  │    - Authorization: ✅ permitAll() - Cho phép qua filter  │ │
│  └──────────────────────────────────────────────────────────┘ │
│                            │                                     │
│                            ▼                                     │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ 2. Gateway Global Filters (Order)                        │ │
│  │    a) GlobalErrorFilter (Order: -1)                      │ │
│  │       - Xử lý errors, thêm CORS headers                    │ │
│  │    b) LoggingFilter (Order: -200)                         │ │
│  │       - Log request/response, thêm Correlation ID          │ │
│  │    c) RateLimitFilter (Order: -150)                       │ │
│  │       - Rate limiting dựa trên User ID hoặc IP            │ │
│  │    d) ✅ JwtAuthenticationFilter (Order: -100)             │ │
│  │       - Extract token từ Authorization header             │ │
│  │       - Validate token với Keycloak JWKS                  │ │
│  │       - Extract user info (userId, username, roles)        │ │
│  │       - Add headers: X-User-Id, X-User-Name, X-User-Roles│ │
│  │       - Forward token qua X-Auth-Token header             │ │
│  │       - Block unauthorized requests (401)                  │ │
│  └──────────────────────────────────────────────────────────┘ │
│                            │                                     │
│                            ▼                                     │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ 3. Route Matching & Circuit Breaker                      │ │
│  │    - Match route dựa trên path                            │ │
│  │    - Apply Circuit Breaker (Resilience4j)                 │ │
│  │    - Strip prefix, forward to microservice                │ │
│  │    - Forward headers: X-User-Id, X-Auth-Token, etc.     │ │
│  └──────────────────────────────────────────────────────────┘ │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│              MICROSERVICE (user-service, ticket-service, etc.)  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ TokenVerificationInterceptor (Spring MVC Interceptor)     │ │
│  │    - Nhận header X-Auth-Token từ gateway                  │ │
│  │    - Verify token signature (nếu enabled)                 │ │
│  │    - ✅ Token đã được validate ở gateway                  │ │
│  └──────────────────────────────────────────────────────────┘ │
│                            │                                     │
│                            ▼                                     │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ Controller                                                │ │
│  │    - Sử dụng X-User-Id, X-User-Name từ gateway headers    │ │
│  │    - Xử lý business logic                                 │ │
│  └──────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📊 So sánh Config

| Component | Hiện tại | Đề xuất | Ghi chú |
|-----------|----------|---------|---------|
| **JwtAuthenticationFilter** | ❌ Không có | ✅ Cần implement | **QUAN TRỌNG** |
| **KeycloakTokenValidator** | ❌ Không có | ✅ Cần implement | Validate JWT với Keycloak |
| **WebClient Bean** | ⚠️ Có nhưng không dùng | ✅ Giữ lại để dùng cho JWT filter | |
| **ObjectMapper Bean** | ⚠️ Có nhưng không dùng | ✅ Giữ lại để dùng cho JWT filter | |
| **KeycloakConfig** | ⚠️ Có nhưng không dùng | ✅ Giữ lại để dùng cho JWT filter | |
| **ipKeyResolver** | ⚠️ Có nhưng không dùng | ❌ Xóa hoặc giữ | Không cần thiết vì có @Primary |
| **userKeyResolver** | ✅ Đang dùng | ✅ Giữ lại | @Primary, dùng cho rate limiting |
| **CORS Config** | ✅ Đang dùng | ✅ Giữ lại | SecurityConfig |
| **SecurityWebFilterChain** | ✅ Đang dùng | ✅ Giữ lại | permitAll() để filter xử lý |

---

## 🎯 Đề xuất Hành động

### 🔴 Ưu tiên cao (Critical)

1. **Implement JwtAuthenticationFilter**
   - Validate JWT token từ Keycloak
   - Extract user info và add headers
   - Block unauthorized requests
   - **File:** `apigateway/src/main/java/com/example/apigateway/filter/JwtAuthenticationFilter.java`

2. **Implement KeycloakTokenValidator**
   - Fetch JWKS từ Keycloak
   - Verify token signature
   - Validate issuer, audience, expiration
   - **File:** `apigateway/src/main/java/com/example/apigateway/util/KeycloakTokenValidator.java`

### 🟡 Ưu tiên trung bình (Medium)

3. **Xóa hoặc giữ lại ipKeyResolver**
   - Nếu không có kế hoạch sử dụng, nên xóa
   - Hoặc giữ lại nếu muốn có option switch giữa user-based và IP-based rate limiting

### 🟢 Ưu tiên thấp (Low)

4. **Cập nhật documentation**
   - Cập nhật docs để phản ánh flow security thực tế
   - Xóa references đến JwtAuthenticationFilter nếu chưa implement

---

## 📝 Tóm tắt

### ✅ Điểm mạnh
- CORS được cấu hình đúng
- Rate limiting hoạt động
- Logging và error handling tốt
- Circuit breaker được cấu hình

### ❌ Điểm yếu
- **KHÔNG có JWT authentication ở gateway level** ⚠️
- Gateway không validate token
- Gateway không forward token đến microservice
- Gateway không extract user info

### 🎯 Kết luận

Gateway hiện tại **THIẾU authentication layer**. Tất cả requests đều được permitAll() và authentication chỉ được thực hiện ở microservice level. Điều này không phù hợp với kiến trúc microservice best practices, nơi gateway nên là điểm kiểm soát authentication duy nhất.

**Khuyến nghị:** Implement JwtAuthenticationFilter ngay lập tức để đảm bảo security ở gateway level.
