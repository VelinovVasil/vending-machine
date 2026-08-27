package com.vendingmachine.service;

import com.vendingmachine.config.CoinInventoryProperties;
import com.vendingmachine.dto.CoinQuantityRequest;
import com.vendingmachine.dto.CoinQuantityResponse;
import com.vendingmachine.dto.PurchaseRequest;
import com.vendingmachine.dto.PurchaseResponse;
import com.vendingmachine.exception.PurchaseDeclinedException;
import com.vendingmachine.model.CoinDenomination;
import com.vendingmachine.model.Product;
import com.vendingmachine.repository.InMemoryCoinInventoryRepository;
import com.vendingmachine.repository.InMemoryProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VendingServiceTest {

    private InMemoryProductRepository productRepository;
    private InMemoryCoinInventoryRepository coinRepository;
    private VendingService vendingService;

    @BeforeEach
    void setUp() {
        productRepository = new InMemoryProductRepository();
        coinRepository = new InMemoryCoinInventoryRepository(
                new CoinInventoryProperties(configuredInventory(0)));
        vendingService = new VendingServiceImpl(
                productRepository,
                coinRepository,
                new ChangeCalculator(),
                new VendingMachineStateCoordinator());
    }

    @Test
    void exposesAcceptedEuroDenominations() {
        assertThat(vendingService.getConfiguration().currency()).isEqualTo("EUR");
        assertThat(vendingService.getConfiguration().denominations())
                .containsExactly(10, 20, 50, 100, 200);
    }

    @Test
    void completesExactPaymentAndCommitsStockAndCoins() {
        productRepository.replaceAll(List.of(new Product(1, "Coke", 150, 2, false)));

        PurchaseResponse response = vendingService.purchase(request(
                1,
                coin(100, 1),
                coin(50, 1)));

        assertThat(response.product().quantity()).isEqualTo(1);
        assertThat(response.insertedAmount()).isEqualTo(150);
        assertThat(response.changeAmount()).isZero();
        assertThat(response.change()).isEmpty();
        assertThat(coinRepository.snapshot())
                .containsEntry(CoinDenomination.ONE_EURO, 1)
                .containsEntry(CoinDenomination.FIFTY_CENTS, 1);
    }

    @Test
    void canUseSubmittedCoinsForExactChange() {
        productRepository.replaceAll(List.of(new Product(1, "Coke", 150, 2, false)));

        PurchaseResponse response = vendingService.purchase(request(
                1,
                coin(100, 1),
                coin(50, 2)));

        assertThat(response.changeAmount()).isEqualTo(50);
        assertThat(response.change()).containsExactly(new CoinQuantityResponse(50, 1));
        assertThat(coinRepository.snapshot())
                .containsEntry(CoinDenomination.ONE_EURO, 1)
                .containsEntry(CoinDenomination.FIFTY_CENTS, 1);
    }

    @Test
    void declinesWithoutMutatingStateWhenExactChangeIsUnavailable() {
        Product product = new Product(1, "Coke", 150, 2, false);
        productRepository.replaceAll(List.of(product));
        Map<CoinDenomination, Integer> initialCoins = coinRepository.snapshot();

        assertThatThrownBy(() -> vendingService.purchase(request(1, coin(200, 1))))
                .isInstanceOfSatisfying(PurchaseDeclinedException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo("EXACT_CHANGE_UNAVAILABLE");
                    assertThat(exception.returnedCoins()).containsExactly(new CoinQuantityResponse(200, 1));
                    assertThat(exception.properties()).containsEntry("changeDue", 50);
                });

        assertThat(productRepository.findById(1)).contains(product);
        assertThat(coinRepository.snapshot()).isEqualTo(initialCoins);
    }

    @Test
    void returnsCoinsForMissingOutOfStockAndUnderpaidPurchases() {
        productRepository.replaceAll(List.of(
                new Product(1, "Coke", 150, 0, false),
                new Product(2, "Water", 100, 1, false)));

        assertDeclined(request(99, coin(200, 1)), "PRODUCT_NOT_FOUND");
        assertDeclined(request(1, coin(200, 1)), "OUT_OF_STOCK");
        assertDeclined(request(2, coin(50, 1)), "INSUFFICIENT_FUNDS");
        assertThat(productRepository.findById(2).orElseThrow().quantity()).isEqualTo(1);
        assertThat(coinRepository.snapshot().values()).containsOnly(0);
    }

    @Test
    void rejectsUnsupportedDuplicateAndExcessiveCoinSelections() {
        productRepository.replaceAll(List.of(new Product(1, "Coke", 150, 1, false)));

        assertDeclined(request(1, coin(5, 1)), "INVALID_COIN_SELECTION");
        assertDeclined(request(1, coin(100, 1), coin(100, 1)), "INVALID_COIN_SELECTION");
        assertDeclined(request(1, coin(10, 100), coin(20, 1)), "INVALID_COIN_SELECTION");
    }

    @Test
    void allowsOnlyOneConcurrentPurchaseOfTheLastProduct() throws Exception {
        productRepository.replaceAll(List.of(new Product(1, "Coke", 150, 1, false)));
        PurchaseRequest request = request(1, coin(100, 1), coin(50, 1));
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<String> first = executor.submit(() -> purchaseResultAfter(start, request));
            Future<String> second = executor.submit(() -> purchaseResultAfter(start, request));
            start.countDown();

            assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder("SUCCESS", "OUT_OF_STOCK");
        }

        assertThat(productRepository.findById(1).orElseThrow().quantity()).isZero();
    }

    private String purchaseResultAfter(CountDownLatch start, PurchaseRequest request) {
        try {
            start.await();
            vendingService.purchase(request);
            return "SUCCESS";
        } catch (PurchaseDeclinedException exception) {
            return exception.errorCode();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Purchase was interrupted", exception);
        }
    }

    private void assertDeclined(PurchaseRequest request, String errorCode) {
        assertThatThrownBy(() -> vendingService.purchase(request))
                .isInstanceOfSatisfying(PurchaseDeclinedException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(errorCode);
                    assertThat(exception.returnedCoins()).isNotEmpty();
                });
    }

    private PurchaseRequest request(int productId, CoinQuantityRequest... coins) {
        return new PurchaseRequest(productId, List.of(coins));
    }

    private CoinQuantityRequest coin(int denomination, int quantity) {
        return new CoinQuantityRequest(denomination, quantity);
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
