package com.vendingmachine.service;

import com.vendingmachine.client.ProductCatalogClient;
import com.vendingmachine.model.Product;
import com.vendingmachine.repository.InMemoryProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InitialProductLoaderTest {

    private static final String BASE_URL = "http://localhost:3001";

    @Test
    void fetchesProductsAndLoadsThemIntoApplicationState() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        InMemoryProductRepository repository = new InMemoryProductRepository();
        InitialProductLoader loader = new InitialProductLoader(
                new ProductCatalogClient(restClientBuilder, BASE_URL), repository);

        server.expect(once(), requestTo(BASE_URL + "/products"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {"id": 1, "name": "Coke", "price": 150, "quantity": 10},
                          {"id": 2, "name": "Water", "price": 100, "quantity": 15}
                        ]
                        """, MediaType.APPLICATION_JSON));

        loader.run(null);

        assertThat(repository.findActivePage(0, 10).content())
                .containsExactly(
                        new Product(1, "Coke", 150, 10, false),
                        new Product(2, "Water", 100, 15, false));
        server.verify();
    }

    @Test
    void failsWithoutChangingApplicationStateWhenTheExternalApiFails() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        InMemoryProductRepository repository = new InMemoryProductRepository();
        InitialProductLoader loader = new InitialProductLoader(
                new ProductCatalogClient(restClientBuilder, BASE_URL), repository);

        server.expect(once(), requestTo(BASE_URL + "/products"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> loader.run(null))
                .isInstanceOf(RestClientResponseException.class);
        assertThat(repository.findActivePage(0, 10).content()).isEmpty();
        server.verify();
    }
}
