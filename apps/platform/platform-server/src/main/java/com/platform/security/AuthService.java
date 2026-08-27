package com.platform.security;

import com.platform.security.AuthDtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final JwtEncoder jwtEncoder;
    private final Duration tokenTtl;
    private final String bootstrapUsername;
    private final String bootstrapPassword;
    private final String bootstrapDisplayName;

    public AuthService(JdbcTemplate jdbc, PasswordEncoder passwords, JwtEncoder jwtEncoder,
                       @Value("${platform.security.token-ttl}") Duration tokenTtl,
                       @Value("${platform.security.bootstrap-admin.username}") String bootstrapUsername,
                       @Value("${platform.security.bootstrap-admin.password}") String bootstrapPassword,
                       @Value("${platform.security.bootstrap-admin.display-name}") String bootstrapDisplayName) {
        this.jdbc = jdbc;
        this.passwords = passwords;
        this.jwtEncoder = jwtEncoder;
        this.tokenTtl = tokenTtl;
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapPassword = bootstrapPassword;
        this.bootstrapDisplayName = bootstrapDisplayName;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void bootstrapAdministrator() {
        jdbc.update("DELETE FROM platform_revoked_token WHERE expires_at < NOW()");
        if (count("SELECT COUNT(*) FROM platform_user WHERE username = ?", bootstrapUsername) > 0) return;
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("INSERT INTO platform_user(username,password_hash,display_name,enabled,created_at,updated_at) VALUES(?,?,?,?,?,?)",
                bootstrapUsername, passwords.encode(bootstrapPassword), bootstrapDisplayName, true, now, now);
        Long id = jdbc.queryForObject("SELECT id FROM platform_user WHERE username = ?", Long.class, bootstrapUsername);
        jdbc.update("INSERT INTO platform_user_role(user_id,role_id) SELECT ?,id FROM platform_role WHERE code='ADMIN'", id);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserRow user = findByUsername(request.username());
        if (user == null || !user.enabled() || !passwords.matches(request.password(), user.passwordHash())) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        UserResponse response = response(user);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(tokenTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("platform").subject(user.username())
                .id(UUID.randomUUID().toString()).issuedAt(now).expiresAt(expiresAt)
                .claim("uid", user.id()).claim("ver", user.tokenVersion()).claim("displayName", user.displayName())
                .claim("roles", response.roles()).claim("permissions", response.permissions()).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, expiresAt, response);
    }

    public UserResponse me(Long id, String username, String displayName, List<String> roles, List<String> permissions) {
        return new UserResponse(id, username, displayName, true,
                roles == null ? List.of() : roles, permissions == null ? List.of() : permissions);
    }

    @Transactional
    public void logout(String jti, Instant expiresAt) {
        if (count("SELECT COUNT(*) FROM platform_revoked_token WHERE jti = ?", jti) == 0) {
            jdbc.update("INSERT INTO platform_revoked_token(jti,expires_at,revoked_at) VALUES(?,?,?)",
                    jti, Timestamp.from(expiresAt), LocalDateTime.now());
        }
    }

    @Transactional(readOnly = true)
    public List<UserResponse> users() {
        return jdbc.query("SELECT id,username,password_hash,display_name,enabled,token_version FROM platform_user ORDER BY id",
                (rs, row) -> new UserRow(rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"),
                        rs.getString("display_name"), rs.getBoolean("enabled"), rs.getInt("token_version"))).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> roles() {
        return jdbc.query("SELECT code,name FROM platform_role ORDER BY id", (rs, row) ->
                new RoleResponse(rs.getString("code"), rs.getString("name"), permissionsForRole(rs.getString("code"))));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (count("SELECT COUNT(*) FROM platform_user WHERE username = ?", request.username()) > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("INSERT INTO platform_user(username,password_hash,display_name,enabled,created_at,updated_at) VALUES(?,?,?,?,?,?)",
                request.username(), passwords.encode(request.password()), request.displayName(), true, now, now);
        Long id = jdbc.queryForObject("SELECT id FROM platform_user WHERE username = ?", Long.class, request.username());
        replaceRoles(id, request.roles() == null || request.roles().isEmpty() ? List.of("VIEWER") : request.roles());
        return response(requireUser(id));
    }

    @Transactional
    public UserResponse updateRoles(Long id, UpdateRolesRequest request) {
        requireUser(id);
        replaceRoles(id, request.roles() == null ? List.of() : request.roles());
        jdbc.update("UPDATE platform_user SET token_version=token_version+1,updated_at=? WHERE id=?", LocalDateTime.now(), id);
        return response(requireUser(id));
    }

    @Transactional
    public UserResponse updateEnabled(Long id, boolean enabled) {
        if (jdbc.update("UPDATE platform_user SET enabled=?,token_version=token_version+1,updated_at=? WHERE id=?", enabled, LocalDateTime.now(), id) == 0) {
            throw new IllegalArgumentException("用户不存在");
        }
        return response(requireUser(id));
    }

    @Transactional
    public void resetPassword(Long id, String password) {
        if (jdbc.update("UPDATE platform_user SET password_hash=?,token_version=token_version+1,updated_at=? WHERE id=?",
                passwords.encode(password), LocalDateTime.now(), id) == 0) throw new IllegalArgumentException("用户不存在");
    }

    private void replaceRoles(Long userId, List<String> roles) {
        Set<String> distinct = new LinkedHashSet<>(roles);
        if (distinct.isEmpty()) throw new IllegalArgumentException("用户至少需要一个角色");
        for (String role : distinct) {
            if (count("SELECT COUNT(*) FROM platform_role WHERE code = ?", role) == 0) {
                throw new IllegalArgumentException("未知角色: " + role);
            }
        }
        jdbc.update("DELETE FROM platform_user_role WHERE user_id = ?", userId);
        distinct.forEach(role -> jdbc.update(
                "INSERT INTO platform_user_role(user_id,role_id) SELECT ?,id FROM platform_role WHERE code=?", userId, role));
    }

    private UserResponse response(UserRow user) {
        List<String> roles = jdbc.queryForList("SELECT r.code FROM platform_role r JOIN platform_user_role ur ON ur.role_id=r.id WHERE ur.user_id=? ORDER BY r.id",
                String.class, user.id());
        List<String> permissions = jdbc.queryForList(
                "SELECT p.code FROM platform_permission p JOIN platform_role_permission rp ON rp.permission_id=p.id " +
                        "JOIN platform_user_role ur ON ur.role_id=rp.role_id WHERE ur.user_id=? GROUP BY p.code ORDER BY MIN(p.id)",
                String.class, user.id());
        return new UserResponse(user.id(), user.username(), user.displayName(), user.enabled(), roles, permissions);
    }

    private List<String> permissionsForRole(String role) {
        return jdbc.queryForList("SELECT p.code FROM platform_permission p JOIN platform_role_permission rp ON rp.permission_id=p.id " +
                "JOIN platform_role r ON r.id=rp.role_id WHERE r.code=? ORDER BY p.id", String.class, role);
    }

    private UserRow findByUsername(String username) {
        List<UserRow> users = jdbc.query("SELECT id,username,password_hash,display_name,enabled,token_version FROM platform_user WHERE username=?",
                (rs, row) -> new UserRow(rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"),
                        rs.getString("display_name"), rs.getBoolean("enabled"), rs.getInt("token_version")), username);
        return users.isEmpty() ? null : users.getFirst();
    }
    private UserRow requireUser(Long id) {
        List<UserRow> users = jdbc.query("SELECT id,username,password_hash,display_name,enabled,token_version FROM platform_user WHERE id=?",
                (rs, row) -> new UserRow(rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"),
                        rs.getString("display_name"), rs.getBoolean("enabled"), rs.getInt("token_version")), id);
        if (users.isEmpty()) throw new IllegalArgumentException("用户不存在");
        return users.getFirst();
    }
    private int count(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
    private record UserRow(Long id, String username, String passwordHash, String displayName, boolean enabled,
                           int tokenVersion) {}
}
