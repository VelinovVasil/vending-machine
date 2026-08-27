package com.vendingmachine.dto;

import java.util.List;

public record PurchaseResponse(
        ProductResponse product,
        int insertedAmount,
        int changeAmount,
        List<CoinQuantityResponse> change) {

    public PurchaseResponse {
        change = List.copyOf(change);
    }
}
