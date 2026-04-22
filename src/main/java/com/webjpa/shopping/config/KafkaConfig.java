package com.webjpa.shopping.config;

import com.webjpa.shopping.logging.LogValues;
import com.webjpa.shopping.messaging.OrderNotificationMessage;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Bean
    @ConditionalOnMissingBean(KafkaAdmin.class)
    KafkaAdmin kafkaAdmin(@Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        return new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
    }

    @Bean
    @ConditionalOnMissingBean(ProducerFactory.class)
    ProducerFactory<String, OrderNotificationMessage> orderNotificationProducerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        properties.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    @ConditionalOnMissingBean(KafkaTemplate.class)
    KafkaTemplate<String, OrderNotificationMessage> kafkaTemplate(
            ProducerFactory<String, OrderNotificationMessage> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    @ConditionalOnMissingBean(ConsumerFactory.class)
    ConsumerFactory<String, OrderNotificationMessage> orderNotificationConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id:zigzag-notification-consumer}") String groupId,
            @Value("${spring.kafka.consumer.auto-offset-reset:earliest}") String autoOffsetReset) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        properties.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.webjpa.shopping.messaging");
        properties.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, OrderNotificationMessage.class.getName());
        properties.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, OrderNotificationMessage> kafkaListenerContainerFactory(
            ConsumerFactory<String, OrderNotificationMessage> consumerFactory,
            DefaultErrorHandler orderNotificationErrorHandler,
            @Value("${spring.kafka.listener.auto-startup:true}") boolean autoStartup) {
        ConcurrentKafkaListenerContainerFactory<String, OrderNotificationMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(orderNotificationErrorHandler);
        factory.setAutoStartup(autoStartup);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean(DefaultErrorHandler.class)
    DefaultErrorHandler orderNotificationErrorHandler(
            KafkaOperations<String, OrderNotificationMessage> kafkaOperations,
            @Value("${app.kafka.topics.order-notifications-dlt}") String deadLetterTopic,
            @Value("${app.kafka.retry.max-attempts:3}") int maxAttempts,
            @Value("${app.kafka.retry.backoff-ms:1000}") long backoffMs) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, ex) -> deadLetterTopicPartition(record, deadLetterTopic, ex)
        );
        long retryAttempts = Math.max(0, maxAttempts - 1L);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(backoffMs, retryAttempts));
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> log.warn(
                "event=order_notification.kafka.retry topic={} partition={} offset={} deliveryAttempt={} errorType={} error={}",
                record.topic(),
                record.partition(),
                record.offset(),
                deliveryAttempt,
                ex.getClass().getSimpleName(),
                LogValues.safe(ex.getMessage())
        ));
        return errorHandler;
    }

    private TopicPartition deadLetterTopicPartition(ConsumerRecord<?, ?> record, String deadLetterTopic, Exception ex) {
        log.warn("event=order_notification.kafka.dlt topic={} partition={} offset={} deadLetterTopic={} errorType={} error={}",
                record.topic(),
                record.partition(),
                record.offset(),
                deadLetterTopic,
                ex.getClass().getSimpleName(),
                LogValues.safe(ex.getMessage()),
                ex);
        return new TopicPartition(deadLetterTopic, record.partition());
    }
}
