package com.capitalmarkets.trade.adapter.inbound.messaging.consumer;

import com.capitalmarkets.trade.adapter.messaging.dto.TradeMessage;
import com.capitalmarkets.trade.domain.TradeSource;
import com.capitalmarkets.trade.service.TradeCaptureService;
import com.capitalmarkets.trade.service.dto.TradeCaptureData;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TradeKafkaConsumer {
    private final TradeCaptureService captureService;
    private final Validator validator;

    public TradeKafkaConsumer(TradeCaptureService captureService, Validator validator) {
        this.captureService = captureService;
        this.validator = validator;
    }

    @KafkaListener(
            topics = "#{@tradeKafkaTopics}",
            groupId = "${spring.kafka.consumer.group-id}",
            autoStartup = "${app.kafka.consumer.enabled:true}")
    public void consume(TradeMessage message) {
        var violations = validator.validate(message);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        var data = new TradeCaptureData(message.bookingSystem(), message.tradeId(), message.instrumentId(),
                message.quantity(), message.tradePrice(), message.tradeTimestamp());
        captureService.capture(List.of(data), TradeSource.KAFKA);
    }
}
