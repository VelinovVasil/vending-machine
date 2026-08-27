package com.vendingmachine.exception;

import com.vendingmachine.dto.CoinQuantityResponse;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

public class PurchaseDeclinedException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final List<CoinQuantityResponse> returnedCoins;
    private final Map<String, Object> properties;

    public PurchaseDeclinedException(
            HttpStatus status,
            String errorCode,
            String message,
            List<CoinQuantityResponse> returnedCoins,
            Map<String, Object> properties) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.returnedCoins = List.copyOf(returnedCoins);
        this.properties = Map.copyOf(properties);
    }

    public HttpStatus status() {
        return status;
    }

    public String errorCode() {
        return errorCode;
    }

    public List<CoinQuantityResponse> returnedCoins() {
        return returnedCoins;
    }

    public Map<String, Object> properties() {
        return properties;
    }
}
