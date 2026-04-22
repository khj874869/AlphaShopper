package com.webjpa.shopping.controller;

import com.webjpa.shopping.domain.DltReplayAuditStatus;
import com.webjpa.shopping.domain.MemberRole;
import com.webjpa.shopping.dto.CreateMemberRequest;
import com.webjpa.shopping.dto.DltReplayAuditResponse;
import com.webjpa.shopping.dto.DltReplayResponse;
import com.webjpa.shopping.messaging.OrderNotificationMessage;
import com.webjpa.shopping.service.MemberService;
import com.webjpa.shopping.service.OrderNotificationDltReplayAuditService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminKafkaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberService memberService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private KafkaTemplate<String, OrderNotificationMessage> kafkaTemplate;

    @MockitoBean
    private OrderNotificationDltReplayAuditService orderNotificationDltReplayAuditService;

    @Test
    void replayOrderNotificationDlt_requiresAdminRole() throws Exception {
        memberService.create(new CreateMemberRequest(
                "Regular User",
                "regular-user@alphashopper.local",
                "userpass123"
        ));
        MvcResult loginResult = login("regular-user@alphashopper.local", "userpass123");
        MvcResult csrfResult = csrf();

        mockMvc.perform(post("/api/admin/kafka/order-notifications/dlt/replay")
                        .cookie(loginResult.getResponse().getCookies())
                        .cookie(csrfCookie(csrfResult))
                        .header("X-XSRF-TOKEN", csrfToken(csrfResult)))
                .andExpect(status().isForbidden());
    }

    @Test
    void replayOrderNotificationDlt_allowsAdminAndReturnsReplayResult() throws Exception {
        memberService.create(new CreateMemberRequest(
                "Admin User",
                "admin-user@alphashopper.local",
                "adminpass123"
        ), MemberRole.ADMIN);
        when(orderNotificationDltReplayAuditService.replay(eq(25), eq(false), any())).thenReturn(new DltReplayResponse(
                "order-notifications.DLT",
                "order-notifications",
                "dlt-replay",
                false,
                25,
                2,
                2,
                2,
                0,
                null,
                42L
        ));
        MvcResult loginResult = login("admin-user@alphashopper.local", "adminpass123");
        MvcResult csrfResult = csrf();

        mockMvc.perform(post("/api/admin/kafka/order-notifications/dlt/replay")
                        .param("maxMessages", "25")
                        .param("dryRun", "false")
                        .cookie(loginResult.getResponse().getCookies())
                        .cookie(csrfCookie(csrfResult))
                        .header("X-XSRF-TOKEN", csrfToken(csrfResult)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceTopic").value("order-notifications.DLT"))
                .andExpect(jsonPath("$.targetTopic").value("order-notifications"))
                .andExpect(jsonPath("$.auditId").value(42))
                .andExpect(jsonPath("$.replayedMessages").value(2))
                .andExpect(jsonPath("$.committedMessages").value(2));

        verify(orderNotificationDltReplayAuditService).replay(eq(25), eq(false), any());
    }

    @Test
    void listOrderNotificationDltReplayAudits_allowsAdminAndReturnsAuditRows() throws Exception {
        memberService.create(new CreateMemberRequest(
                "Audit Admin",
                "audit-admin@alphashopper.local",
                "adminpass123"
        ), MemberRole.ADMIN);
        when(orderNotificationDltReplayAuditService.listRecentAudits(10)).thenReturn(List.of(new DltReplayAuditResponse(
                42L,
                1L,
                "audit-admin@alphashopper.local",
                "Audit Admin",
                "order-notifications.DLT",
                "order-notifications",
                "dlt-replay",
                true,
                10,
                3,
                0,
                0,
                0,
                DltReplayAuditStatus.SUCCEEDED,
                LocalDateTime.of(2026, 4, 22, 16, 0),
                LocalDateTime.of(2026, 4, 22, 16, 1),
                null
        )));
        MvcResult loginResult = login("audit-admin@alphashopper.local", "adminpass123");

        mockMvc.perform(get("/api/admin/kafka/order-notifications/dlt/replay/audits")
                        .param("limit", "10")
                        .cookie(loginResult.getResponse().getCookies()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].adminEmail").value("audit-admin@alphashopper.local"))
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$[0].dryRun").value(true))
                .andExpect(jsonPath("$[0].inspectedMessages").value(3));

        verify(orderNotificationDltReplayAuditService).listRecentAudits(10);
    }

    private MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private MvcResult csrf() throws Exception {
        return mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private Cookie csrfCookie(MvcResult csrfResult) {
        return csrfResult.getResponse().getCookie("XSRF-TOKEN");
    }

    private String csrfToken(MvcResult csrfResult) {
        Cookie cookie = csrfCookie(csrfResult);
        return cookie == null ? "" : cookie.getValue();
    }
}
