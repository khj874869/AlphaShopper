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

@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=health,info,prometheus",
        "app.management.prometheus.public-access=false",
        "app.management.prometheus.allowed-ip-ranges=127.0.0.1/32"
})
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
    void prometheusEndpoint_allowsAllowlistedIp() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_info")));
    }

    @Test
    void prometheusEndpoint_rejectsNonAllowlistedIp() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.10");
                            return request;
                        }))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthProbeEndpoints_areExposedForOrchestrators() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"status\":\"UP\"")));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"status\":\"UP\"")));
    }
}
