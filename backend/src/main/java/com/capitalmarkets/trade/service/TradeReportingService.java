package com.capitalmarkets.trade.service;

import com.capitalmarkets.trade.service.repository.TradeRepository;
import com.capitalmarkets.trade.service.dto.TradePage;
import com.capitalmarkets.trade.service.dto.TradeSummary;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TradeReportingService {
    private final TradeRepository repository;

    public TradeReportingService(TradeRepository repository) {
        this.repository = repository;
    }

    public TradePage find(TradeQuery query) {
        return repository.find(query);
    }

    public TradeSummary summarize(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must be earlier than or equal to 'to'");
        }
        return repository.summarize(from, to);
    }
}
