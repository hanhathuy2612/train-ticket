package com.example.userservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.userservice.dto.AuthResponse;
import com.example.userservice.dto.ChangePasswordRequest;
import com.example.userservice.dto.LoginRequest;
import com.example.userservice.dto.RefreshTokenRequest;
import com.example.userservice.dto.RegisterRequest;
import com.example.userservice.dto.UpdateProfileRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.entity.User;
import com.example.userservice.entity.User.Role;
import com.example.userservice.exception.InvalidCredentialsException;
import com.example.userservice.exception.InvalidTokenException;
import com.example.userservice.exception.UserAlreadyExistsException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String USER_CACHE_PREFIX = "user:";
    private static final String SESSION_CACHE_PREFIX = "user:session:";
    private static final String REFRESH_TOKEN_PREFIX = "user:refresh:";
    private static final int SESSION_HOURS = 24;
    private static final int REFRESH_TOKEN_DAYS = 7;
    private static final int PASSWORD_RESET_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    // ============ Authentication Methods ============

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        logger.info("Registering new user with username: {}", request.getUsername());

        validateRegistration(request);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .city(request.getCity())
                .idNumber(request.getIdNumber())
                .build();

        user = userRepository.save(user);
        logger.info("User registered successfully with id: {}", user.getId());

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        logger.info("Login attempt for user: {}", request.getUsername());

        User user = userRepository.findByUsernameOrEmail(request.getUsername(), request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Invalid password for user: {}", request.getUsername());
            throw new InvalidCredentialsException();
        }

        if (!user.getActive()) {
            logger.warn("Login attempt for inactive user: {}", request.getUsername());
            throw new InvalidCredentialsException("User account is inactive");
        }

        // Update last login info
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now(), ipAddress);

        logger.info("User {} logged in successfully", user.getUsername());
        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        Long userId = jwtUtil.extractUserId(refreshToken);

        // Check if refresh token is still valid in Redis
        String storedToken = (String) redisTemplate.opsForValue()
                .get(REFRESH_TOKEN_PREFIX + userId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.getActive()) {
            throw new InvalidCredentialsException("User account is inactive");
        }

        logger.info("Token refreshed for user: {}", username);
        return createAuthResponse(user);
    }

    @Transactional
    public void logout(Long userId) {
        logger.info("Logging out user: {}", userId);
        redisTemplate.delete(SESSION_CACHE_PREFIX + userId);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        redisTemplate.delete(USER_CACHE_PREFIX + userId);
    }

    // ============ User Management Methods ============

    @Cacheable(value = "users", key = "#id")
    public UserResponse getUserById(Long id) {
        logger.debug("Fetching user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return UserResponse.from(user);
    }

    public UserResponse getUserByUsername(String username) {
        logger.debug("Fetching user by username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("username", username));
        return UserResponse.from(user);
    }

    public UserResponse getMyProfile(Long userId) {
        return getUserById(userId);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        logger.info("Updating profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Check email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("email", request.getEmail());
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(false); // Require re-verification
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
            user.setPhoneVerified(false);
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getIdNumber() != null) {
            user.setIdNumber(request.getIdNumber());
        }

        user = userRepository.save(user);
        logger.info("Profile updated for user: {}", userId);

        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        logger.info("Changing password for user: {}", userId);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        userRepository.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()));
        
        // Invalidate all sessions
        logout(userId);
        
        logger.info("Password changed for user: {}", userId);
    }

    @Transactional
    public String requestPasswordReset(String email) {
        logger.info("Password reset requested for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("email", email));

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetExpiry(LocalDateTime.now().plusHours(PASSWORD_RESET_HOURS));
        userRepository.save(user);

        // TODO: Send email with reset link
        logger.info("Password reset token generated for user: {}", user.getId());

        return resetToken;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        logger.info("Resetting password with token");

        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        if (user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Password reset token has expired");
        }

        userRepository.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
        logout(user.getId());

        logger.info("Password reset successful for user: {}", user.getId());
    }

    // ============ Admin Methods ============

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        logger.debug("Fetching all users with pagination");
        return userRepository.findByActiveTrue(pageable)
                .map(UserResponse::from);
    }

    public Page<UserResponse> searchUsers(String query, Pageable pageable) {
        logger.debug("Searching users with query: {}", query);
        return userRepository.searchUsers(query, pageable)
                .map(UserResponse::from);
    }

    public List<UserResponse> getUsersByRole(Role role) {
        logger.debug("Fetching users by role: {}", role);
        return userRepository.findByRole(role).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void updateUserStatus(Long userId, boolean active) {
        logger.info("Updating status for user {}: active={}", userId, active);
        
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        
        userRepository.updateActiveStatus(userId, active);
        
        if (!active) {
            logout(userId);
        }
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public UserResponse updateUserRole(Long userId, Role role, boolean add) {
        logger.info("Updating role for user {}: role={}, add={}", userId, role, add);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (add) {
            user.addRole(role);
        } else {
            user.removeRole(role);
        }

        user = userRepository.save(user);
        return UserResponse.from(user);
    }

    // ============ Token Validation (for Gateway) ============

    public boolean validateSession(Long userId, String token) {
        String storedToken = (String) redisTemplate.opsForValue()
                .get(SESSION_CACHE_PREFIX + userId);
        return storedToken != null && storedToken.equals(token);
    }

    public UserResponse validateAndGetUser(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new InvalidTokenException();
        }

        Long userId = jwtUtil.extractUserId(token);
        if (!validateSession(userId, token)) {
            throw new InvalidTokenException("Session has been invalidated");
        }

        return getUserById(userId);
    }

    // ============ Helper Methods ============

    private void validateRegistration(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("email", request.getEmail());
        }
        if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new UserAlreadyExistsException("phone number", request.getPhoneNumber());
        }
    }

    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // Cache tokens in Redis
        redisTemplate.opsForValue().set(
                SESSION_CACHE_PREFIX + user.getId(),
                accessToken,
                SESSION_HOURS,
                TimeUnit.HOURS
        );
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                REFRESH_TOKEN_DAYS,
                TimeUnit.DAYS
        );

        return AuthResponse.from(
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpirationMs() / 1000,
                user
        );
    }
}
