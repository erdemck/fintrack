package com.fintrack.core.dto;

import jakarta.validation.constraints.Size;

public class UpdateAccountRequest {

    @Size(max = 100, message = "Account name must not be longer than 100 characters")
    private String name;

    @Size(min = 3, max = 3, message = "Currency must be a 3 letter")
    private String currency;

    public UpdateAccountRequest() {
    }

    public UpdateAccountRequest(String name, String currency) {
        this.name = name;
        this.currency = currency;
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
}
