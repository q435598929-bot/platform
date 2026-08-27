package com.platform.security;

import com.platform.security.AuthDtos.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/api/auth/login") public LoginResponse login(@Valid @RequestBody LoginRequest request,
                                                                HttpServletResponse response) {
        LoginResponse result = service.login(request);
        long maxAge = Math.max(0, result.expiresAt().getEpochSecond() - java.time.Instant.now().getEpochSecond());
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("PLATFORM_TOKEN", result.accessToken())
                .httpOnly(true).sameSite("Lax").path("/").maxAge(maxAge).build().toString());
        return result;
    }
    @GetMapping("/api/auth/me") public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.me(jwt.getClaim("uid"), jwt.getSubject(), jwt.getClaimAsString("displayName"),
                jwt.getClaimAsStringList("roles"), jwt.getClaimAsStringList("permissions"));
    }
    @PostMapping("/api/auth/logout") public Map<String, Boolean> logout(@AuthenticationPrincipal Jwt jwt,
                                                                        HttpServletResponse response) {
        service.logout(jwt.getId(), jwt.getExpiresAt());
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("PLATFORM_TOKEN", "")
                .httpOnly(true).sameSite("Lax").path("/").maxAge(0).build().toString());
        return Map.of("loggedOut", true);
    }

    @GetMapping("/api/admin/users") public List<UserResponse> users() { return service.users(); }
    @PostMapping("/api/admin/users") @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) { return service.create(request); }
    @PutMapping("/api/admin/users/{id}/roles")
    public UserResponse roles(@PathVariable Long id, @RequestBody UpdateRolesRequest request) { return service.updateRoles(id, request); }
    @PutMapping("/api/admin/users/{id}/enabled")
    public UserResponse enabled(@PathVariable Long id, @RequestBody UpdateEnabledRequest request) { return service.updateEnabled(id, request.enabled()); }
    @PutMapping("/api/admin/users/{id}/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) { service.resetPassword(id, request.password()); }
    @GetMapping("/api/admin/roles") public List<RoleResponse> roles() { return service.roles(); }
}
