package com.capitalmarkets.trade.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Trade(
        UUID id,
        String bookingSystem,
        long tradeId,
        String instrumentId,
        long quantity,
        BigDecimal tradePrice,
        Instant tradeTimestamp,
        TradeSource source,
        TradeStatus status,
        String validationReason,
        Instant receivedAt
) {
    public Trade {
        if (id == null || bookingSystem == null || bookingSystem.isBlank()
                || instrumentId == null || instrumentId.isBlank()
                || tradePrice == null || tradeTimestamp == null || source == null
                || status == null || receivedAt == null) {
            throw new IllegalArgumentException("Required trade fields must be provided");
        }
        if (status == TradeStatus.VALID && validationReason != null) {
            throw new IllegalArgumentException("Valid trades cannot have a validation reason");
        }
        if (status == TradeStatus.INVALID && (validationReason == null || validationReason.isBlank())) {
            throw new IllegalArgumentException("Invalid trades require a validation reason");
        }
    }
}
