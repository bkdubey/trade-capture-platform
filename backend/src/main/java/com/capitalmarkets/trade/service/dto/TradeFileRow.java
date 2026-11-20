package com.capitalmarkets.trade.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeFileRow(
        @NotBlank String bookingSystem,
        @NotNull Long tradeId,
        @NotBlank String instrumentId,
        @NotNull Long quantity,
        @NotNull BigDecimal tradePrice,
        @NotNull Instant tradeTimestamp
) {
    public TradeCaptureData toCaptureData() {
        return new TradeCaptureData(bookingSystem, tradeId, instrumentId, quantity, tradePrice, tradeTimestamp);
    }
}
