package com.capitalmarkets.trade.adapter.inbound.messaging;

import com.capitalmarkets.trade.application.TradeCaptureService;
import com.capitalmarkets.trade.application.TradeRecord;
import com.capitalmarkets.trade.domain.TradeSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

class TradeKafkaConsumerTest {
    @Test
    void routesMessagesThroughTheSharedCaptureUseCase() {
        var service = mock(TradeCaptureService.class);
        var consumer = new TradeKafkaConsumer(service);
        var record = new TradeRecord("MUREX", 100L, "IBM", 12L, new BigDecimal("10.50"),
                Instant.parse("2026-01-01T10:00:00Z"));

        consumer.consume(record);

        verify(service).capture(List.of(record), TradeSource.KAFKA);
    }
}
