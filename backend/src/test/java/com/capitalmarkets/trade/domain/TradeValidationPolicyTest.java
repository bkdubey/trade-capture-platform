package com.capitalmarkets.trade.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TradeValidationPolicyTest {
    private final TradeValidationPolicy policy = new TradeValidationPolicy();

    @Test
    void flagsZeroOrNegativeQuantityAsInvalid() {
        var result = policy.validate(0, new BigDecimal("10.25"));

        assertThat(result.status()).isEqualTo(TradeStatus.INVALID);
        assertThat(result.reason()).isEqualTo("Quantity must be greater than zero");
        assertThat(policy.validate(-1, new BigDecimal("10.25")).reason())
                .isEqualTo("Quantity must be greater than zero");
    }

    @Test
    void flagsZeroOrNegativePriceAsInvalid() {
        assertThat(policy.validate(2, BigDecimal.ZERO).reason())
                .isEqualTo("Trade price must be greater than zero");
        assertThat(policy.validate(2, new BigDecimal("-0.01")).status())
                .isEqualTo(TradeStatus.INVALID);
    }

    @Test
    void acceptsPositivePriceAndQuantity() {
        var result = policy.validate(2, new BigDecimal("0.01"));

        assertThat(result.status()).isEqualTo(TradeStatus.VALID);
        assertThat(result.reason()).isNull();
    }
}
