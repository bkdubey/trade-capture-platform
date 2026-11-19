package com.capitalmarkets.trade.config;

import com.capitalmarkets.trade.domain.TradeValidationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfiguration {
    @Bean
    TradeValidationPolicy tradeValidationPolicy() {
        return new TradeValidationPolicy();
    }
}
