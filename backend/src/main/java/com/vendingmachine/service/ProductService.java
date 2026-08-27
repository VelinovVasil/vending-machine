package com.vendingmachine.service;

import com.vendingmachine.dto.CreateProductRequest;
import com.vendingmachine.dto.ProductPageResponse;
import com.vendingmachine.dto.ProductResponse;
import com.vendingmachine.dto.UpdateProductRequest;

public interface ProductService {

    ProductPageResponse findAll(int page, int size);

    ProductResponse findById(int id);

    ProductResponse create(CreateProductRequest request);

    ProductResponse update(int id, UpdateProductRequest request);

    ProductResponse delete(int id);
}
