package com.webjpa.shopping.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaTopicConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(KafkaTopicConfig.class)
            .withPropertyValues(
                    "app.kafka.topics.order-notifications=orders",
                    "app.kafka.topics.order-notifications-dlt=orders.DLT"
            );

    @Test
    void createsNotificationAndDeadLetterTopicsWhenAutoCreateIsEnabled() {
        contextRunner
                .withPropertyValues("app.kafka.auto-create-topics=true")
                .run(context -> {
                    Map<String, NewTopic> topics = context.getBeansOfType(NewTopic.class);

                    assertThat(topics).hasSize(2);
                    assertThat(topics.get("orderNotificationsTopic").name()).isEqualTo("orders");
                    assertThat(topics.get("orderNotificationsDltTopic").name()).isEqualTo("orders.DLT");
                });
    }

    @Test
    void doesNotCreateTopicsWhenAutoCreateIsDisabled() {
        contextRunner
                .withPropertyValues("app.kafka.auto-create-topics=false")
                .run(context -> assertThat(context.getBeansOfType(NewTopic.class)).isEmpty());
    }
}
