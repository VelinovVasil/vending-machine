package com.vendingmachine.repository;

import com.vendingmachine.model.PageResult;
import com.vendingmachine.model.Product;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

@Repository
public class InMemoryProductRepository {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PRODUCT_QUANTITY = 15;
    private static final int MINIMUM_PRICE_INCREMENT = 10;

    private final ConcurrentNavigableMap<Integer, Product> productsById = new ConcurrentSkipListMap<>();
    private int nextProductId = 1;
    private long activeProductCount;

    public synchronized void replaceAll(Collection<Product> products) {
        Map<Integer, Product> replacement = new HashMap<>();

        for (Product product : products) {
            validateProduct(product);
            Product existing = replacement.putIfAbsent(product.id(), product);
            if (Objects.nonNull(existing)) {
                throw new IllegalArgumentException("Duplicate product id: " + product.id());
            }
        }

        productsById.clear();
        productsById.putAll(replacement);
        activeProductCount = replacement.values().stream()
                .filter(product -> !product.deleted())
                .count();
        nextProductId = replacement.keySet().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    public synchronized PageResult<Product> findActivePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }

        long offset = (long) page * size;
        var content = new ArrayList<Product>(size);
        long activeIndex = 0;

        if (offset < activeProductCount) {
            for (Product product : productsById.values()) {
                if (product.deleted()) {
                    continue;
                }

                if (activeIndex++ < offset) {
                    continue;
                }

                content.add(product);
                if (content.size() == size) {
                    break;
                }
            }
        }

        int totalPages = activeProductCount == 0
                ? 0
                : (int) (((activeProductCount - 1) / size) + 1);
        return new PageResult<>(content, page, size, activeProductCount, totalPages);
    }

    public Optional<Product> findById(int id) {
        return Optional.ofNullable(productsById.get(id));
    }

    public synchronized Product create(String name, int price, int quantity) {
        int id = nextProductId++;
        Product product = new Product(id, name, price, quantity, false);
        validateProduct(product);
        productsById.put(id, product);
        activeProductCount++;
        return product;
    }

    public synchronized Optional<Product> update(int id, String name, int price, int quantity) {
        Product existing = productsById.get(id);
        if (Objects.isNull(existing) || existing.deleted()) {
            return Optional.empty();
        }

        Product updated = new Product(id, name, price, quantity, false);
        validateProduct(updated);
        productsById.put(id, updated);
        return Optional.of(updated);
    }

    public synchronized Optional<Product> softDelete(int id) {
        Product existing = productsById.get(id);
        if (Objects.isNull(existing) || existing.deleted()) {
            return Optional.empty();
        }

        Product deleted = new Product(
                existing.id(), existing.name(), existing.price(), existing.quantity(), true);
        productsById.put(id, deleted);
        activeProductCount--;
        return Optional.of(deleted);
    }

    public synchronized Optional<Product> decrementQuantity(int id) {
        Product existing = productsById.get(id);
        if (Objects.isNull(existing) || existing.deleted() || existing.quantity() == 0) {
            return Optional.empty();
        }

        Product updated = new Product(
                existing.id(),
                existing.name(),
                existing.price(),
                existing.quantity() - 1,
                false);
        productsById.put(id, updated);
        return Optional.of(updated);
    }

    private void validateProduct(Product product) {
        if (product.id() <= 0) {
            throw new IllegalArgumentException("Product id must be positive");
        }
        if (product.name() == null || product.name().isBlank() || product.name().length() > 100) {
            throw new IllegalArgumentException("Product name must contain between 1 and 100 characters");
        }
        if (product.price() <= 0 || product.price() % MINIMUM_PRICE_INCREMENT != 0) {
            throw new IllegalArgumentException("Product price must be a positive multiple of 10 cents");
        }
        if (product.quantity() < 0 || product.quantity() > MAX_PRODUCT_QUANTITY) {
            throw new IllegalArgumentException("Product quantity must be between 0 and 15");
        }
    }
}
