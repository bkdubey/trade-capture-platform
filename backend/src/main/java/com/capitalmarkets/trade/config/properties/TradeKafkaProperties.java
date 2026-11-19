package com.capitalmarkets.trade.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    // Booking-system routing overrides; unlisted systems use the default topic.
    @Valid
    private Map<String, @NotBlank String> bookingSystemTopics = new LinkedHashMap<>();

    // Topic-specific partition counts override the default for newly provisioned topics.
    @Valid
    private Map<String, @Min(1) Integer> topicPartitions = new LinkedHashMap<>();

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

    public Map<String, String> getBookingSystemTopics() {
        return bookingSystemTopics;
    }

    public void setBookingSystemTopics(Map<String, String> bookingSystemTopics) {
        this.bookingSystemTopics = bookingSystemTopics == null ? new LinkedHashMap<>() : bookingSystemTopics;
    }

    public Map<String, Integer> getTopicPartitions() {
        return topicPartitions;
    }

    public void setTopicPartitions(Map<String, Integer> topicPartitions) {
        this.topicPartitions = topicPartitions == null ? new LinkedHashMap<>() : topicPartitions;
    }

    public String topicFor(String bookingSystem) {
        return bookingSystemTopics.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(bookingSystem))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(topic);
    }

    public int partitionsFor(String topicName) {
        return topicPartitions.getOrDefault(topicName, partitions);
    }

    public List<String> getAllTopics() {
        var topics = new LinkedHashSet<String>();
        topics.add(topic);
        topics.addAll(bookingSystemTopics.values());
        return List.copyOf(topics);
    }

    public List<String> getConsumerTopics() {
        var configured = consumer.getTopics().stream().filter(value -> value != null && !value.isBlank()).toList();
        return configured.isEmpty() ? getAllTopics() : configured;
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
        private boolean enabled = true;

        private List<String> topics = new ArrayList<>();

        @NotNull
        private Duration retryBackoff = Duration.ofSeconds(1);

        @Min(0)
        private long retryAttempts = 3;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getTopics() {
            return topics;
        }

        public void setTopics(List<String> topics) {
            this.topics = topics == null ? new ArrayList<>() : topics;
        }

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
