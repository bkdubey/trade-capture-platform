package com.capitalmarkets.trade.adapter.inbound.web.controller;

import com.capitalmarkets.trade.adapter.inbound.web.dto.TradeSummaryResponse;
import com.capitalmarkets.trade.domain.Trade;
import com.capitalmarkets.trade.domain.TradeSource;
import com.capitalmarkets.trade.domain.TradeStatus;
import com.capitalmarkets.trade.service.TradeQuery;
import com.capitalmarkets.trade.service.TradeReportingService;
import com.capitalmarkets.trade.service.TradeSortDirection;
import com.capitalmarkets.trade.service.TradeSortField;
import com.capitalmarkets.trade.service.dto.TradePage;
import com.capitalmarkets.trade.service.dto.TradeSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TradeControllerTest {
    private final TradeReportingService reportingService = mock(TradeReportingService.class);
    private final TradeController controller = new TradeController(reportingService);

    @Test
    void returnsPagedTradesUsingAllFilters() {
        var from = Instant.parse("2026-01-01T00:00:00Z");
        var to = Instant.parse("2026-01-02T00:00:00Z");
        var trade = new Trade(UUID.randomUUID(), "MUREX", 42, "IBM", 12, new BigDecimal("10.50"),
                from, TradeSource.KAFKA, TradeStatus.VALID, null, from);
        when(reportingService.find(any())).thenReturn(new TradePage(List.of(trade), 17, 2, 10));

        var response = controller.trades(
                TradeStatus.INVALID, TradeSource.KAFKA, from, to, 2, 10, "ibm",
                TradeSortField.TRADE_ID, TradeSortDirection.ASC);

        assertThat(response.totalElements()).isEqualTo(17);
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.content().getFirst().tradeId()).isEqualTo(42);
        verify(reportingService).find(new TradeQuery(TradeStatus.INVALID, TradeSource.KAFKA,
                from, to, 2, 10, "ibm", TradeSortField.TRADE_ID, TradeSortDirection.ASC));
    }

    @Test
    void returnsSummaryForOptionalTimeRange() {
        var from = Instant.parse("2026-01-01T00:00:00Z");
        when(reportingService.summarize(from, null)).thenReturn(
                new TradeSummary(8, 6, 2, 5, 3));

        var response = controller.summary(from, null);

        assertThat(response).isEqualTo(new TradeSummaryResponse(8, 6, 2, 5, 3));
    }
}
