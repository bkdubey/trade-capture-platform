package com.capitalmarkets.trade.application;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TradeReportingService {
    private final TradeRepository repository;

    public TradeReportingService(TradeRepository repository) {
        this.repository = repository;
    }

    public TradeRepository.TradePage find(TradeQuery query) {
        return repository.find(query);
    }

    public TradeRepository.TradeSummary summarize(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must be earlier than or equal to 'to'");
        }
        return repository.summarize(from, to);
    }
}
