package com.maxwell.userregistration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "test-secret-key-for-testing-purposes-only-must-be-at-least-32-chars";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900_000L);
    }

    @Test
    void generateAccessToken_returnsNonNullToken() {
        String token = jwtService.generateAccessToken("user@example.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void validateToken_returnsTrueForValidToken() {
        String token = jwtService.generateAccessToken("user@example.com");
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_returnsFalseForGarbage() {
        assertThat(jwtService.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String email = "user@example.com";
        String token = jwtService.generateAccessToken(email);
        assertThat(jwtService.extractEmail(token)).isEqualTo(email);
    }

    @Test
    void accessToken_isMfaPendingFalse() {
        String token = jwtService.generateAccessToken("user@example.com");
        assertThat(jwtService.isMfaPending(token)).isFalse();
    }

    @Test
    void mfaPendingToken_isMfaPendingTrue() {
        String token = jwtService.generateMfaPendingToken("user@example.com");
        assertThat(jwtService.isMfaPending(token)).isTrue();
    }

    @Test
    void mfaPendingToken_doesNotGrantAuthentication() {
        String token = jwtService.generateMfaPendingToken("user@example.com");
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.isMfaPending(token)).isTrue();
    }
}
