package com.webjpa.shopping.controller;

import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.dto.AiChatResponse;
import com.webjpa.shopping.dto.CreateMemberRequest;
import com.webjpa.shopping.messaging.OrderNotificationMessage;
import com.webjpa.shopping.service.MemberService;
import com.webjpa.shopping.service.ShoppingAiService;
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

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.ai.allow-anonymous=false")
@AutoConfigureMockMvc
@Transactional
class AiControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberService memberService;

    @MockitoBean
    private ShoppingAiService shoppingAiService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private KafkaTemplate<String, OrderNotificationMessage> kafkaTemplate;

    @Test
    void aiEndpoint_rejectsAnonymousRequestsWhenDisabled() throws Exception {
        Cookie xsrfCookie = fetchXsrfCookie();

        mockMvc.perform(post("/api/ai/chat")
                        .cookie(xsrfCookie)
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "recommend",
                                  "maxRecommendations": 3
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aiEndpoint_allowsAuthenticatedRequestsWhenAnonymousAccessIsDisabled() throws Exception {
        memberService.create(new CreateMemberRequest(
                "AI User",
                "ai-user@alphashopper.local",
                "cookiepass123"
        ));
        when(shoppingAiService.chat("recommend", null, 3))
                .thenReturn(new AiChatResponse(
                        "recommendation result",
                        false,
                        AiRecommendationSource.DATABASE,
                        AiRecommendationBucket.DEFAULT,
                        List.of()
                ));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ai-user@alphashopper.local",
                                  "password": "cookiepass123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Cookie xsrfCookie = fetchXsrfCookie(loginResult.getResponse().getCookies());
        Cookie[] cookies = concat(loginResult.getResponse().getCookies(), xsrfCookie);

        mockMvc.perform(post("/api/ai/chat")
                        .cookie(cookies)
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "recommend",
                                  "maxRecommendations": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("recommendation result"));

        verify(shoppingAiService).chat("recommend", null, 3);
    }

    private Cookie fetchXsrfCookie(Cookie... cookies) throws Exception {
        var requestBuilder = get("/api/auth/csrf");
        if (cookies.length > 0) {
            requestBuilder.cookie(cookies);
        }

        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn();
        return result.getResponse().getCookie("XSRF-TOKEN");
    }

    private Cookie[] concat(Cookie[] cookies, Cookie extraCookie) {
        return Arrays.stream(cookies)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        list -> {
                            list.add(extraCookie);
                            return list.toArray(Cookie[]::new);
                        }
                ));
    }
}
