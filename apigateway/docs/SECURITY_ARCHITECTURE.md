# Security Architecture - API Gateway vs Distributed

## Hiện tại: Centralized Security ở API Gateway

### Cách hoạt động:

1. Client gửi request với Keycloak JWT token → API Gateway
2. Gateway validate token với Keycloak
3. Gateway extract user info và thêm vào headers: `X-User-Id`, `X-User-Name`, `X-User-Roles`
4. Gateway forward request đến downstream services
5. Services trust headers từ gateway (không validate lại)

### Ưu điểm:

✅ **Single point of control** - Quản lý security ở một nơi
✅ **Giảm code duplication** - Services không cần implement security
✅ **Performance tốt** - Validate token một lần
✅ **Dễ maintain** - Update security logic ở một chỗ
✅ **Separation of concerns** - Services tập trung vào business logic

### Nhược điểm:

❌ **Single point of failure** - Gateway down = toàn bộ hệ thống down
❌ **Security risk** - Services trust headers, có thể bị spoof nếu bypass gateway
❌ **Khó scale** - Gateway có thể trở thành bottleneck
❌ **Trust boundary** - Services phải trust gateway hoàn toàn

---

## Alternative: Distributed Security (Mỗi service tự validate)

### Cách hoạt động:

1. Client gửi request với Keycloak JWT token → API Gateway
2. Gateway chỉ route (không validate)
3. Gateway forward token đến services
4. Mỗi service tự validate token với Keycloak
5. Services extract user info từ token

### Ưu điểm:

✅ **Better security** - Mỗi service tự validate, không trust headers
✅ **Resilience** - Service vẫn hoạt động nếu gateway có vấn đề
✅ **No single point of failure** - Security không phụ thuộc vào gateway
✅ **Flexibility** - Mỗi service có thể có security rules riêng

### Nhược điểm:

❌ **Code duplication** - Mỗi service phải implement security
❌ **Performance overhead** - Validate token nhiều lần
❌ **Maintenance overhead** - Phải update security ở nhiều nơi
❌ **Complexity** - Phức tạp hơn về mặt kiến trúc

---

## Recommended: Hybrid Approach (Best of both worlds)

### Cách hoạt động:

1. Client gửi request với Keycloak JWT token → API Gateway
2. Gateway validate token và extract user info
3. Gateway forward **CẢ token VÀ user context headers** đến services
4. Services có thể:
   - **Option A**: Trust headers từ gateway (nhanh, cho internal services)
   - **Option B**: Verify token signature (lightweight, cho critical operations)
   - **Option C**: Full validation (cho external-facing services)

### Implementation:

#### Gateway:

- Validate token với Keycloak
- Forward token trong header: `X-Auth-Token`
- Forward user context: `X-User-Id`, `X-User-Name`, `X-User-Roles`

#### Services:

- Có interceptor/filter để verify token signature (lightweight)
- Hoặc trust headers nếu request đến từ trusted network (mTLS/internal network)

### Ưu điểm:

✅ **Security tốt hơn** - Services có thể verify token nếu cần
✅ **Performance tốt** - Có thể trust headers cho internal calls
✅ **Flexibility** - Mỗi service chọn level of verification
✅ **Defense in depth** - Nhiều lớp bảo vệ

---

## So sánh các approach:

| Tiêu chí        | Centralized (Hiện tại) | Distributed | Hybrid    |
| --------------- | ---------------------- | ----------- | --------- |
| Security        | ⚠️ Medium              | ✅ High     | ✅ High   |
| Performance     | ✅ Best                | ⚠️ Medium   | ✅ Good   |
| Maintainability | ✅ Easy                | ❌ Hard     | ⚠️ Medium |
| Resilience      | ❌ Low                 | ✅ High     | ✅ High   |
| Complexity      | ✅ Low                 | ❌ High     | ⚠️ Medium |

---

## Recommendation cho project này:

### Cho Development/Staging:

✅ **Giữ Centralized Security** (hiện tại) - Đơn giản, dễ maintain

### Cho Production:

✅ **Chuyển sang Hybrid Approach**:

1. Gateway validate và forward token
2. Services verify token signature (lightweight check)
3. Hoặc dùng mTLS giữa gateway và services để đảm bảo requests đến từ gateway

### Cải thiện ngay:

1. **Thêm token vào headers** - Forward cả token để services có thể verify
2. **Network security** - Đảm bảo services chỉ nhận requests từ gateway (firewall/network policies)
3. **Service-to-service auth** - Dùng service account tokens cho internal calls

---

## Code Changes Needed cho Hybrid:

### Gateway:

```java
// Forward token để services có thể verify
ServerHttpRequest modifiedRequest = request.mutate()
    .header("X-Auth-Token", token)  // Forward token
    .header("X-User-Id", userId)
    .header("X-User-Name", username)
    .header("X-User-Roles", String.join(",", roles))
    .build();
```

### Services:

```java
// Lightweight token verification interceptor
@Component
public class TokenVerificationInterceptor implements HandlerInterceptor {
    public boolean preHandle(HttpServletRequest request, ...) {
        String token = request.getHeader("X-Auth-Token");
        if (token != null) {
            // Lightweight signature verification
            verifyTokenSignature(token);
        }
        return true;
    }
}
```
