package com.treserve.auth.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void generateAccessToken_containsUserClaimsAndCanBeParsed() {
        JwtService jwtService = new JwtService(SECRET, 60_000L, 120_000L);

        String token = jwtService.generateAccessToken(42L, "user@example.com", "ADMIN");
        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(jwtService.getUserId(token)).isEqualTo(42L);
        assertThat(jwtService.getRole(token)).isEqualTo("ADMIN");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void generateRefreshToken_marksTokenAsRefresh() {
        JwtService jwtService = new JwtService(SECRET, 60_000L, 120_000L);

        String token = jwtService.generateRefreshToken(42L);
        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
        assertThat(claims.get("role", String.class)).isNull();
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isValid_whenMalformedToken_returnsFalse() {
        JwtService jwtService = new JwtService(SECRET, 60_000L, 120_000L);

        assertThat(jwtService.isValid("not-a-jwt")).isFalse();
    }
}
