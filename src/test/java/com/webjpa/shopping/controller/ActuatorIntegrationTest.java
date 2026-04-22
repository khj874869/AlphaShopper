package com.webjpa.shopping.controller;

import com.webjpa.shopping.messaging.OrderNotificationMessage;
import com.webjpa.shopping.search.ProductSearchRepository;
import com.webjpa.shopping.service.ProductSearchIndexService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "management.endpoints.web.exposure.include=health,info,prometheus")
@AutoConfigureMockMvc
class ActuatorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductSearchIndexService productSearchIndexService;

    @MockitoBean
    private ProductSearchRepository productSearchRepository;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private KafkaTemplate<String, OrderNotificationMessage> kafkaTemplate;

    @Test
    void prometheusEndpoint_isExposedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_info")));
    }
}
