package com.vendingmachine.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CoinQuantityRequest(
        @NotNull @Positive Integer denomination,
        @NotNull @Positive @Max(100) Integer quantity) {
}
