package com.vendingmachine.repository;

import com.vendingmachine.model.PageResult;
import com.vendingmachine.model.Product;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryProductRepositoryTest {

    private final InMemoryProductRepository repository = new InMemoryProductRepository();

    @Test
    void replacesTheCatalogAndReturnsAnImmutablePage() {
        Product coke = new Product(1, "Coke", 150, 10, false);
        Product water = new Product(2, "Water", 100, 15, false);

        repository.replaceAll(List.of(coke, water));

        PageResult<Product> page = repository.findActivePage(0, 10);

        assertThat(page.content()).containsExactly(coke, water);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(repository.findById(1)).contains(coke);
        assertThat(repository.findById(99)).isEmpty();
        assertThatThrownBy(() -> page.content().add(new Product(3, "Juice", 200, 8, false)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsDuplicateProductIdsWithoutChangingTheExistingCatalog() {
        Product existing = new Product(1, "Coke", 150, 10, false);
        repository.replaceAll(List.of(existing));

        assertThatThrownBy(() -> repository.replaceAll(List.of(
                new Product(2, "Water", 100, 15, false),
                new Product(2, "Juice", 200, 8, false))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate product id: 2");

        assertThat(repository.findActivePage(0, 10).content()).containsExactly(existing);
    }

    @Test
    void returnsOnlyTheRequestedActivePageInStableIdOrder() {
        repository.replaceAll(List.of(
                new Product(5, "Chocolate Bar", 250, 6, false),
                new Product(2, "Water", 100, 15, true),
                new Product(1, "Coke", 150, 10, false),
                new Product(3, "Orange Juice", 200, 8, false)));

        PageResult<Product> firstPage = repository.findActivePage(0, 2);
        PageResult<Product> secondPage = repository.findActivePage(1, 2);

        assertThat(firstPage.content()).extracting(Product::id).containsExactly(1, 3);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.content()).extracting(Product::id).containsExactly(5);
    }

    @Test
    void createsUpdatesAndSoftDeletesWithoutRemovingProductData() {
        repository.replaceAll(List.of(
                new Product(2, "Water", 100, 15, false),
                new Product(5, "Chocolate Bar", 250, 6, false)));

        Product created = repository.create("Juice", 200, 8);
        assertThat(created).isEqualTo(new Product(6, "Juice", 200, 8, false));

        Product updated = repository.update(6, "Orange Juice", 220, 7).orElseThrow();
        assertThat(updated).isEqualTo(new Product(6, "Orange Juice", 220, 7, false));

        Product deleted = repository.softDelete(6).orElseThrow();
        assertThat(deleted).isEqualTo(new Product(6, "Orange Juice", 220, 7, true));
        assertThat(repository.findById(6)).contains(deleted);
        assertThat(repository.findActivePage(0, 10).totalElements()).isEqualTo(2);
        assertThat(repository.update(6, "Juice", 200, 8)).isEmpty();
        assertThat(repository.softDelete(6)).isEmpty();
    }

    @Test
    void replacingTheCatalogResetsGeneratedIdsFromTheNewMaximum() {
        repository.replaceAll(List.of(new Product(8, "Coke", 150, 10, false)));
        assertThat(repository.create("Water", 100, 15).id()).isEqualTo(9);

        repository.replaceAll(List.of(new Product(2, "Juice", 200, 8, false)));
        assertThat(repository.create("Chips", 180, 12).id()).isEqualTo(3);
    }

    @Test
    void rejectsCatalogPricesThatCannotBePaidWithAcceptedCoins() {
        assertThatThrownBy(() -> repository.replaceAll(List.of(
                new Product(1, "Invalid Price", 155, 1, false))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product price must be a positive multiple of 10 cents");
    }
}
