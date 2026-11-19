package com.capitalmarkets.trade.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeValidationContext(
        String bookingSystem,
        long tradeId,
        String instrumentId,
        long quantity,
        BigDecimal tradePrice,
        Instant tradeTimestamp
) {
}
