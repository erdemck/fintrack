package com.fintrack.api.service;

import com.fintrack.api.exception.ResourceNotFoundException;
import com.fintrack.core.dto.AccountResponse;
import com.fintrack.core.dto.CreateAccountRequest;
import com.fintrack.core.dto.UpdateAccountRequest;
import com.fintrack.persistence.entity.AccountEntity;
import com.fintrack.persistence.entity.UserEntity;
import com.fintrack.persistence.repository.AccountRepository;
import com.fintrack.persistence.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long ACCOUNT_ID = 10L;

    private UserEntity createUser(Long id) {
        UserEntity user = new UserEntity();
        user.setEmail("user" + id + "@example.com");
        // Use reflection or a test-friendly approach to set the ID
        try {
            var idField = UserEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    private AccountEntity createAccount(Long accountId, UserEntity user) {
        AccountEntity account = new AccountEntity();
        account.setUser(user);
        account.setName("Checking");
        account.setCurrency("USD");
        account.setBalance(BigDecimal.valueOf(1000));
        try {
            var idField = AccountEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(account, accountId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return account;
    }

    @Nested
    @DisplayName("createAccount")
    class CreateAccountTests {

        @Test
        @DisplayName("Should create account successfully")
        void shouldCreateAccount() {
            UserEntity user = createUser(USER_ID);
            CreateAccountRequest request = new CreateAccountRequest("Checking", "USD", BigDecimal.valueOf(500));

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> {
                AccountEntity saved = invocation.getArgument(0);
                try {
                    var idField = AccountEntity.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(saved, ACCOUNT_ID);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return saved;
            });

            AccountResponse response = accountService.createAccount(USER_ID, request);

            assertThat(response.getId()).isEqualTo(ACCOUNT_ID);
            assertThat(response.getName()).isEqualTo("Checking");
            assertThat(response.getCurrency()).isEqualTo("USD");
            assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(500));
            verify(accountRepository).save(any(AccountEntity.class));
        }

        @Test
        @DisplayName("Should default balance to zero when not provided")
        void shouldDefaultBalanceToZero() {
            UserEntity user = createUser(USER_ID);
            CreateAccountRequest request = new CreateAccountRequest("Savings", "EUR", null);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            AccountResponse response = accountService.createAccount(USER_ID, request);

            assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            CreateAccountRequest request = new CreateAccountRequest("Checking", "USD", BigDecimal.ZERO);

            assertThatThrownBy(() -> accountService.createAccount(USER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("getAccountsByUser")
    class GetAccountsByUserTests {

        @Test
        @DisplayName("Should return all accounts for user")
        void shouldReturnAllAccounts() {
            UserEntity user = createUser(USER_ID);
            AccountEntity account1 = createAccount(1L, user);
            AccountEntity account2 = createAccount(2L, user);
            account2.setName("Savings");

            when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account1, account2));

            List<AccountResponse> responses = accountService.getAccountsByUser(USER_ID);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getName()).isEqualTo("Checking");
            assertThat(responses.get(1).getName()).isEqualTo("Savings");
        }

        @Test
        @DisplayName("Should return empty list when no accounts exist")
        void shouldReturnEmptyList() {
            when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of());

            List<AccountResponse> responses = accountService.getAccountsByUser(USER_ID);

            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAccountById")
    class GetAccountByIdTests {

        @Test
        @DisplayName("Should return account when owned by user")
        void shouldReturnAccount() {
            UserEntity user = createUser(USER_ID);
            AccountEntity account = createAccount(ACCOUNT_ID, user);

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            AccountResponse response = accountService.getAccountById(USER_ID, ACCOUNT_ID);

            assertThat(response.getId()).isEqualTo(ACCOUNT_ID);
            assertThat(response.getName()).isEqualTo("Checking");
        }

        @Test
        @DisplayName("Should throw when account not found")
        void shouldThrowWhenNotFound() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getAccountById(USER_ID, ACCOUNT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found");
        }

        @Test
        @DisplayName("Should throw when account not owned by user")
        void shouldThrowWhenNotOwned() {
            UserEntity otherUser = createUser(OTHER_USER_ID);
            AccountEntity account = createAccount(ACCOUNT_ID, otherUser);

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            assertThatThrownBy(() -> accountService.getAccountById(USER_ID, ACCOUNT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found");
        }
    }

    @Nested
    @DisplayName("updateAccount")
    class UpdateAccountTests {

        @Test
        @DisplayName("Should update name only when currency is null")
        void shouldUpdateNameOnly() {
            UserEntity user = createUser(USER_ID);
            AccountEntity account = createAccount(ACCOUNT_ID, user);
            UpdateAccountRequest request = new UpdateAccountRequest("Savings", null);

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            AccountResponse response = accountService.updateAccount(USER_ID, ACCOUNT_ID, request);

            assertThat(response.getName()).isEqualTo("Savings");
            assertThat(response.getCurrency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Should update currency only when name is null")
        void shouldUpdateCurrencyOnly() {
            UserEntity user = createUser(USER_ID);
            AccountEntity account = createAccount(ACCOUNT_ID, user);
            UpdateAccountRequest request = new UpdateAccountRequest(null, "EUR");

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            AccountResponse response = accountService.updateAccount(USER_ID, ACCOUNT_ID, request);

            assertThat(response.getName()).isEqualTo("Checking");
            assertThat(response.getCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("Should throw when updating non-existent account")
        void shouldThrowWhenNotFound() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            UpdateAccountRequest request = new UpdateAccountRequest("Savings", null);

            assertThatThrownBy(() -> accountService.updateAccount(USER_ID, ACCOUNT_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw when updating account not owned by user")
        void shouldThrowWhenNotOwned() {
            UserEntity otherUser = createUser(OTHER_USER_ID);
            AccountEntity account = createAccount(ACCOUNT_ID, otherUser);
            UpdateAccountRequest request = new UpdateAccountRequest("Savings", null);

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            assertThatThrownBy(() -> accountService.updateAccount(USER_ID, ACCOUNT_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccountTests {

        @Test
        @DisplayName("Should delete account successfully")
        void shouldDeleteAccount() {
            UserEntity user = createUser(USER_ID);
            AccountEntity account = createAccount(ACCOUNT_ID, user);

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            accountService.deleteAccount(USER_ID, ACCOUNT_ID);

            verify(accountRepository).delete(account);
        }

        @Test
        @DisplayName("Should throw when deleting non-existent account")
        void shouldThrowWhenNotFound() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.deleteAccount(USER_ID, ACCOUNT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw when deleting account not owned by user")
        void shouldThrowWhenNotOwned() {
            UserEntity otherUser = createUser(OTHER_USER_ID);
            AccountEntity account = createAccount(ACCOUNT_ID, otherUser);

            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            assertThatThrownBy(() -> accountService.deleteAccount(USER_ID, ACCOUNT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
