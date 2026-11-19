package com.capitalmarkets.trade.adapter.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

// Stable Kafka value contract, independent of HTTP request and persistence models.
public record TradeMessage(
        @NotBlank String bookingSystem,
        @NotNull Long tradeId,
        @NotBlank String instrumentId,
        @NotNull Long quantity,
        @NotNull BigDecimal tradePrice,
        @NotNull Instant tradeTimestamp
) {
}
