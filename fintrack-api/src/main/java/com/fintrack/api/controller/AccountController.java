package com.fintrack.api.controller;

import com.fintrack.api.security.SecurityUtil;
import com.fintrack.api.service.AccountService;
import com.fintrack.core.dto.AccountResponse;
import com.fintrack.core.dto.CreateAccountRequest;
import com.fintrack.core.dto.UpdateAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        AccountResponse response = accountService.createAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAccounts() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<AccountResponse> accounts = accountService.getAccountsByUser(userId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        AccountResponse response = accountService.getAccountById(userId, id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateAccountRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        AccountResponse response = accountService.updateAccount(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        accountService.deleteAccount(userId, id);
        return ResponseEntity.noContent().build();
    }
}
