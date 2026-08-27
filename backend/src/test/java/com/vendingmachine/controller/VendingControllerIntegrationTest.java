package com.vendingmachine.controller;

import com.vendingmachine.config.CoinInventoryProperties;
import com.vendingmachine.exception.ApiExceptionHandler;
import com.vendingmachine.model.CoinDenomination;
import com.vendingmachine.model.Product;
import com.vendingmachine.repository.InMemoryCoinInventoryRepository;
import com.vendingmachine.repository.InMemoryProductRepository;
import com.vendingmachine.service.ChangeCalculator;
import com.vendingmachine.service.VendingMachineStateCoordinator;
import com.vendingmachine.service.VendingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VendingController.class)
@Import({
        VendingServiceImpl.class,
        ChangeCalculator.class,
        VendingMachineStateCoordinator.class,
        InMemoryProductRepository.class,
        InMemoryCoinInventoryRepository.class,
        ApiExceptionHandler.class,
        VendingControllerIntegrationTest.CoinTestConfiguration.class
})
class VendingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryProductRepository productRepository;

    @Autowired
    private InMemoryCoinInventoryRepository coinRepository;

    @BeforeEach
    void setUp() {
        productRepository.replaceAll(List.of(new Product(1, "Coke", 150, 2, false)));
        coinRepository.replaceAll(emptyInventory());
    }

    @Test
    void returnsAcceptedEuroDenominations() throws Exception {
        mockMvc.perform(get("/api/vending/denominations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.denominations[0]").value(10))
                .andExpect(jsonPath("$.denominations[4]").value(200));
    }

    @Test
    void purchasesAProductAndReturnsExactChange() throws Exception {
        mockMvc.perform(post("/api/vending/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 1,
                                  "coins": [
                                    {"denomination": 100, "quantity": 1},
                                    {"denomination": 50, "quantity": 2}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.id").value(1))
                .andExpect(jsonPath("$.product.quantity").value(1))
                .andExpect(jsonPath("$.insertedAmount").value(200))
                .andExpect(jsonPath("$.changeAmount").value(50))
                .andExpect(jsonPath("$.change[0].denomination").value(50))
                .andExpect(jsonPath("$.change[0].quantity").value(1));
    }

    @Test
    void returnsStructuredDeclineAndRefundWhenChangeIsUnavailable() throws Exception {
        mockMvc.perform(post("/api/vending/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 1,
                                  "coins": [{"denomination": 200, "quantity": 1}]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Purchase declined"))
                .andExpect(jsonPath("$.errorCode").value("EXACT_CHANGE_UNAVAILABLE"))
                .andExpect(jsonPath("$.changeDue").value(50))
                .andExpect(jsonPath("$.returnedCoins[0].denomination").value(200))
                .andExpect(jsonPath("$.returnedCoins[0].quantity").value(1));

        assertThat(productRepository.findById(1).orElseThrow().quantity()).isEqualTo(2);
        assertThat(coinRepository.snapshot().values()).containsOnly(0);
    }

    @Test
    void validatesCoinBodiesAndSelections() throws Exception {
        mockMvc.perform(post("/api/vending/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":1,"coins":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_COIN_SELECTION"));

        mockMvc.perform(post("/api/vending/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 1,
                                  "coins": [{"denomination": 5, "quantity": 1}]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_COIN_SELECTION"))
                .andExpect(jsonPath("$.returnedCoins[0].denomination").value(5));
    }

    private Map<CoinDenomination, Integer> emptyInventory() {
        EnumMap<CoinDenomination, Integer> inventory = new EnumMap<>(CoinDenomination.class);
        for (CoinDenomination denomination : CoinDenomination.values()) {
            inventory.put(denomination, 0);
        }
        return inventory;
    }

    @TestConfiguration
    static class CoinTestConfiguration {

        @Bean
        CoinInventoryProperties coinInventoryProperties() {
            return new CoinInventoryProperties(Map.of(
                    10, 0,
                    20, 0,
                    50, 0,
                    100, 0,
                    200, 0));
        }
    }
}
