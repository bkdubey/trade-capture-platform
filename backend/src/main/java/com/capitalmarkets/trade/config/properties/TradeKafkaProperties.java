package com.capitalmarkets.trade.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.kafka")
public class TradeKafkaProperties {
    @NotBlank
    private String topic = "trades.intraday";

    @NotBlank
    private String deadLetterSuffix = ".DLT";

    @Min(1)
    private int partitions = 3;

    @Min(1)
    private int replicas = 1;

    @Valid
    @NotNull
    private final Producer producer = new Producer();

    @Valid
    @NotNull
    private final Consumer consumer = new Consumer();

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDeadLetterSuffix() {
        return deadLetterSuffix;
    }

    public void setDeadLetterSuffix(String deadLetterSuffix) {
        this.deadLetterSuffix = deadLetterSuffix;
    }

    public int getPartitions() {
        return partitions;
    }

    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public Producer getProducer() {
        return producer;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public static class Producer {
        @NotNull
        private Duration publishTimeout = Duration.ofSeconds(10);

        public Duration getPublishTimeout() {
            return publishTimeout;
        }

        public void setPublishTimeout(Duration publishTimeout) {
            this.publishTimeout = publishTimeout;
        }
    }

    public static class Consumer {
        @NotNull
        private Duration retryBackoff = Duration.ofSeconds(1);

        @Min(0)
        private long retryAttempts = 3;

        public Duration getRetryBackoff() {
            return retryBackoff;
        }

        public void setRetryBackoff(Duration retryBackoff) {
            this.retryBackoff = retryBackoff;
        }

        public long getRetryAttempts() {
            return retryAttempts;
        }

        public void setRetryAttempts(long retryAttempts) {
            this.retryAttempts = retryAttempts;
        }
    }
}
