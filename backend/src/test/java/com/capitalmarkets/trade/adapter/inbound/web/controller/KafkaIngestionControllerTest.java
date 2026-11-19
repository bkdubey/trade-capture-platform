package com.capitalmarkets.trade.adapter.inbound.web.controller;

import com.capitalmarkets.trade.adapter.inbound.web.dto.KafkaPublishResponse;
import com.capitalmarkets.trade.adapter.inbound.web.dto.KafkaTradeRequest;
import com.capitalmarkets.trade.adapter.outbound.messaging.producer.TradeKafkaProducer;
import com.capitalmarkets.trade.config.properties.TradeKafkaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class KafkaIngestionControllerTest {
    private final TradeKafkaProducer producer = mock(TradeKafkaProducer.class);
    private final TradeKafkaProperties properties = new TradeKafkaProperties();
    private final KafkaIngestionController controller = new KafkaIngestionController(producer, properties);
    private final KafkaTradeRequest trade = new KafkaTradeRequest("MUREX", 123L, "IBM", 25L,
            new BigDecimal("184.25"), Instant.parse("2026-11-18T10:00:00Z"));

    @Test
    void publishesTradeAndReturnsBrokerAcknowledgement() {
        properties.setTopic("trades.test");
        when(producer.topicFor(trade.toMessage())).thenReturn("trades.test");
        when(producer.publish(trade.toMessage())).thenReturn(completedSend());

        var response = controller.publish(trade).join();

        assertThat(response).isEqualTo(new KafkaPublishResponse(123, "trades.test", true));
    }

    @Test
    void reportsBrokerFailuresInsteadOfReturningSuccess() {
        var result = new CompletableFuture<org.springframework.kafka.support.SendResult<Object, Object>>();
        result.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(producer.publish(trade.toMessage())).thenReturn(result);

        assertThatThrownBy(() -> controller.publish(trade).join())
                .isInstanceOf(java.util.concurrent.CompletionException.class)
                .hasCauseInstanceOf(ResponseStatusException.class);
    }

    @Test
    void retainsBusinessInvalidEventsForTheConsumerValidationFlow() {
        var invalidTrade = new KafkaTradeRequest("MUREX", 124L, "IBM", 0L,
                new BigDecimal("184.25"), Instant.parse("2026-11-18T10:00:00Z"));
        when(producer.publish(invalidTrade.toMessage())).thenReturn(completedSend());

        assertThat(controller.publish(invalidTrade).join().published()).isTrue();
        verify(producer).publish(invalidTrade.toMessage());
    }

    @Test
    void reportsPublishTimeoutAsUnavailable() {
        properties.getProducer().setPublishTimeout(java.time.Duration.ofMillis(1));
        when(producer.publish(trade.toMessage())).thenReturn(new CompletableFuture<>());

        assertThatThrownBy(() -> controller.publish(trade).join())
                .isInstanceOf(java.util.concurrent.CompletionException.class)
                .hasCauseInstanceOf(ResponseStatusException.class);
    }

    private CompletableFuture<org.springframework.kafka.support.SendResult<Object, Object>> completedSend() {
        return CompletableFuture.completedFuture(null);
    }
}
