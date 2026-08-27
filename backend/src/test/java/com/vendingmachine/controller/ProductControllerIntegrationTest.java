package com.vendingmachine.controller;

import com.vendingmachine.exception.ApiExceptionHandler;
import com.vendingmachine.model.Product;
import com.vendingmachine.repository.InMemoryProductRepository;
import com.vendingmachine.service.ProductServiceImpl;
import com.vendingmachine.service.VendingMachineStateCoordinator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({
        ProductServiceImpl.class,
        InMemoryProductRepository.class,
        VendingMachineStateCoordinator.class,
        ApiExceptionHandler.class
})
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryProductRepository repository;

    @BeforeEach
    void setUp() {
        repository.replaceAll(List.of(
                new Product(5, "Chocolate Bar", 250, 6, false),
                new Product(3, "Orange Juice", 200, 8, true),
                new Product(1, "Coke", 150, 10, false)));
    }

    @Test
    void listsActiveProductsWithPaginationMetadataAndStableOrdering() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].deleted").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getsOnlyActiveProductsById() throws Exception {
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Coke"))
                .andExpect(jsonPath("$.price").value(150))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.deleted").doesNotExist());

        mockMvc.perform(get("/api/products/3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product not found"));
        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createsAProductWithABackendGeneratedId() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Chips","price":180,"quantity":12}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/products/6"))
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.name").value("Chips"))
                .andExpect(jsonPath("$.price").value(180))
                .andExpect(jsonPath("$.quantity").value(12))
                .andExpect(jsonPath("$.deleted").doesNotExist());
    }

    @Test
    void fullyUpdatesAnActiveProduct() throws Exception {
        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Coke Zero","price":160,"quantity":9}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Coke Zero"))
                .andExpect(jsonPath("$.price").value(160))
                .andExpect(jsonPath("$.quantity").value(9));

        mockMvc.perform(put("/api/products/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Juice","price":200,"quantity":8}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void softDeletesAndThenHidesTheProduct() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Coke"))
                .andExpect(jsonPath("$.deleted").doesNotExist());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNotFound());

        assertThat(repository.findById(1)).get().extracting(Product::deleted).isEqualTo(true);
    }

    @Test
    void validatesProductBodiesAndPaginationParameters() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","price":0,"quantity":16}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.quantity").exists());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Invalid Price","price":155,"quantity":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists());

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Coke","price":150}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.quantity").exists());

        mockMvc.perform(get("/api/products").param("page", "-1").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void rejectsMalformedJsonAndNonNumericParameters() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request could not be parsed"));

        mockMvc.perform(get("/api/products").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request could not be parsed"));
    }
}
