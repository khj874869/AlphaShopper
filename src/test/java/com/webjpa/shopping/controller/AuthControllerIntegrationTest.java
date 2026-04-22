package com.webjpa.shopping.controller;

import com.webjpa.shopping.dto.CreateMemberRequest;
import com.webjpa.shopping.messaging.OrderNotificationMessage;
import com.webjpa.shopping.service.MemberService;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberService memberService;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private KafkaTemplate<String, OrderNotificationMessage> kafkaTemplate;

    @Test
    void login_setsHttpOnlyCookieAndMeAcceptsCookieAuthentication() throws Exception {
        memberService.create(new CreateMemberRequest(
                "Cookie User",
                "cookie-user@alphashopper.local",
                "cookiepass123"
        ));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "cookie-user@alphashopper.local",
                                  "password": "cookiepass123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("alphashopper_access_token"))
                .andExpect(cookie().httpOnly("alphashopper_access_token", true))
                .andExpect(cookie().secure("alphashopper_access_token", false))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.member.email").value("cookie-user@alphashopper.local"))
                .andReturn();

        mockMvc.perform(get("/api/auth/me")
                        .cookie(loginResult.getResponse().getCookies()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cookie-user@alphashopper.local"))
                .andExpect(jsonPath("$.name").value("Cookie User"));
    }

    @Test
    void logout_clearsAccessTokenCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("alphashopper_access_token", 0))
                .andExpect(cookie().httpOnly("alphashopper_access_token", true))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
    }

    @Test
    void csrfEndpoint_issuesReadableXsrfToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void authenticatedMutation_withoutCsrfToken_isRejected() throws Exception {
        memberService.create(new CreateMemberRequest(
                "Csrf User",
                "csrf-user@alphashopper.local",
                "cookiepass123"
        ));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "csrf-user@alphashopper.local",
                                  "password": "cookiepass123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(post("/api/orders/checkout")
                        .cookie(loginResult.getResponse().getCookies())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
