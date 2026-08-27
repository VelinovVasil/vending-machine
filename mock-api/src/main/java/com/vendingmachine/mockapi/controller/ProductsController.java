package com.vendingmachine.mockapi.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductsController {

    private final Resource products = new ClassPathResource("products.json");

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Resource getProducts() {
        return products;
    }
}
