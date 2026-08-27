package com.vendingmachine.dto;

import java.util.List;

public record VendingConfigurationResponse(String currency, List<Integer> denominations) {

    public VendingConfigurationResponse {
        denominations = List.copyOf(denominations);
    }
}
