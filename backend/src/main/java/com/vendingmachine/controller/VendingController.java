package com.vendingmachine.controller;

import com.vendingmachine.dto.PurchaseRequest;
import com.vendingmachine.dto.PurchaseResponse;
import com.vendingmachine.dto.VendingConfigurationResponse;
import com.vendingmachine.service.VendingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vending")
@RequiredArgsConstructor
public class VendingController {

    private final VendingService vendingService;

    @GetMapping("/denominations")
    public VendingConfigurationResponse getDenominations() {
        return vendingService.getConfiguration();
    }

    @PostMapping("/purchases")
    public PurchaseResponse purchase(@Valid @RequestBody PurchaseRequest request) {
        return vendingService.purchase(request);
    }
}
