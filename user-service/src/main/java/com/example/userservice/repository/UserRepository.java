package com.example.userservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.userservice.entity.User;
import com.example.userservice.entity.User.Role;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Basic find operations
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    Optional<User> findByPasswordResetToken(String token);
    
    Optional<User> findByEmailVerificationToken(String token);
    
    // Existence checks
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhoneNumber(String phoneNumber);
    
    boolean existsByIdNumber(String idNumber);
    
    // Find active users
    Optional<User> findByUsernameAndActiveTrue(String username);
    
    Optional<User> findByEmailAndActiveTrue(String email);
    
    List<User> findByActiveTrue();
    
    Page<User> findByActiveTrue(Pageable pageable);
    
    // Find by role
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRole(@Param("role") Role role);
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role AND u.active = true")
    Page<User> findByRoleAndActiveTrue(@Param("role") Role role, Pageable pageable);
    
    // Search users
    @Query("SELECT u FROM User u WHERE u.active = true AND " +
            "(LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);
    
    // Update operations
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime, u.lastLoginIp = :ip WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("loginTime") LocalDateTime loginTime, 
            @Param("ip") String ip);
    
    @Modifying
    @Query("UPDATE User u SET u.password = :password, u.passwordResetToken = null, " +
            "u.passwordResetExpiry = null WHERE u.id = :userId")
    void updatePassword(@Param("userId") Long userId, @Param("password") String password);
    
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true, u.emailVerificationToken = null WHERE u.id = :userId")
    void verifyEmail(@Param("userId") Long userId);
    
    @Modifying
    @Query("UPDATE User u SET u.active = :active WHERE u.id = :userId")
    void updateActiveStatus(@Param("userId") Long userId, @Param("active") boolean active);
    
    // Statistics
    long countByActiveTrue();
    
    long countByActiveFalse();
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r = :role")
    long countByRole(@Param("role") Role role);
    
    // Find users with expired password reset tokens
    @Query("SELECT u FROM User u WHERE u.passwordResetToken IS NOT NULL AND u.passwordResetExpiry < :now")
    List<User> findWithExpiredPasswordResetTokens(@Param("now") LocalDateTime now);
}
