package com.vendingmachine.service;

import com.vendingmachine.dto.CoinQuantityRequest;
import com.vendingmachine.dto.CoinQuantityResponse;
import com.vendingmachine.dto.ProductResponse;
import com.vendingmachine.dto.PurchaseRequest;
import com.vendingmachine.dto.PurchaseResponse;
import com.vendingmachine.dto.VendingConfigurationResponse;
import com.vendingmachine.exception.PurchaseDeclinedException;
import com.vendingmachine.model.CoinDenomination;
import com.vendingmachine.model.Product;
import com.vendingmachine.repository.InMemoryCoinInventoryRepository;
import com.vendingmachine.repository.InMemoryProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VendingServiceImpl implements VendingService {

    private static final int MAX_INSERTED_COINS = 100;

    private final InMemoryProductRepository productRepository;
    private final InMemoryCoinInventoryRepository coinInventoryRepository;
    private final ChangeCalculator changeCalculator;
    private final VendingMachineStateCoordinator stateCoordinator;

    @Override
    public VendingConfigurationResponse getConfiguration() {
        List<Integer> denominations = CoinDenomination.ascending().stream()
                .map(CoinDenomination::cents)
                .toList();
        return new VendingConfigurationResponse("EUR", denominations);
    }

    @Override
    public PurchaseResponse purchase(PurchaseRequest request) {
        Payment payment = normalizePayment(request);
        return stateCoordinator.write(() -> purchaseLocked(request.productId(), payment));
    }

    private PurchaseResponse purchaseLocked(int productId, Payment payment) {
        Product product = productRepository.findById(productId)
                .filter(candidate -> !candidate.deleted())
                .orElseThrow(() -> declined(
                        HttpStatus.NOT_FOUND,
                        "PRODUCT_NOT_FOUND",
                        "Product with id " + productId + " was not found",
                        payment.returnedCoins(),
                        Map.of()));

        if (product.quantity() == 0) {
            throw declined(
                    HttpStatus.CONFLICT,
                    "OUT_OF_STOCK",
                    "Product with id " + productId + " is out of stock",
                    payment.returnedCoins(),
                    Map.of("price", product.price(), "insertedAmount", payment.insertedAmount()));
        }

        if (payment.insertedAmount() < product.price()) {
            throw declined(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INSUFFICIENT_FUNDS",
                    "The inserted amount does not cover the product price",
                    payment.returnedCoins(),
                    Map.of(
                            "price", product.price(),
                            "insertedAmount", payment.insertedAmount(),
                            "shortfall", product.price() - payment.insertedAmount()));
        }

        int changeAmount = payment.insertedAmount() - product.price();
        EnumMap<CoinDenomination, Integer> tentativeInventory =
                new EnumMap<>(CoinDenomination.class);
        tentativeInventory.putAll(coinInventoryRepository.snapshot());
        payment.coins().forEach((denomination, quantity) -> tentativeInventory.merge(
                denomination, quantity, Math::addExact));

        Map<CoinDenomination, Integer> change = changeCalculator
                .calculate(changeAmount, tentativeInventory)
                .orElseThrow(() -> declined(
                        HttpStatus.CONFLICT,
                        "EXACT_CHANGE_UNAVAILABLE",
                        "The machine cannot return exact change",
                        payment.returnedCoins(),
                        Map.of(
                                "price", product.price(),
                                "insertedAmount", payment.insertedAmount(),
                                "changeDue", changeAmount)));

        change.forEach((denomination, quantity) -> tentativeInventory.compute(
                denomination, (ignored, available) -> available - quantity));

        Product updatedProduct = productRepository.decrementQuantity(productId)
                .orElseThrow(() -> new IllegalStateException(
                        "Product state changed while the vending state lock was held"));
        coinInventoryRepository.replaceAll(tentativeInventory);

        return new PurchaseResponse(
                toProductResponse(updatedProduct),
                payment.insertedAmount(),
                changeAmount,
                toCoinResponses(change));
    }

    private Payment normalizePayment(PurchaseRequest request) {
        if (request == null || request.productId() == null || request.productId() <= 0) {
            throw invalidCoinSelection("A positive productId is required", List.of());
        }
        if (request.coins() == null || request.coins().isEmpty() || request.coins().size() > 5) {
            throw invalidCoinSelection("Between one and five coin entries are required", rawCoins(request));
        }

        EnumMap<CoinDenomination, Integer> coins = new EnumMap<>(CoinDenomination.class);
        EnumSet<CoinDenomination> seenDenominations = EnumSet.noneOf(CoinDenomination.class);
        long totalCoinCount = 0;
        long insertedAmount = 0;

        for (CoinQuantityRequest coin : request.coins()) {
            if (coin == null || coin.denomination() == null || coin.quantity() == null || coin.quantity() <= 0) {
                throw invalidCoinSelection("Every coin entry requires a denomination and positive quantity", rawCoins(request));
            }

            CoinDenomination denomination = CoinDenomination.fromCents(coin.denomination())
                    .orElseThrow(() -> invalidCoinSelection(
                            "Unsupported coin denomination: " + coin.denomination(), rawCoins(request)));
            if (!seenDenominations.add(denomination)) {
                throw invalidCoinSelection(
                        "Duplicate coin denomination: " + coin.denomination(), rawCoins(request));
            }

            totalCoinCount += coin.quantity();
            insertedAmount += (long) denomination.cents() * coin.quantity();
            coins.put(denomination, coin.quantity());
        }

        if (totalCoinCount > MAX_INSERTED_COINS) {
            throw invalidCoinSelection(
                    "A purchase may contain at most " + MAX_INSERTED_COINS + " coins", rawCoins(request));
        }

        return new Payment(coins, Math.toIntExact(insertedAmount), toCoinResponses(coins));
    }

    private PurchaseDeclinedException invalidCoinSelection(
            String message,
            List<CoinQuantityResponse> returnedCoins) {
        return declined(
                HttpStatus.BAD_REQUEST,
                "INVALID_COIN_SELECTION",
                message,
                returnedCoins,
                Map.of());
    }

    private PurchaseDeclinedException declined(
            HttpStatus status,
            String errorCode,
            String message,
            List<CoinQuantityResponse> returnedCoins,
            Map<String, Object> properties) {
        return new PurchaseDeclinedException(
                status, errorCode, message, returnedCoins, properties);
    }

    private List<CoinQuantityResponse> rawCoins(PurchaseRequest request) {
        if (request == null || request.coins() == null) {
            return List.of();
        }

        List<CoinQuantityResponse> result = new ArrayList<>();
        for (CoinQuantityRequest coin : request.coins()) {
            if (coin != null && coin.denomination() != null && coin.quantity() != null) {
                result.add(new CoinQuantityResponse(coin.denomination(), coin.quantity()));
            }
        }
        result.sort(Comparator.comparingInt(CoinQuantityResponse::denomination).reversed());
        return List.copyOf(result);
    }

    private List<CoinQuantityResponse> toCoinResponses(Map<CoinDenomination, Integer> coins) {
        return CoinDenomination.descending().stream()
                .filter(coins::containsKey)
                .map(denomination -> new CoinQuantityResponse(
                        denomination.cents(), coins.get(denomination)))
                .toList();
    }

    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(product.id(), product.name(), product.price(), product.quantity());
    }

    private record Payment(
            Map<CoinDenomination, Integer> coins,
            int insertedAmount,
            List<CoinQuantityResponse> returnedCoins) {

        private Payment {
            coins = Map.copyOf(coins);
            returnedCoins = List.copyOf(returnedCoins);
        }
    }
}
