package com.capitalmarkets.trade.adapter.inbound.web;

import com.capitalmarkets.trade.application.TradeRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KafkaIngestionControllerTest {
    private final KafkaTemplate<Object, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final KafkaIngestionController controller = new KafkaIngestionController(kafkaTemplate, "trades.test");
    private final TradeRecord trade = new TradeRecord("MUREX", 123L, "IBM", 25L,
            new BigDecimal("184.25"), Instant.parse("2026-11-18T10:00:00Z"));

    @Test
    void publishesTradeAndReturnsBrokerAcknowledgement() {
        when(kafkaTemplate.send(eq("trades.test"), eq("123"), eq(trade)))
                .thenReturn(CompletableFuture.completedFuture(null));

        var response = controller.publish(trade);

        assertThat(response).isEqualTo(new KafkaIngestionController.KafkaPublishResponse(123, "trades.test", true));
    }

    @Test
    void reportsBrokerFailuresInsteadOfReturningSuccess() {
        var result = new CompletableFuture<SendResult<Object, Object>>();
        result.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(kafkaTemplate.send(eq("trades.test"), eq("123"), eq(trade))).thenReturn(result);

        assertThatThrownBy(() -> controller.publish(trade))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Kafka did not acknowledge");
    }

    @Test
    void retainsBusinessInvalidEventsForTheConsumerValidationFlow() {
        var invalidTrade = new TradeRecord("MUREX", 124L, "IBM", 0L,
                new BigDecimal("184.25"), Instant.parse("2026-11-18T10:00:00Z"));
        when(kafkaTemplate.send(eq("trades.test"), eq("124"), eq(invalidTrade)))
                .thenReturn(CompletableFuture.completedFuture(null));

        assertThat(controller.publish(invalidTrade).published()).isTrue();
        verify(kafkaTemplate).send(eq("trades.test"), eq("124"), eq(invalidTrade));
    }
}
