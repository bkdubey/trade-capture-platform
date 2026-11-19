package com.capitalmarkets.trade.adapter.inbound.messaging.consumer;

import com.capitalmarkets.trade.adapter.messaging.dto.TradeMessage;
import com.capitalmarkets.trade.domain.TradeSource;
import com.capitalmarkets.trade.service.TradeCaptureService;
import com.capitalmarkets.trade.service.dto.TradeCaptureData;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TradeKafkaConsumerTest {
    @Test
    void routesMessagesThroughTheSharedCaptureUseCase() {
        var service = mock(TradeCaptureService.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var consumer = new TradeKafkaConsumer(service, factory.getValidator());
            var record = new TradeMessage("MUREX", 100L, "IBM", 12L, new BigDecimal("10.50"),
                    Instant.parse("2026-01-01T10:00:00Z"));

            consumer.consume(record);

            verify(service).capture(List.of(new TradeCaptureData(
                    record.bookingSystem(), record.tradeId(), record.instrumentId(), record.quantity(),
                    record.tradePrice(), record.tradeTimestamp())), TradeSource.KAFKA);
        }
    }

    @Test
    void rejectsMalformedMessagesBeforeCapture() {
        var service = mock(TradeCaptureService.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var consumer = new TradeKafkaConsumer(service, factory.getValidator());
            var malformed = new TradeMessage("", null, "IBM", 12L, new BigDecimal("10.50"),
                    Instant.parse("2026-01-01T10:00:00Z"));

            assertThatThrownBy(() -> consumer.consume(malformed))
                    .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
            verifyNoInteractions(service);
        }
    }
}
