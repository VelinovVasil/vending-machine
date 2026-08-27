package com.vendingmachine.service;

import com.vendingmachine.dto.CreateProductRequest;
import com.vendingmachine.dto.ProductPageResponse;
import com.vendingmachine.dto.ProductResponse;
import com.vendingmachine.dto.UpdateProductRequest;
import com.vendingmachine.exception.ProductNotFoundException;
import com.vendingmachine.model.Product;
import com.vendingmachine.repository.InMemoryProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductServiceTest {

    private InMemoryProductRepository repository;
    private ProductService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepository();
        repository.replaceAll(List.of(
                new Product(5, "Chocolate Bar", 250, 6, false),
                new Product(2, "Water", 100, 15, true),
                new Product(1, "Coke", 150, 10, false),
                new Product(3, "Orange Juice", 200, 8, false)));
        service = new ProductServiceImpl(repository);
    }

    @Test
    void returnsStableActiveOnlyPagination() {
        ProductPageResponse firstPage = service.findAll(0, 2);
        ProductPageResponse secondPage = service.findAll(1, 2);
        ProductPageResponse outOfRangePage = service.findAll(5, 2);

        assertThat(firstPage.content()).extracting(ProductResponse::id).containsExactly(1, 3);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.content()).extracting(ProductResponse::id).containsExactly(5);
        assertThat(outOfRangePage.content()).isEmpty();
        assertThat(outOfRangePage.totalElements()).isEqualTo(3);
        assertThat(outOfRangePage.totalPages()).isEqualTo(2);
    }

    @Test
    void createsUpdatesAndDeletesActiveProducts() {
        ProductResponse created = service.create(new CreateProductRequest("Chips", 180, 12));
        assertThat(created).isEqualTo(new ProductResponse(6, "Chips", 180, 12));

        ProductResponse updated = service.update(6, new UpdateProductRequest("Salted Chips", 190, 11));
        assertThat(updated).isEqualTo(new ProductResponse(6, "Salted Chips", 190, 11));

        ProductResponse deleted = service.delete(6);
        assertThat(deleted).isEqualTo(updated);
        assertThatThrownBy(() -> service.findById(6)).isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> service.update(6, new UpdateProductRequest("Chips", 180, 12)))
                .isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> service.delete(6)).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void treatsMissingAndDeletedProductsAsNotFound() {
        assertThatThrownBy(() -> service.findById(2)).isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> service.findById(99)).isInstanceOf(ProductNotFoundException.class);
    }
}
