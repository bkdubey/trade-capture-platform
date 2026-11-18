package com.capitalmarkets.trade.application;

import com.capitalmarkets.trade.domain.Trade;
import com.capitalmarkets.trade.domain.TradeSource;

import java.time.Instant;
import java.util.List;

public interface TradeRepository {
    void saveBatch(List<Trade> trades);

    TradePage find(TradeQuery query);

    TradeSummary summarize(Instant from, Instant to);

    record TradePage(List<Trade> content, long totalElements, int page, int size) {
    }

    record TradeSummary(long total, long valid, long invalid, long file, long kafka) {
    }
}
