package com.vendingmachine.repository;

import com.vendingmachine.config.CoinInventoryProperties;
import com.vendingmachine.model.CoinDenomination;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryCoinInventoryRepositoryTest {

    @Test
    void loadsEveryConfiguredDenominationAndReturnsAnImmutableSnapshot() {
        InMemoryCoinInventoryRepository repository = new InMemoryCoinInventoryRepository(
                new CoinInventoryProperties(configuredInventory(10)));

        Map<CoinDenomination, Integer> snapshot = repository.snapshot();

        assertThat(snapshot).hasSize(5);
        assertThat(snapshot.values()).containsOnly(10);
        assertThatThrownBy(() -> snapshot.put(CoinDenomination.TEN_CENTS, 0))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsMissingOrUnsupportedConfiguration() {
        assertThatThrownBy(() -> new InMemoryCoinInventoryRepository(
                new CoinInventoryProperties(Map.of(10, 10))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing initial inventory");

        assertThatThrownBy(() -> new InMemoryCoinInventoryRepository(
                new CoinInventoryProperties(Map.of(
                        10, 10,
                        20, 10,
                        50, 10,
                        100, 10,
                        200, 10,
                        500, 1))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unsupported configured coin denomination: 500");
    }

    private Map<Integer, Integer> configuredInventory(int quantity) {
        return Map.of(
                10, quantity,
                20, quantity,
                50, quantity,
                100, quantity,
                200, quantity);
    }
}
