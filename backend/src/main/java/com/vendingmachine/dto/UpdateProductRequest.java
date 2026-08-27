package com.vendingmachine.dto;

import com.vendingmachine.validation.MultipleOf;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Positive @MultipleOf(10) Integer price,
        @NotNull @Min(0) @Max(15) Integer quantity) {
}
