package com.fintrack.api.controller;

import com.fintrack.api.BaseIntegrationTest;
import com.fintrack.core.dto.AuthResponse;
import com.fintrack.core.dto.LoginRequest;
import com.fintrack.core.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Auth Integration Test — register → login → protected endpoint")
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String TEST_EMAIL = "integration@test.com";
    private static final String TEST_PASSWORD = "securePassword123";

    private static String authToken;

    @Test
    @Order(1)
    @DisplayName("Should register a new user and return 201 with token")
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/register", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
        assertThat(response.getBody().getEmail()).isEqualTo(TEST_EMAIL);

        // Store token for subsequent tests
        authToken = response.getBody().getToken();
    }

    @Test
    @Order(2)
    @DisplayName("Should reject duplicate registration with 400")
    void shouldRejectDuplicateRegistration() {
        RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_PASSWORD);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/register", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(3)
    @DisplayName("Should login with valid credentials and return 200 with token")
    void shouldLoginWithValidCredentials() {
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/login", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
        assertThat(response.getBody().getEmail()).isEqualTo(TEST_EMAIL);

        // Update token from login response
        authToken = response.getBody().getToken();
    }

    @Test
    @Order(4)
    @DisplayName("Should access protected endpoint with valid token")
    void shouldAccessProtectedEndpointWithToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/accounts", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(5)
    @DisplayName("Should reject unauthenticated access to protected endpoint")
    void shouldRejectUnauthenticatedAccess() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/accounts", String.class);

        // Spring Security returns 403 by default for unauthenticated requests
        // when there is no custom AuthenticationEntryPoint configured
        assertThat(response.getStatusCode()).satisfiesAnyOf(
                status -> assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED),
                status -> assertThat(status).isEqualTo(HttpStatus.FORBIDDEN));
    }
}
