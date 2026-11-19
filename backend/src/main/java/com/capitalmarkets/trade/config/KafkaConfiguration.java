package com.capitalmarkets.trade.config;

import com.capitalmarkets.trade.config.properties.TradeKafkaProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class KafkaConfiguration {
    @Bean
    String[] tradeKafkaTopics(TradeKafkaProperties properties) {
        return properties.getConsumerTopics().toArray(String[]::new);
    }

    @Bean
    KafkaAdmin.NewTopics tradeTopics(TradeKafkaProperties properties) {
        List<NewTopic> topics = new ArrayList<>();
        for (String topic : properties.getAllTopics()) {
            int partitions = properties.partitionsFor(topic);
            topics.add(TopicBuilder.name(topic)
                    .partitions(partitions)
                    .replicas(properties.getReplicas())
                    .build());
            topics.add(TopicBuilder.name(topic + properties.getDeadLetterSuffix())
                    .partitions(partitions)
                    .replicas(properties.getReplicas())
                    .build());
        }
        return new KafkaAdmin.NewTopics(topics.toArray(NewTopic[]::new));
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate,
            TradeKafkaProperties properties) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(
                        record.topic() + properties.getDeadLetterSuffix(), record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(
                properties.getConsumer().getRetryBackoff().toMillis(),
                properties.getConsumer().getRetryAttempts()));
    }
}
