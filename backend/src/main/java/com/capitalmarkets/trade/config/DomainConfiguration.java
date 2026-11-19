package com.capitalmarkets.trade.config;

import com.capitalmarkets.trade.domain.QuantityPositiveValidationRule;
import com.capitalmarkets.trade.domain.TradePricePositiveValidationRule;
import com.capitalmarkets.trade.domain.TradeValidationPolicy;
import com.capitalmarkets.trade.domain.TradeValidationRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
public class DomainConfiguration {
    @Bean
    @Order(10)
    TradeValidationRule quantityPositiveValidationRule() {
        return new QuantityPositiveValidationRule();
    }

    @Bean
    @Order(20)
    TradeValidationRule tradePricePositiveValidationRule() {
        return new TradePricePositiveValidationRule();
    }

    @Bean
    TradeValidationPolicy tradeValidationPolicy(List<TradeValidationRule> rules) {
        return new TradeValidationPolicy(rules);
    }
}
