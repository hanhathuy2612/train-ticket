package com.example.userservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.userservice.dto.ApiResponse;
import com.example.userservice.dto.AuthResponse;
import com.example.userservice.dto.ChangePasswordRequest;
import com.example.userservice.dto.LoginRequest;
import com.example.userservice.dto.RefreshTokenRequest;
import com.example.userservice.dto.RegisterRequest;
import com.example.userservice.dto.ResetPasswordRequest;
import com.example.userservice.dto.UpdateProfileRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.entity.User.Role;
import com.example.userservice.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserService userService;

    // ============ Authentication Endpoints ============

    /**
     * Register a new user
     * POST /users/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        logger.info("Register request for username: {}", request.getUsername());
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    /**
     * Login with username/email and password
     * POST /users/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        logger.info("Login request for user: {}", request.getUsername());
        String ipAddress = getClientIpAddress(httpRequest);
        AuthResponse response = userService.login(request, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Refresh access token
     * POST /users/refresh-token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        logger.info("Token refresh request");
        AuthResponse response = userService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    /**
     * Logout current user
     * POST /users/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("X-User-Id") Long userId) {
        logger.info("Logout request for user: {}", userId);
        userService.logout(userId);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    // ============ Profile Endpoints ============

    /**
     * Get current user's profile
     * GET /users/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @RequestHeader("X-User-Id") Long userId) {
        logger.debug("Get profile for user: {}", userId);
        UserResponse response = userService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update current user's profile
     * PUT /users/me
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        logger.info("Update profile for user: {}", userId);
        UserResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", response));
    }

    /**
     * Change password
     * POST /users/me/change-password
     */
    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        logger.info("Change password for user: {}", userId);
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    // ============ Password Reset Endpoints ============

    /**
     * Request password reset
     * POST /users/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        logger.info("Password reset request for email: {}", request.getEmail());
        userService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
                "If the email exists, a password reset link has been sent", null));
    }

    /**
     * Reset password with token
     * POST /users/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword) {
        logger.info("Password reset with token");
        userService.resetPassword(token, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null));
    }

    // ============ User Lookup Endpoints ============

    /**
     * Get user by ID
     * GET /users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        logger.debug("Get user by id: {}", id);
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get user by username
     * GET /users/username/{username}
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByUsername(
            @PathVariable String username) {
        logger.debug("Get user by username: {}", username);
        UserResponse response = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ============ Admin Endpoints ============

    /**
     * Get all users (admin only)
     * GET /users?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        logger.debug("Get all users - page: {}, size: {}", page, size);
        
        size = Math.min(size, MAX_PAGE_SIZE);
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Search users (admin only)
     * GET /users/search?q=query
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.debug("Search users with query: {}", q);
        
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<UserResponse> users = userService.searchUsers(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Activate/Deactivate user (admin only)
     * PUT /users/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        logger.info("Update user {} status to: {}", id, active);
        userService.updateUserStatus(id, active);
        return ResponseEntity.ok(ApiResponse.success("User status updated", null));
    }

    /**
     * Add role to user (admin only)
     * POST /users/{id}/roles/{role}
     */
    @PostMapping("/{id}/roles/{role}")
    public ResponseEntity<ApiResponse<UserResponse>> addUserRole(
            @PathVariable Long id,
            @PathVariable Role role) {
        logger.info("Adding role {} to user {}", role, id);
        UserResponse response = userService.updateUserRole(id, role, true);
        return ResponseEntity.ok(ApiResponse.success("Role added", response));
    }

    /**
     * Remove role from user (admin only)
     * DELETE /users/{id}/roles/{role}
     */
    @DeleteMapping("/{id}/roles/{role}")
    public ResponseEntity<ApiResponse<UserResponse>> removeUserRole(
            @PathVariable Long id,
            @PathVariable Role role) {
        logger.info("Removing role {} from user {}", role, id);
        UserResponse response = userService.updateUserRole(id, role, false);
        return ResponseEntity.ok(ApiResponse.success("Role removed", response));
    }

    // ============ Token Validation Endpoint (for Gateway) ============

    /**
     * Validate token and return user info
     * GET /users/validate
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<UserResponse>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        UserResponse response = userService.validateAndGetUser(token);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Health check
     * GET /users/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("User Service is healthy"));
    }

    // ============ Helper Methods ============

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
