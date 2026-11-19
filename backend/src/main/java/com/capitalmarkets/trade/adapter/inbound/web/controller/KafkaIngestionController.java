package com.capitalmarkets.trade.adapter.inbound.web.controller;

import com.capitalmarkets.trade.adapter.inbound.web.dto.KafkaPublishResponse;
import com.capitalmarkets.trade.adapter.inbound.web.dto.KafkaTradeRequest;
import com.capitalmarkets.trade.adapter.outbound.messaging.producer.TradeKafkaProducer;
import com.capitalmarkets.trade.config.properties.TradeKafkaProperties;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/ingestion")
public class KafkaIngestionController {
    private final TradeKafkaProducer producer;
    private final TradeKafkaProperties properties;

    public KafkaIngestionController(
            TradeKafkaProducer producer,
            TradeKafkaProperties properties) {
        this.producer = producer;
        this.properties = properties;
    }

    @PostMapping("/kafka")
    public CompletableFuture<KafkaPublishResponse> publish(@Valid @RequestBody KafkaTradeRequest request) {
        var message = request.toMessage();
        // Spring MVC keeps the request async while waiting for the configured broker acknowledgement timeout.
        return producer.publish(message)
                .orTimeout(properties.getProducer().getPublishTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .thenApply(result -> new KafkaPublishResponse(message.tradeId(), producer.topic(), true))
                .exceptionally(this::publishFailure);
    }

    private KafkaPublishResponse publishFailure(Throwable failure) {
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause() : failure;
        throw unavailable("Kafka did not acknowledge the trade event", cause);
    }

    private ResponseStatusException unavailable(String message, Throwable cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
