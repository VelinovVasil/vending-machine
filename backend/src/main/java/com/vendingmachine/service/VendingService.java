package com.vendingmachine.service;

import com.vendingmachine.dto.PurchaseRequest;
import com.vendingmachine.dto.PurchaseResponse;
import com.vendingmachine.dto.VendingConfigurationResponse;

public interface VendingService {

    VendingConfigurationResponse getConfiguration();

    PurchaseResponse purchase(PurchaseRequest request);
}
