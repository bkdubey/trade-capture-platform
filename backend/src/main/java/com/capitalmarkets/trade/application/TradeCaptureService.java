package com.capitalmarkets.trade.application;

import com.capitalmarkets.trade.domain.Trade;
import com.capitalmarkets.trade.domain.TradeSource;
import com.capitalmarkets.trade.domain.TradeValidationPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TradeCaptureService {
    private static final int WRITE_BATCH_SIZE = 1_000;

    private final TradeRepository repository;
    private final TradeValidationPolicy validationPolicy;
    private final Clock clock;

    public TradeCaptureService(TradeRepository repository, TradeValidationPolicy validationPolicy, Clock clock) {
        this.repository = repository;
        this.validationPolicy = validationPolicy;
        this.clock = clock;
    }

    @Transactional
    public int capture(List<TradeRecord> records, TradeSource source) {
        Instant receivedAt = clock.instant();
        int captured = 0;
        for (int start = 0; start < records.size(); start += WRITE_BATCH_SIZE) {
            List<Trade> batch = records.subList(start, Math.min(start + WRITE_BATCH_SIZE, records.size()))
                    .stream().map(record -> toTrade(record, source, receivedAt)).toList();
            repository.saveBatch(batch);
            captured += batch.size();
        }
        return captured;
    }

    private Trade toTrade(TradeRecord record, TradeSource source, Instant receivedAt) {
        var result = validationPolicy.validate(record.quantity(), record.tradePrice());
        return new Trade(UUID.randomUUID(), record.bookingSystem(), record.tradeId(), record.instrumentId(),
                record.quantity(), record.tradePrice(), record.tradeTimestamp(), source, result.status(),
                result.reason(), receivedAt);
    }
}
