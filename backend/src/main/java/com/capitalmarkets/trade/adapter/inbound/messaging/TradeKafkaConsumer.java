package com.capitalmarkets.trade.adapter.inbound.messaging;

import com.capitalmarkets.trade.application.TradeCaptureService;
import com.capitalmarkets.trade.application.TradeRecord;
import com.capitalmarkets.trade.domain.TradeSource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TradeKafkaConsumer {
    private final TradeCaptureService captureService;

    public TradeKafkaConsumer(TradeCaptureService captureService) {
        this.captureService = captureService;
    }

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(TradeRecord record) {
        captureService.capture(List.of(record), TradeSource.KAFKA);
    }
}
