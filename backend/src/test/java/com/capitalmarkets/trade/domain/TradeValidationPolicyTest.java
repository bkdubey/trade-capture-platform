package com.capitalmarkets.trade.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TradeValidationPolicyTest {
    private final TradeValidationPolicy policy = new TradeValidationPolicy(List.of(
            new QuantityPositiveValidationRule(), new TradePricePositiveValidationRule()));

    @Test
    void flagsZeroOrNegativeQuantityAsInvalid() {
        var result = policy.validate(trade(0, new BigDecimal("10.25")));

        assertThat(result.status()).isEqualTo(TradeStatus.INVALID);
        assertThat(result.code()).isEqualTo(TradeValidationCode.QUANTITY_NOT_POSITIVE);
        assertThat(result.reason()).isEqualTo("Quantity must be greater than zero");
        assertThat(policy.validate(trade(-1, new BigDecimal("10.25"))).reason())
                .isEqualTo("Quantity must be greater than zero");
    }

    @Test
    void flagsZeroOrNegativePriceAsInvalid() {
        assertThat(policy.validate(trade(2, BigDecimal.ZERO)).reason())
                .isEqualTo("Trade price must be greater than zero");
        assertThat(policy.validate(trade(2, new BigDecimal("-0.01"))).status())
                .isEqualTo(TradeStatus.INVALID);
    }

    @Test
    void acceptsPositivePriceAndQuantity() {
        var result = policy.validate(trade(2, new BigDecimal("0.01")));

        assertThat(result.status()).isEqualTo(TradeStatus.VALID);
        assertThat(result.reason()).isNull();
    }

    @Test
    void acceptsAdditionalRulesWithoutChangingTheValidationPolicy() {
        var extensiblePolicy = new TradeValidationPolicy(List.of(
                new QuantityPositiveValidationRule(),
                new TradePricePositiveValidationRule(),
                trade -> trade.instrumentId().startsWith("OTC")
                        ? Optional.of(TradeValidationCode.INSTRUMENT_NOT_SUPPORTED)
                        : Optional.empty()));

        assertThat(extensiblePolicy.validate(new TradeValidationContext(
                "BOOK", 1, "OTC-XYZ", 2, BigDecimal.ONE, Instant.EPOCH)).reason())
                .isEqualTo("Instrument is not supported");
    }

    private TradeValidationContext trade(long quantity, BigDecimal price) {
        return new TradeValidationContext("BOOK", 1, "IBM", quantity, price, Instant.EPOCH);
    }
}
