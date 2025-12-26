package com.example.userservice.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.example.userservice.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private Set<String> roles;
        private Boolean emailVerified;
        private LocalDateTime lastLoginAt;
    }

    public static AuthResponse from(String accessToken, String refreshToken, Long expiresIn, User user) {
        Set<String> roleNames = new java.util.HashSet<>();
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> roleNames.add(role.name()));
        }

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .roles(roleNames)
                        .emailVerified(user.getEmailVerified())
                        .lastLoginAt(user.getLastLoginAt())
                        .build())
                .build();
    }
}
