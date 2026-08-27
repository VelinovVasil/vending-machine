package com.vendingmachine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "vending.coins")
public record CoinInventoryProperties(Map<Integer, Integer> initialInventory) {

    public CoinInventoryProperties {
        initialInventory = initialInventory == null ? Map.of() : Map.copyOf(initialInventory);
    }
}
