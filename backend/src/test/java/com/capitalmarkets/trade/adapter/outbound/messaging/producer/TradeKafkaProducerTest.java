package com.capitalmarkets.trade.adapter.outbound.messaging.producer;

import com.capitalmarkets.trade.adapter.messaging.dto.TradeMessage;
import com.capitalmarkets.trade.config.properties.TradeKafkaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradeKafkaProducerTest {
    @Test
    void routesTradeToConfiguredTopicUsingBookingSystemAndTradeAsKey() {
        @SuppressWarnings("unchecked")
        var kafkaTemplate = (KafkaTemplate<Object, Object>) mock(KafkaTemplate.class);
        var properties = new TradeKafkaProperties();
        properties.setTopic("trades.test");
        var producer = new TradeKafkaProducer(kafkaTemplate, properties);
        var message = new TradeMessage("MUREX", 123L, "IBM", 25L,
                new BigDecimal("184.25"), Instant.parse("2026-11-18T10:00:00Z"));
        var result = new CompletableFuture<SendResult<Object, Object>>();
        when(kafkaTemplate.send(eq("trades.test"), eq("MUREX:123"), eq(message))).thenReturn(result);

        producer.publish(message);

        verify(kafkaTemplate).send("trades.test", "MUREX:123", message);
    }
}
