package com.fintrack.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.api.security.JwtAuthenticationFilter;
import com.fintrack.api.security.SecurityConfig;
import com.fintrack.api.service.UserService;
import com.fintrack.core.dto.AuthResponse;
import com.fintrack.core.dto.LoginRequest;
import com.fintrack.core.dto.RegisterRequest;
import com.fintrack.core.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should return 201 with token on successful registration")
        void shouldReturn201OnSuccessfulRegister() throws Exception {
            RegisterRequest request = new RegisterRequest("test@example.com", "password123");
            AuthResponse response = new AuthResponse("jwt_token", "test@example.com");

            when(userService.register(any(RegisterRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("jwt_token"))
                    .andExpect(jsonPath("$.email").value("test@example.com"));
        }

        @Test
        @DisplayName("Should return 400 when email is blank")
        void shouldReturn400WhenEmailBlank() throws Exception {
            RegisterRequest request = new RegisterRequest("", "password123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.email").exists());
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            RegisterRequest request = new RegisterRequest("not-an-email", "password123");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.email").exists());
        }

        @Test
        @DisplayName("Should return 400 when password is too short")
        void shouldReturn400WhenPasswordTooShort() throws Exception {
            RegisterRequest request = new RegisterRequest("test@example.com", "short");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.password").exists());
        }

        @Test
        @DisplayName("Should return 400 when email already exists")
        void shouldReturn400WhenEmailAlreadyExists() throws Exception {
            RegisterRequest request = new RegisterRequest("existing@example.com", "password123");

            when(userService.register(any(RegisterRequest.class)))
                    .thenThrow(new IllegalArgumentException("This email is already registered: existing@example.com"));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("This email is already registered: existing@example.com"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should return 200 with token on successful login")
        void shouldReturn200OnSuccessfulLogin() throws Exception {
            LoginRequest request = new LoginRequest("user@example.com", "password123");
            AuthResponse response = new AuthResponse("jwt_token", "user@example.com");

            when(userService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt_token"))
                    .andExpect(jsonPath("$.email").value("user@example.com"));
        }

        @Test
        @DisplayName("Should return 400 when credentials are invalid")
        void shouldReturn400WhenCredentialsInvalid() throws Exception {
            LoginRequest request = new LoginRequest("user@example.com", "wrong_password");

            when(userService.login(any(LoginRequest.class)))
                    .thenThrow(new IllegalArgumentException("Invalid email or password"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid email or password"));
        }

        @Test
        @DisplayName("Should return 400 when email is blank")
        void shouldReturn400WhenLoginEmailBlank() throws Exception {
            LoginRequest request = new LoginRequest("", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.email").exists());
        }

        @Test
        @DisplayName("Should return 400 when password is blank")
        void shouldReturn400WhenLoginPasswordBlank() throws Exception {
            LoginRequest request = new LoginRequest("user@example.com", "");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.password").exists());
        }
    }
}
