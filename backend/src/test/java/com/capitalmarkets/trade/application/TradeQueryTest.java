package com.capitalmarkets.trade.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TradeQueryTest {
    @Test
    void rejectsNegativePageAndInvalidPageSize() {
        assertThatThrownBy(() -> new TradeQuery(null, null, null, null, -1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TradeQuery(null, null, null, null, 0, 501))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnboundedSearchTerms() {
        assertThatThrownBy(() -> new TradeQuery(
                null, null, null, null, 0, 50, "x".repeat(129),
                TradeSortField.TRADE_TIMESTAMP, TradeSortDirection.DESC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("128 characters");
    }
}
