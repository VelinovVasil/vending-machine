package com.vendingmachine.service;

import com.vendingmachine.model.CoinDenomination;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeCalculatorTest {

    private final ChangeCalculator calculator = new ChangeCalculator();

    @Test
    void returnsNoCoinsWhenNoChangeIsDue() {
        assertThat(calculator.calculate(0, Map.of())).contains(Map.of());
    }

    @Test
    void findsExactChangeWhenGreedySelectionWouldFail() {
        Map<CoinDenomination, Integer> inventory = inventory(
                CoinDenomination.FIFTY_CENTS, 1,
                CoinDenomination.TWENTY_CENTS, 3);

        assertThat(calculator.calculate(60, inventory))
                .contains(Map.of(CoinDenomination.TWENTY_CENTS, 3));
    }

    @Test
    void returnsTheCombinationWithTheFewestCoins() {
        Map<CoinDenomination, Integer> inventory = inventory(
                CoinDenomination.ONE_EURO, 1,
                CoinDenomination.FIFTY_CENTS, 2,
                CoinDenomination.TWENTY_CENTS, 5);

        assertThat(calculator.calculate(100, inventory))
                .contains(Map.of(CoinDenomination.ONE_EURO, 1));
    }

    @Test
    void respectsBoundedInventoryAndRejectsImpossibleAmounts() {
        assertThat(calculator.calculate(
                60,
                inventory(CoinDenomination.FIFTY_CENTS, 1)))
                .isEmpty();
        assertThat(calculator.calculate(
                15,
                inventory(CoinDenomination.TEN_CENTS, 10)))
                .isEmpty();
    }

    private Map<CoinDenomination, Integer> inventory(Object... values) {
        EnumMap<CoinDenomination, Integer> inventory = new EnumMap<>(CoinDenomination.class);
        for (int index = 0; index < values.length; index += 2) {
            inventory.put((CoinDenomination) values[index], (Integer) values[index + 1]);
        }
        return inventory;
    }
}
