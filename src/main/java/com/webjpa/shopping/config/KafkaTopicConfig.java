package com.webjpa.shopping.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class KafkaTopicConfig {

    @Bean
    @ConditionalOnProperty(name = "app.kafka.auto-create-topics", havingValue = "true", matchIfMissing = true)
    NewTopic orderNotificationsTopic(@Value("${app.kafka.topics.order-notifications}") String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.auto-create-topics", havingValue = "true", matchIfMissing = true)
    NewTopic orderNotificationsDltTopic(@Value("${app.kafka.topics.order-notifications-dlt}") String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
