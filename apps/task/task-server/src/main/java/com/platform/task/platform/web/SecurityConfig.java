package com.platform.task.platform.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain taskSecurity(HttpSecurity http, JwtAuthenticationConverter converter,
                                     BearerTokenResolver bearerTokenResolver) throws Exception {
        return http.cors(Customizer.withDefaults()).csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/tasks/*/enabled").hasAuthority("task:manage")
                        .requestMatchers(HttpMethod.POST, "/api/v1/tasks").hasAuthority("task:manage")
                        .requestMatchers(HttpMethod.POST, "/api/v1/merchants").hasAuthority("task:manage")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/merchants/*/configuration").hasAuthority("task:manage")
                        .requestMatchers(HttpMethod.GET, "/api/v1/merchants/*/configuration").hasAuthority("task:manage")
                        .requestMatchers(HttpMethod.POST, "/api/v1/tasks/*/executions").hasAuthority("task:execute")
                        .requestMatchers(HttpMethod.POST, "/api/v1/executions/*/query-now").hasAuthority("task:execute")
                        .requestMatchers(HttpMethod.POST, "/api/v1/uploads/images").hasAuthority("task:execute")
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAuthority("task:read")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resource -> resource.bearerTokenResolver(bearerTokenResolver)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }

    @Bean
    NimbusJwtDecoder taskJwtDecoder(@Value("${platform.security.jwt-secret}") String encodedSecret,
                                    JdbcTemplate jdbcTemplate) {
        byte[] bytes = Base64.getDecoder().decode(encodedSecret);
        if (bytes.length < 32) throw new IllegalStateException("PLATFORM_JWT_SECRET must decode to at least 32 bytes");
        SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> revocation = token -> {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM platform_revoked_token WHERE jti = ?", Integer.class, token.getId());
            if (count != null && count > 0) return invalid("Token has been revoked");
            Number userId = token.getClaim("uid"), version = token.getClaim("ver");
            Integer active = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM platform_user WHERE id=? AND enabled=TRUE AND token_version=?",
                    Integer.class, userId, version);
            return active != null && active > 0 ? OAuth2TokenValidatorResult.success() : invalid("User or permission version is no longer active");
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("platform"), revocation));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter taskJwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("permissions");
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    private static OAuth2TokenValidatorResult invalid(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }

    @Bean
    BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver header = new DefaultBearerTokenResolver();
        return request -> {
            String token = header.resolve(request);
            if (token != null || request.getCookies() == null) return token;
            for (var cookie : request.getCookies()) if ("PLATFORM_TOKEN".equals(cookie.getName())) return cookie.getValue();
            return null;
        };
    }
}
