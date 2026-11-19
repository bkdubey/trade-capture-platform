package com.capitalmarkets.trade.service.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeCaptureData(
        String bookingSystem,
        Long tradeId,
        String instrumentId,
        Long quantity,
        BigDecimal tradePrice,
        Instant tradeTimestamp
) {
}
