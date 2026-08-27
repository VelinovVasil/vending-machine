package com.vendingmachine.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PurchaseRequest(
        @NotNull @Positive Integer productId,
        @NotEmpty @Size(max = 5) List<@Valid CoinQuantityRequest> coins) {

    public PurchaseRequest {
        coins = coins == null ? null : List.copyOf(coins);
    }
}
