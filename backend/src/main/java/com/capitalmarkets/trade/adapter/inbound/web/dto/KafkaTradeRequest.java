package com.capitalmarkets.trade.adapter.inbound.web.dto;

import com.capitalmarkets.trade.adapter.messaging.dto.TradeMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

// HTTP request DTO: keep the external REST contract separate from the Kafka message.
public record KafkaTradeRequest(
        @NotBlank String bookingSystem,
        @NotNull Long tradeId,
        @NotBlank String instrumentId,
        @NotNull Long quantity,
        @NotNull BigDecimal tradePrice,
        @NotNull Instant tradeTimestamp
) {
    public TradeMessage toMessage() {
        return new TradeMessage(bookingSystem, tradeId, instrumentId, quantity, tradePrice, tradeTimestamp);
    }
}
