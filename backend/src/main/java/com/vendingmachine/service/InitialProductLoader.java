package com.vendingmachine.service;

import com.vendingmachine.client.ProductCatalogClient;
import com.vendingmachine.dto.ExternalProductDto;
import com.vendingmachine.model.Product;
import com.vendingmachine.repository.InMemoryProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialProductLoader implements ApplicationRunner {

    private final ProductCatalogClient productCatalogClient;
    private final InMemoryProductRepository productRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<Product> products = productCatalogClient.fetchProducts().stream()
                .map(this::toProduct)
                .toList();

        productRepository.replaceAll(products);
        log.info("Loaded {} products from the external products API", products.size());
    }

    private Product toProduct(ExternalProductDto product) {
        return new Product(product.id(), product.name(), product.price(), product.quantity());
    }
}
