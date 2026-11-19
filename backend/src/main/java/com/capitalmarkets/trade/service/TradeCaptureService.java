package com.capitalmarkets.trade.service;

import com.capitalmarkets.trade.config.properties.TradeIngestionProperties;
import com.capitalmarkets.trade.domain.Trade;
import com.capitalmarkets.trade.domain.TradeSource;
import com.capitalmarkets.trade.domain.TradeValidationPolicy;
import com.capitalmarkets.trade.service.dto.TradeCaptureData;
import com.capitalmarkets.trade.service.port.TradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TradeCaptureService {
    private final TradeRepository repository;
    private final TradeValidationPolicy validationPolicy;
    private final Clock clock;
    private final TradeIngestionProperties ingestionProperties;

    public TradeCaptureService(
            TradeRepository repository,
            TradeValidationPolicy validationPolicy,
            Clock clock,
            TradeIngestionProperties ingestionProperties) {
        this.repository = repository;
        this.validationPolicy = validationPolicy;
        this.clock = clock;
        this.ingestionProperties = ingestionProperties;
    }

    @Transactional
    public int capture(List<TradeCaptureData> records, TradeSource source) {
        Instant receivedAt = clock.instant();
        int captured = 0;
        int batchSize = ingestionProperties.getWriteBatchSize();
        for (int start = 0; start < records.size(); start += batchSize) {
            List<Trade> batch = records.subList(start, Math.min(start + batchSize, records.size()))
                    .stream().map(record -> toTrade(record, source, receivedAt)).toList();
            repository.saveBatch(batch);
            captured += batch.size();
        }
        return captured;
    }

    private Trade toTrade(TradeCaptureData record, TradeSource source, Instant receivedAt) {
        var result = validationPolicy.validate(record.quantity(), record.tradePrice());
        return new Trade(UUID.randomUUID(), record.bookingSystem(), record.tradeId(), record.instrumentId(),
                record.quantity(), record.tradePrice(), record.tradeTimestamp(), source, result.status(),
                result.reason(), receivedAt);
    }
}
