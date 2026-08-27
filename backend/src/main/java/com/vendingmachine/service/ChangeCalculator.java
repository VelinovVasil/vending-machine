package com.vendingmachine.service;

import com.vendingmachine.model.CoinDenomination;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ChangeCalculator {

    public Optional<Map<CoinDenomination, Integer>> calculate(
            int changeAmount,
            Map<CoinDenomination, Integer> availableCoins) {
        if (changeAmount < 0) {
            throw new IllegalArgumentException("Change amount cannot be negative");
        }
        if (changeAmount == 0) {
            return Optional.of(Map.of());
        }
        if (changeAmount % CoinDenomination.TEN_CENTS.cents() != 0) {
            return Optional.empty();
        }

        List<CoinDenomination> denominations = CoinDenomination.descending();
        Selection selection = findBestSelection(
                0, changeAmount, denominations, availableCoins, new HashMap<>());
        if (selection == null) {
            return Optional.empty();
        }

        EnumMap<CoinDenomination, Integer> change = new EnumMap<>(CoinDenomination.class);
        for (int index = 0; index < denominations.size(); index++) {
            if (selection.quantities()[index] > 0) {
                change.put(denominations.get(index), selection.quantities()[index]);
            }
        }
        return Optional.of(Map.copyOf(change));
    }

    private Selection findBestSelection(
            int denominationIndex,
            int remainingAmount,
            List<CoinDenomination> denominations,
            Map<CoinDenomination, Integer> availableCoins,
            Map<State, Selection> memo) {
        if (remainingAmount == 0) {
            return new Selection(0, new int[denominations.size()]);
        }
        if (denominationIndex == denominations.size()) {
            return null;
        }

        State state = new State(denominationIndex, remainingAmount);
        if (memo.containsKey(state)) {
            return memo.get(state);
        }

        CoinDenomination denomination = denominations.get(denominationIndex);
        int maximumQuantity = Math.min(
                availableCoins.getOrDefault(denomination, 0),
                remainingAmount / denomination.cents());
        Selection best = null;

        for (int quantity = maximumQuantity; quantity >= 0; quantity--) {
            int newRemainingAmount = remainingAmount - quantity * denomination.cents();
            Selection remainder = findBestSelection(
                    denominationIndex + 1,
                    newRemainingAmount,
                    denominations,
                    availableCoins,
                    memo);
            if (remainder == null) {
                continue;
            }

            int coinCount = quantity + remainder.coinCount();
            if (best == null || coinCount < best.coinCount()) {
                int[] quantities = remainder.quantities().clone();
                quantities[denominationIndex] = quantity;
                best = new Selection(coinCount, quantities);
            }
        }

        memo.put(state, best);
        return best;
    }

    private record State(int denominationIndex, int remainingAmount) {
    }

    private record Selection(int coinCount, int[] quantities) {
    }
}
