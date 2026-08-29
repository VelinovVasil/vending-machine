package com.vendingmachine.exception;

public class DuplicateProductPriceException extends RuntimeException {

    private final int price;

    public DuplicateProductPriceException(int price) {
        super("An active product already uses the price " + price + " cents");
        this.price = price;
    }

    public int price() {
        return price;
    }
}
