package com.fintrack.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.api.exception.ResourceNotFoundException;
import com.fintrack.api.security.JwtAuthenticationFilter;
import com.fintrack.api.security.SecurityConfig;
import com.fintrack.api.service.AccountService;
import com.fintrack.core.dto.AccountResponse;
import com.fintrack.core.dto.CreateAccountRequest;
import com.fintrack.core.dto.UpdateAccountRequest;
import com.fintrack.core.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private JwtService jwtService;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "user@example.com";
    private static final String VALID_TOKEN = "valid.jwt.token";

    private void setupAuthentication() {
        when(jwtService.isTokenValid(VALID_TOKEN)).thenReturn(true);
        when(jwtService.getUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(jwtService.getEmail(VALID_TOKEN)).thenReturn(USER_EMAIL);
    }

    private AccountResponse sampleResponse() {
        return new AccountResponse(1L, "Checking", "USD", BigDecimal.valueOf(1000),
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Nested
    @DisplayName("POST /api/accounts")
    class CreateTests {

        @Test
        @DisplayName("Should return 201 on successful account creation")
        void shouldReturn201OnCreate() throws Exception {
            setupAuthentication();
            CreateAccountRequest request = new CreateAccountRequest("Checking", "USD", BigDecimal.valueOf(1000));
            AccountResponse response = sampleResponse();

            when(accountService.createAccount(eq(USER_ID), any(CreateAccountRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/accounts")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Checking"))
                    .andExpect(jsonPath("$.currency").value("USD"))
                    .andExpect(jsonPath("$.balance").value(1000));
        }

        @Test
        @DisplayName("Should return 400 when name is blank")
        void shouldReturn400WhenNameBlank() throws Exception {
            setupAuthentication();
            CreateAccountRequest request = new CreateAccountRequest("", "USD", BigDecimal.ZERO);

            mockMvc.perform(post("/api/accounts")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.name").exists());
        }

        @Test
        @DisplayName("Should return 400 when currency is invalid")
        void shouldReturn400WhenCurrencyInvalid() throws Exception {
            setupAuthentication();
            CreateAccountRequest request = new CreateAccountRequest("Checking", "US", BigDecimal.ZERO);

            mockMvc.perform(post("/api/accounts")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.currency").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/accounts")
    class ListTests {

        @Test
        @DisplayName("Should return 200 with list of accounts")
        void shouldReturn200WithAccounts() throws Exception {
            setupAuthentication();
            AccountResponse account1 = new AccountResponse(1L, "Checking", "USD", BigDecimal.valueOf(1000),
                    OffsetDateTime.now(), OffsetDateTime.now());
            AccountResponse account2 = new AccountResponse(2L, "Savings", "EUR", BigDecimal.valueOf(5000),
                    OffsetDateTime.now(), OffsetDateTime.now());

            when(accountService.getAccountsByUser(USER_ID)).thenReturn(List.of(account1, account2));

            mockMvc.perform(get("/api/accounts")
                            .header("Authorization", "Bearer " + VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("Checking"))
                    .andExpect(jsonPath("$[1].name").value("Savings"));
        }

        @Test
        @DisplayName("Should return 200 with empty list when no accounts exist")
        void shouldReturn200WithEmptyList() throws Exception {
            setupAuthentication();
            when(accountService.getAccountsByUser(USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/api/accounts")
                            .header("Authorization", "Bearer " + VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/accounts/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("Should return 200 with account details")
        void shouldReturn200WithAccount() throws Exception {
            setupAuthentication();
            AccountResponse response = sampleResponse();

            when(accountService.getAccountById(USER_ID, 1L)).thenReturn(response);

            mockMvc.perform(get("/api/accounts/1")
                            .header("Authorization", "Bearer " + VALID_TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Checking"));
        }

        @Test
        @DisplayName("Should return 404 when account not found")
        void shouldReturn404WhenNotFound() throws Exception {
            setupAuthentication();
            when(accountService.getAccountById(USER_ID, 99L))
                    .thenThrow(new ResourceNotFoundException("Account not found with id: 99"));

            mockMvc.perform(get("/api/accounts/99")
                            .header("Authorization", "Bearer " + VALID_TOKEN))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Account not found with id: 99"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/accounts/{id}")
    class UpdateTests {

        @Test
        @DisplayName("Should return 200 on successful update")
        void shouldReturn200OnUpdate() throws Exception {
            setupAuthentication();
            UpdateAccountRequest request = new UpdateAccountRequest("Savings", null);
            AccountResponse response = new AccountResponse(1L, "Savings", "USD", BigDecimal.valueOf(1000),
                    OffsetDateTime.now(), OffsetDateTime.now());

            when(accountService.updateAccount(eq(USER_ID), eq(1L), any(UpdateAccountRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(patch("/api/accounts/1")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Savings"));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent account")
        void shouldReturn404WhenUpdateNotFound() throws Exception {
            setupAuthentication();
            UpdateAccountRequest request = new UpdateAccountRequest("Savings", null);

            when(accountService.updateAccount(eq(USER_ID), eq(99L), any(UpdateAccountRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Account not found with id: 99"));

            mockMvc.perform(patch("/api/accounts/99")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Account not found with id: 99"));
        }

        @Test
        @DisplayName("Should return 400 when currency format is invalid")
        void shouldReturn400WhenCurrencyInvalid() throws Exception {
            setupAuthentication();
            UpdateAccountRequest request = new UpdateAccountRequest(null, "ABCD");

            mockMvc.perform(patch("/api/accounts/1")
                            .header("Authorization", "Bearer " + VALID_TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.currency").exists());
        }
    }

    @Nested
    @DisplayName("DELETE /api/accounts/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Should return 204 on successful deletion")
        void shouldReturn204OnDelete() throws Exception {
            setupAuthentication();
            doNothing().when(accountService).deleteAccount(USER_ID, 1L);

            mockMvc.perform(delete("/api/accounts/1")
                            .header("Authorization", "Bearer " + VALID_TOKEN))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent account")
        void shouldReturn404WhenDeleteNotFound() throws Exception {
            setupAuthentication();
            doThrow(new ResourceNotFoundException("Account not found with id: 99"))
                    .when(accountService).deleteAccount(USER_ID, 99L);

            mockMvc.perform(delete("/api/accounts/99")
                            .header("Authorization", "Bearer " + VALID_TOKEN))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Account not found with id: 99"));
        }
    }

    @Nested
    @DisplayName("Unauthenticated requests")
    class UnauthenticatedTests {

        @Test
        @DisplayName("Should return 403 when no token is provided")
        void shouldReturn403WithoutToken() throws Exception {
            mockMvc.perform(get("/api/accounts"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when token is invalid")
        void shouldReturn403WithInvalidToken() throws Exception {
            when(jwtService.isTokenValid("invalid.token")).thenReturn(false);

            mockMvc.perform(get("/api/accounts")
                            .header("Authorization", "Bearer invalid.token"))
                    .andExpect(status().isForbidden());
        }
    }
}
