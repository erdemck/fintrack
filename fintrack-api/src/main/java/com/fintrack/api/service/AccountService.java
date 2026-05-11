package com.fintrack.api.service;

import com.fintrack.api.exception.ResourceNotFoundException;
import com.fintrack.core.dto.AccountResponse;
import com.fintrack.core.dto.CreateAccountRequest;
import com.fintrack.core.dto.UpdateAccountRequest;
import com.fintrack.persistence.entity.AccountEntity;
import com.fintrack.persistence.entity.UserEntity;
import com.fintrack.persistence.repository.AccountRepository;
import com.fintrack.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AccountResponse createAccount(Long userId, CreateAccountRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        AccountEntity account = new AccountEntity();
        account.setUser(user);
        account.setName(request.getName());
        account.setCurrency(request.getCurrency());
        account.setBalance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO);

        AccountEntity saved = accountRepository.save(account);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUser(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long userId, Long accountId) {
        AccountEntity account = findAccountOwnedByUser(userId, accountId);
        return toResponse(account);
    }

    @Transactional
    public AccountResponse updateAccount(Long userId, Long accountId, UpdateAccountRequest request) {
        AccountEntity account = findAccountOwnedByUser(userId, accountId);

        if (request.getName() != null) {
            account.setName(request.getName());
        }
        if (request.getCurrency() != null) {
            account.setCurrency(request.getCurrency());
        }

        AccountEntity updated = accountRepository.save(account);
        return toResponse(updated);
    }

    @Transactional
    public void deleteAccount(Long userId, Long accountId) {
        AccountEntity account = findAccountOwnedByUser(userId, accountId);
        accountRepository.delete(account);
    }

    private AccountEntity findAccountOwnedByUser(Long userId, Long accountId) {
        AccountEntity account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        if (!account.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Account not found with id: " + accountId);
        }

        return account;
    }

    private AccountResponse toResponse(AccountEntity entity) {
        return new AccountResponse(
                entity.getId(),
                entity.getName(),
                entity.getCurrency(),
                entity.getBalance(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
