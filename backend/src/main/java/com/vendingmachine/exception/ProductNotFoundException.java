package com.vendingmachine.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(int id) {
        super("Product with id " + id + " was not found");
    }
}
