package com.capitalmarkets.trade.config;

import com.capitalmarkets.trade.config.properties.TradeKafkaProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfiguration {
    @Bean
    NewTopic tradesTopic(TradeKafkaProperties properties) {
        // Provision one shared intraday topic; use the message key for partition routing.
        return TopicBuilder.name(properties.getTopic())
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicas())
                .build();
    }

    @Bean
    NewTopic deadLetterTopic(TradeKafkaProperties properties) {
        return TopicBuilder.name(properties.getTopic() + properties.getDeadLetterSuffix())
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicas())
                .build();
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate,
            TradeKafkaProperties properties) {
        String topic = properties.getTopic();
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(topic + properties.getDeadLetterSuffix(), record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(
                properties.getConsumer().getRetryBackoff().toMillis(),
                properties.getConsumer().getRetryAttempts()));
    }
}
