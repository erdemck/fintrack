package com.fintrack.core.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    // 64-character secret for HMAC-SHA (minimum 256 bits)
    private static final String TEST_SECRET = "4f7a2b9c8e1d3f6a5b0c7d8e9f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a";
    private static final long EXPIRATION_MS = 86400000; // 24 hours

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("Should generate a non-null token")
    void shouldGenerateToken() {
        String token = jwtService.generateToken(1L, "test@example.com");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("Should validate a correctly generated token")
    void shouldValidateGeneratedToken() {
        String token = jwtService.generateToken(1L, "test@example.com");

        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    @DisplayName("Should extract correct userId from token")
    void shouldExtractUserId() {
        Long expectedUserId = 42L;
        String token = jwtService.generateToken(expectedUserId, "test@example.com");

        Long actualUserId = jwtService.getUserId(token);

        assertEquals(expectedUserId, actualUserId);
    }

    @Test
    @DisplayName("Should extract correct email from token")
    void shouldExtractEmail() {
        String expectedEmail = "user@fintrack.com";
        String token = jwtService.generateToken(1L, expectedEmail);

        String actualEmail = jwtService.getEmail(token);

        assertEquals(expectedEmail, actualEmail);
    }

    @Test
    @DisplayName("Should return false for a tampered token")
    void shouldRejectTamperedToken() {
        String token = jwtService.generateToken(1L, "test@example.com");
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        assertFalse(jwtService.isTokenValid(tamperedToken));
    }

    @Test
    @DisplayName("Should return false for a completely invalid token")
    void shouldRejectGarbageToken() {
        assertFalse(jwtService.isTokenValid("not.a.valid.token"));
    }

    @Test
    @DisplayName("Should return false for null token")
    void shouldRejectNullToken() {
        assertFalse(jwtService.isTokenValid(null));
    }

    @Test
    @DisplayName("Should return false for an empty token")
    void shouldRejectEmptyToken() {
        assertFalse(jwtService.isTokenValid(""));
    }

    @Test
    @DisplayName("Should reject a token signed with a different secret")
    void shouldRejectTokenWithDifferentSecret() {
        String differentSecret = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        SecretKey differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));

        String tokenFromDifferentKey = Jwts.builder()
                .subject("1")
                .claim("email", "test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(differentKey)
                .compact();

        assertFalse(jwtService.isTokenValid(tokenFromDifferentKey));
    }

    @Test
    @DisplayName("Should reject an expired token")
    void shouldRejectExpiredToken() {
        // Create a JwtService with 0ms expiration so token is immediately expired
        JwtService shortLivedService = new JwtService(TEST_SECRET, 0);
        String expiredToken = shortLivedService.generateToken(1L, "test@example.com");

        assertFalse(jwtService.isTokenValid(expiredToken));
    }
}
