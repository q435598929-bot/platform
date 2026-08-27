package com.platform.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String accessToken, Instant expiresAt, UserResponse user) {}
    public record UserResponse(Long id, String username, String displayName, boolean enabled,
                               List<String> roles, List<String> permissions) {}
    public record CreateUserRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,100}") String username,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 150) String displayName,
            List<String> roles) {}
    public record UpdateRolesRequest(List<String> roles) {}
    public record UpdateEnabledRequest(boolean enabled) {}
    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 100) String password) {}
    public record RoleResponse(String code, String name, List<String> permissions) {}
}
