package com.fintrack.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class CreateAccountRequest {

    @NotBlank(message = "Account name is required")
    @Size(max = 100, message = "Account name must not be longer than 100 characters")
    private String name;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3 letter")
    private String currency;

    private BigDecimal balance;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(String name, String currency, BigDecimal balance) {
        this.name = name;
        this.currency = currency;
        this.balance = balance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
