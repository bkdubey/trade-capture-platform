package com.capitalmarkets.trade.service.repository;

import com.capitalmarkets.trade.domain.Trade;
import com.capitalmarkets.trade.service.TradeQuery;
import com.capitalmarkets.trade.service.dto.TradePage;
import com.capitalmarkets.trade.service.dto.TradeSummary;

import java.time.Instant;
import java.util.List;

public interface TradeRepository {
    void saveBatch(List<Trade> trades);

    TradePage find(TradeQuery query);

    TradeSummary summarize(Instant from, Instant to);
}
