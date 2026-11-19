package com.capitalmarkets.trade.service;

import com.capitalmarkets.trade.service.repository.TradeRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class TradeReportingServiceTest {
    @Test
    void rejectsInvertedSummaryDateRange() {
        var service = new TradeReportingService(mock(TradeRepository.class));

        assertThatThrownBy(() -> service.summarize(
                Instant.parse("2026-02-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
