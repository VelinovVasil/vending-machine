package com.vendingmachine.repository;

import com.vendingmachine.model.Product;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryProductRepositoryTest {

    private final InMemoryProductRepository repository = new InMemoryProductRepository();

    @Test
    void replacesTheCatalogWithAnImmutableMapIndexedByProductId() {
        Product coke = new Product(1, "Coke", 150, 10);
        Product water = new Product(2, "Water", 100, 15);

        repository.replaceAll(List.of(coke, water));

        assertThat(repository.findAll())
                .containsOnlyKeys(1, 2)
                .containsEntry(1, coke)
                .containsEntry(2, water);
        assertThat(repository.findById(1)).contains(coke);
        assertThat(repository.findById(99)).isEmpty();
        assertThatThrownBy(() -> repository.findAll().put(3, new Product(3, "Juice", 200, 8)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsDuplicateProductIdsWithoutChangingTheExistingCatalog() {
        Product existing = new Product(1, "Coke", 150, 10);
        repository.replaceAll(List.of(existing));

        assertThatThrownBy(() -> repository.replaceAll(List.of(
                new Product(2, "Water", 100, 15),
                new Product(2, "Juice", 200, 8))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate product id: 2");

        assertThat(repository.findAll()).containsOnlyKeys(1).containsEntry(1, existing);
    }
}
