package com.capitalmarkets.trade.application;

import com.capitalmarkets.trade.domain.TradeSource;
import com.capitalmarkets.trade.domain.TradeStatus;

import java.time.Instant;

public record TradeQuery(
        TradeStatus status,
        TradeSource source,
        Instant from,
        Instant to,
        int page,
        int size,
        String search,
        TradeSortField sortBy,
        TradeSortDirection sortDirection
) {
    public TradeQuery(TradeStatus status, TradeSource source, Instant from, Instant to, int page, int size) {
        this(status, source, from, to, page, size, null,
                TradeSortField.TRADE_TIMESTAMP, TradeSortDirection.DESC);
    }

    public TradeQuery {
        if (page < 0 || size < 1 || size > 500) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 500");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must be earlier than or equal to 'to'");
        }
        if (sortBy == null || sortDirection == null) {
            throw new IllegalArgumentException("Trade sort field and direction are required");
        }
        if (search != null && search.length() > 128) {
            throw new IllegalArgumentException("Trade search is limited to 128 characters");
        }
        if (search != null && search.isBlank()) {
            search = null;
        }
    }
}
