package com.capitalmarkets.trade.domain;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TradeValidationPolicy {
    public ValidationResult validate(long quantity, BigDecimal tradePrice) {
        if (quantity <= 0) {
            return new ValidationResult(TradeStatus.INVALID, "Quantity must be greater than zero");
        }
        if (tradePrice.signum() <= 0) {
            return new ValidationResult(TradeStatus.INVALID, "Trade price must be greater than zero");
        }
        return new ValidationResult(TradeStatus.VALID, null);
    }

    public record ValidationResult(TradeStatus status, String reason) {
    }
}
