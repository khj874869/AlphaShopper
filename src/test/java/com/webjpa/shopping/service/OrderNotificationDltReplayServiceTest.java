package com.webjpa.shopping.service;

import com.webjpa.shopping.dto.DltReplayResponse;
import com.webjpa.shopping.messaging.OrderNotificationMessage;
import com.webjpa.shopping.messaging.OrderNotificationType;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class OrderNotificationDltReplayServiceTest {

    private ConsumerFactory<String, OrderNotificationMessage> consumerFactory;
    private KafkaTemplate<String, OrderNotificationMessage> kafkaTemplate;
    private Consumer<String, OrderNotificationMessage> consumer;
    private OrderNotificationDltReplayService service;

    @BeforeEach
    void setUp() {
        consumerFactory = mock(ConsumerFactory.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        consumer = mock(Consumer.class);
        service = new OrderNotificationDltReplayService(
                consumerFactory,
                kafkaTemplate,
                "orders.DLT",
                "orders",
                "replay-group",
                100,
                1000
        );

        when(consumerFactory.createConsumer(eq("replay-group"), eq("dlt-replay"), anyString(), any(Properties.class)))
                .thenReturn(consumer);
    }

    @Test
    void replay_resendsDltMessageAndCommitsOnlyAfterSuccessfulSend() {
        OrderNotificationMessage message = message();
        TopicPartition topicPartition = new TopicPartition("orders.DLT", 0);
        ConsumerRecord<String, OrderNotificationMessage> record =
                new ConsumerRecord<>("orders.DLT", 0, 4L, "101", message);
        when(consumer.poll(Duration.ofMillis(100)))
                .thenReturn(new ConsumerRecords<>(
                        Map.of(topicPartition, List.of(record)),
                        Map.of(topicPartition, new OffsetAndMetadata(5L))
                ));
        when(kafkaTemplate.send("orders", "101", message))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        DltReplayResponse response = service.replay(10, false);

        assertThat(response.replayedMessages()).isEqualTo(1);
        assertThat(response.committedMessages()).isEqualTo(1);
        assertThat(response.failedMessages()).isZero();
        verify(consumer).subscribe(List.of("orders.DLT"));
        verify(kafkaTemplate).send("orders", "101", message);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<TopicPartition, OffsetAndMetadata>> commitCaptor = ArgumentCaptor.forClass(Map.class);
        verify(consumer).commitSync(commitCaptor.capture());
        assertThat(commitCaptor.getValue()).containsKey(topicPartition);
        assertThat(commitCaptor.getValue().get(topicPartition).offset()).isEqualTo(5L);
    }

    @Test
    void replay_whenDryRun_doesNotSendOrCommit() {
        OrderNotificationMessage message = message();
        TopicPartition topicPartition = new TopicPartition("orders.DLT", 0);
        ConsumerRecord<String, OrderNotificationMessage> record =
                new ConsumerRecord<>("orders.DLT", 0, 4L, "101", message);
        when(consumer.poll(Duration.ofMillis(100)))
                .thenReturn(new ConsumerRecords<>(
                        Map.of(topicPartition, List.of(record)),
                        Map.of(topicPartition, new OffsetAndMetadata(5L))
                ));

        DltReplayResponse response = service.replay(10, true);

        assertThat(response.dryRun()).isTrue();
        assertThat(response.inspectedMessages()).isEqualTo(1);
        assertThat(response.replayedMessages()).isZero();
        assertThat(response.committedMessages()).isZero();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(OrderNotificationMessage.class));
        verify(consumer, never()).commitSync(any(Map.class));
    }

    private OrderNotificationMessage message() {
        return new OrderNotificationMessage(
                OrderNotificationType.ORDER_CONFIRMED,
                101L,
                7L,
                "buyer@example.com",
                "Buyer",
                "Product",
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                new BigDecimal("10000"),
                "Seoul",
                null,
                LocalDateTime.now(),
                "request-123"
        );
    }
}
