package com.fintrack.api.service;

import com.fintrack.core.dto.AuthResponse;
import com.fintrack.core.dto.LoginRequest;
import com.fintrack.core.dto.RegisterRequest;
import com.fintrack.core.security.JwtService;
import com.fintrack.persistence.entity.UserEntity;
import com.fintrack.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("Register")
    class RegisterTests {

        @Test
        @DisplayName("Should register a new user and return token")
        void shouldRegisterNewUser() {
            RegisterRequest request = new RegisterRequest("test@example.com", "password123");

            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed_password");

            UserEntity savedUser = new UserEntity();
            savedUser.setEmail("test@example.com");
            savedUser.setPasswordHash("hashed_password");
            when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

            when(jwtService.generateToken(any(), eq("test@example.com"))).thenReturn("jwt_token");

            AuthResponse response = userService.register(request);

            assertNotNull(response);
            assertEquals("jwt_token", response.getToken());
            assertEquals("test@example.com", response.getEmail());
        }

        @Test
        @DisplayName("Should hash the password before saving")
        void shouldHashPasswordBeforeSaving() {
            RegisterRequest request = new RegisterRequest("test@example.com", "password123");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("bcrypt_hashed");

            UserEntity savedUser = new UserEntity();
            savedUser.setEmail("test@example.com");
            when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any(), anyString())).thenReturn("token");

            userService.register(request);

            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(userCaptor.capture());

            assertEquals("bcrypt_hashed", userCaptor.getValue().getPasswordHash());
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            RegisterRequest request = new RegisterRequest("existing@example.com", "password123");

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.register(request)
            );

            assertTrue(exception.getMessage().contains("existing@example.com"));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        private UserEntity existingUser;

        @BeforeEach
        void setUp() {
            existingUser = new UserEntity();
            existingUser.setEmail("user@example.com");
            existingUser.setPasswordHash("hashed_password");
        }

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginWithValidCredentials() {
            LoginRequest request = new LoginRequest("user@example.com", "correct_password");

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("correct_password", "hashed_password")).thenReturn(true);
            when(jwtService.generateToken(any(), eq("user@example.com"))).thenReturn("jwt_token");

            AuthResponse response = userService.login(request);

            assertNotNull(response);
            assertEquals("jwt_token", response.getToken());
            assertEquals("user@example.com", response.getEmail());
        }

        @Test
        @DisplayName("Should throw exception when email not found")
        void shouldThrowExceptionWhenEmailNotFound() {
            LoginRequest request = new LoginRequest("nonexistent@example.com", "password");

            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.login(request)
            );

            assertEquals("Invalid email or password", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when password is incorrect")
        void shouldThrowExceptionWhenPasswordIncorrect() {
            LoginRequest request = new LoginRequest("user@example.com", "wrong_password");

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.login(request)
            );

            assertEquals("Invalid email or password", exception.getMessage());
            verify(jwtService, never()).generateToken(any(), anyString());
        }
    }
}
