package com.capitalmarkets.trade.application;

import com.capitalmarkets.trade.domain.Trade;
import com.capitalmarkets.trade.domain.TradeSource;
import com.capitalmarkets.trade.domain.TradeStatus;
import com.capitalmarkets.trade.domain.TradeValidationPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class TradeCaptureServiceTest {
    private final TradeRepository repository = mock(TradeRepository.class);
    private final Instant receivedAt = Instant.parse("2026-01-01T10:00:00Z");
    private final TradeCaptureService service = new TradeCaptureService(
            repository, new TradeValidationPolicy(), Clock.fixed(receivedAt, ZoneOffset.UTC));

    @Test
    void validatesAndPersistsBothValidAndInvalidRecords() {
        TradeRecord valid = record(1, 25, "100.50");
        TradeRecord invalidZero = record(2, 0, "100.50");
        TradeRecord invalidNegative = record(3, -2, "100.50");

        int accepted = service.capture(List.of(valid, invalidZero, invalidNegative), TradeSource.FILE);

        assertThat(accepted).isEqualTo(3);
        var captor = org.mockito.ArgumentCaptor.<List<Trade>>captor();
        verify(repository).saveBatch(captor.capture());
        List<Trade> trades = captor.getValue();
        assertThat(trades).extracting(Trade::status)
                .containsExactly(TradeStatus.VALID, TradeStatus.INVALID, TradeStatus.INVALID);
        assertThat(trades.get(0).source()).isEqualTo(TradeSource.FILE);
        assertThat(trades.get(1).validationReason()).isEqualTo("Quantity must be greater than zero");
        assertThat(trades.get(2).validationReason()).isEqualTo("Quantity must be greater than zero");
        assertThat(trades).allSatisfy(trade -> assertThat(trade.receivedAt()).isEqualTo(receivedAt));
    }

    @Test
    void persistsLargeCapturesInBoundedBatches() {
        List<TradeRecord> records = new ArrayList<>();
        for (int i = 0; i < 1_001; i++) {
            records.add(record(i, 1, "1.25"));
        }

        assertThat(service.capture(records, TradeSource.KAFKA)).isEqualTo(1_001);

        var captor = org.mockito.ArgumentCaptor.<List<Trade>>captor();
        verify(repository, times(2)).saveBatch(captor.capture());
        assertThat(captor.getAllValues()).extracting(List::size).containsExactly(1_000, 1);
    }

    @Test
    void emptyCaptureDoesNotWriteToRepository() {
        assertThat(service.capture(List.of(), TradeSource.FILE)).isZero();
        verify(repository, never()).saveBatch(anyList());
    }

    private TradeRecord record(long id, long quantity, String price) {
        return new TradeRecord("BOOK", id, "IBM", quantity, new BigDecimal(price), receivedAt);
    }
}
