package com.vendingmachine.repository;

import com.vendingmachine.model.Product;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class InMemoryProductRepository {

    private volatile Map<Integer, Product> productsById = Map.of();

    public void replaceAll(Collection<Product> products) {
        Map<Integer, Product> replacement = new HashMap<>();

        for (Product product : products) {
            Product existing = replacement.putIfAbsent(product.id(), product);
            if (Objects.nonNull(existing)) {
                throw new IllegalArgumentException("Duplicate product id: " + product.id());
            }
        }

        productsById = Map.copyOf(replacement);
    }

    public Map<Integer, Product> findAll() {
        return productsById;
    }

    public Optional<Product> findById(int id) {
        return Optional.ofNullable(productsById.get(id));
    }
}
