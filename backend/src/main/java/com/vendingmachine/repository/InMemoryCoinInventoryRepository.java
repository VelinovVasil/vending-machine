package com.vendingmachine.repository;

import com.vendingmachine.config.CoinInventoryProperties;
import com.vendingmachine.model.CoinDenomination;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.Map;

@Repository
public class InMemoryCoinInventoryRepository {

    private final EnumMap<CoinDenomination, Integer> inventory = new EnumMap<>(CoinDenomination.class);

    public InMemoryCoinInventoryRepository(CoinInventoryProperties properties) {
        replaceAll(toDenominationMap(properties.initialInventory()));
    }

    public synchronized Map<CoinDenomination, Integer> snapshot() {
        return Map.copyOf(inventory);
    }

    public synchronized void replaceAll(Map<CoinDenomination, Integer> replacement) {
        validateCompleteInventory(replacement);
        inventory.clear();
        inventory.putAll(replacement);
    }

    private EnumMap<CoinDenomination, Integer> toDenominationMap(Map<Integer, Integer> configuredInventory) {
        EnumMap<CoinDenomination, Integer> result = new EnumMap<>(CoinDenomination.class);

        for (Map.Entry<Integer, Integer> entry : configuredInventory.entrySet()) {
            CoinDenomination denomination = CoinDenomination.fromCents(entry.getKey())
                    .orElseThrow(() -> new IllegalStateException(
                            "Unsupported configured coin denomination: " + entry.getKey()));
            result.put(denomination, entry.getValue());
        }

        return result;
    }

    private void validateCompleteInventory(Map<CoinDenomination, Integer> candidate) {
        for (CoinDenomination denomination : CoinDenomination.values()) {
            Integer quantity = candidate.get(denomination);
            if (quantity == null) {
                throw new IllegalStateException(
                        "Missing initial inventory for denomination: " + denomination.cents());
            }
            if (quantity < 0) {
                throw new IllegalStateException(
                        "Coin inventory cannot be negative for denomination: " + denomination.cents());
            }
        }

        if (candidate.size() != CoinDenomination.values().length) {
            throw new IllegalStateException("Coin inventory contains unsupported denominations");
        }
    }
}
