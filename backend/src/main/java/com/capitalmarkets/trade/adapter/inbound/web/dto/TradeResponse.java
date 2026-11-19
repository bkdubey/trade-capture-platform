package com.capitalmarkets.trade.adapter.inbound.web.dto;

import com.capitalmarkets.trade.domain.Trade;
import com.capitalmarkets.trade.domain.TradeSource;
import com.capitalmarkets.trade.domain.TradeStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradeResponse(
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
    public static TradeResponse from(Trade trade) {
        return new TradeResponse(trade.id(), trade.bookingSystem(), trade.tradeId(), trade.instrumentId(),
                trade.quantity(), trade.tradePrice(), trade.tradeTimestamp(), trade.source(), trade.status(),
                trade.validationReason(), trade.receivedAt());
    }
}
