package com.vendingmachine.client;

import com.vendingmachine.dto.ExternalProductDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class ProductCatalogClient {

    private final RestClient restClient;

    public ProductCatalogClient(
            RestClient.Builder restClientBuilder,
            @Value("${vending.external-api.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public List<ExternalProductDto> fetchProducts() {
        ExternalProductDto[] products = restClient.get()
                .uri("/products")
                .retrieve()
                .body(ExternalProductDto[].class);

        if (Objects.isNull(products)) {
            throw new IllegalStateException("External products API returned no response body");
        }

        return List.copyOf(Arrays.asList(products));
    }
}
