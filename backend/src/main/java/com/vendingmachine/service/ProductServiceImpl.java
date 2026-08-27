package com.vendingmachine.service;

import com.vendingmachine.dto.CreateProductRequest;
import com.vendingmachine.dto.ProductPageResponse;
import com.vendingmachine.dto.ProductResponse;
import com.vendingmachine.dto.UpdateProductRequest;
import com.vendingmachine.exception.ProductNotFoundException;
import com.vendingmachine.model.PageResult;
import com.vendingmachine.model.Product;
import com.vendingmachine.repository.InMemoryProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final InMemoryProductRepository productRepository;

    @Override
    public ProductPageResponse findAll(int page, int size) {
        PageResult<Product> productPage = productRepository.findActivePage(page, size);
        List<ProductResponse> content = productPage.content().stream()
                .map(this::toResponse)
                .toList();

        return new ProductPageResponse(
                content,
                productPage.page(),
                productPage.size(),
                productPage.totalElements(),
                productPage.totalPages());
    }

    @Override
    public ProductResponse findById(int id) {
        return productRepository.findById(id)
                .filter(product -> !product.deleted())
                .map(this::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    public ProductResponse create(CreateProductRequest request) {
        Product product = productRepository.create(request.name(), request.price(), request.quantity());
        return toResponse(product);
    }

    @Override
    public ProductResponse update(int id, UpdateProductRequest request) {
        return productRepository.update(id, request.name(), request.price(), request.quantity())
                .map(this::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    public ProductResponse delete(int id) {
        return productRepository.softDelete(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(product.id(), product.name(), product.price(), product.quantity());
    }
}
