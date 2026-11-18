package com.capitalmarkets.trade.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeRecord(
        @NotBlank String bookingSystem,
        @NotNull Long tradeId,
        @NotBlank String instrumentId,
        @NotNull Long quantity,
        @NotNull BigDecimal tradePrice,
        @NotNull Instant tradeTimestamp
) {
}
