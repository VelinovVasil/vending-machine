package com.vendingmachine.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CoinInventoryProperties.class)
public class CoinInventoryConfiguration {
}
