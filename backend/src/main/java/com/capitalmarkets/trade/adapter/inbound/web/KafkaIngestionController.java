package com.capitalmarkets.trade.adapter.inbound.web;

import com.capitalmarkets.trade.application.TradeRecord;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/v1/ingestion")
public class KafkaIngestionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaIngestionController.class);
    private static final long PUBLISH_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final String topic;

    public KafkaIngestionController(
            KafkaTemplate<Object, Object> kafkaTemplate,
            @Value("${app.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @PostMapping("/kafka")
    public KafkaPublishResponse publish(@Valid @RequestBody TradeRecord trade) {
        try {
            kafkaTemplate.send(topic, trade.tradeId().toString(), trade)
                    .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            LOGGER.info("Published local trade event to Kafka topic={} tradeId={}", topic, trade.tradeId());
            return new KafkaPublishResponse(trade.tradeId(), topic, true);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable("Kafka publishing was interrupted", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw unavailable("Kafka did not acknowledge the trade event", exception);
        }
    }

    private ResponseStatusException unavailable(String message, Exception cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }

    public record KafkaPublishResponse(long tradeId, String topic, boolean published) {
    }
}
