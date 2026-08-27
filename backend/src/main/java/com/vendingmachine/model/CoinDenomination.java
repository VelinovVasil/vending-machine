package com.vendingmachine.model;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public enum CoinDenomination {
    TEN_CENTS(10),
    TWENTY_CENTS(20),
    FIFTY_CENTS(50),
    ONE_EURO(100),
    TWO_EUROS(200);

    private static final List<CoinDenomination> ASCENDING = Arrays.stream(values())
            .sorted(Comparator.comparingInt(CoinDenomination::cents))
            .toList();

    private static final List<CoinDenomination> DESCENDING = ASCENDING.reversed();

    private final int cents;

    CoinDenomination(int cents) {
        this.cents = cents;
    }

    public int cents() {
        return cents;
    }

    public static Optional<CoinDenomination> fromCents(int cents) {
        return ASCENDING.stream()
                .filter(denomination -> denomination.cents == cents)
                .findFirst();
    }

    public static List<CoinDenomination> ascending() {
        return ASCENDING;
    }

    public static List<CoinDenomination> descending() {
        return DESCENDING;
    }
}
