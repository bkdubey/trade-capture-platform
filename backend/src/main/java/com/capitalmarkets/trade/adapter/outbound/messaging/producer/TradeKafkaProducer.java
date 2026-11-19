package com.capitalmarkets.trade.adapter.outbound.messaging.producer;

import com.capitalmarkets.trade.adapter.messaging.dto.TradeMessage;
import com.capitalmarkets.trade.config.properties.TradeKafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class TradeKafkaProducer {
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final TradeKafkaProperties properties;

    public TradeKafkaProducer(
            KafkaTemplate<Object, Object> kafkaTemplate,
            TradeKafkaProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public CompletableFuture<SendResult<Object, Object>> publish(TradeMessage message) {
        return kafkaTemplate.send(properties.getTopic(), partitionKey(message), message);
    }

    public String topic() {
        return properties.getTopic();
    }

    private String partitionKey(TradeMessage message) {
        // Keeps events for one booking-system trade on the same partition.
        return message.bookingSystem() + ":" + message.tradeId();
    }
}
