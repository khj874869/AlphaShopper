package com.webjpa.shopping;

import com.webjpa.shopping.search.ProductSearchRepository;
import com.webjpa.shopping.service.ProductSearchIndexService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.webjpa.shopping.messaging.OrderNotificationMessage;

@SpringBootTest
class ShoppingApplicationTests {

    @MockitoBean
    private ProductSearchIndexService productSearchIndexService;

    @MockitoBean
    private ProductSearchRepository productSearchRepository;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private KafkaTemplate<String, OrderNotificationMessage> kafkaTemplate;

    @Test
    void contextLoads() {
    }
}
