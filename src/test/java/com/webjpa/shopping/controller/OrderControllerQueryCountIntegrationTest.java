package com.webjpa.shopping.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjpa.shopping.domain.PaymentMethod;
import com.webjpa.shopping.dto.AddCartItemRequest;
import com.webjpa.shopping.dto.CreateMemberRequest;
import com.webjpa.shopping.dto.CreateProductRequest;
import com.webjpa.shopping.dto.MemberResponse;
import com.webjpa.shopping.dto.PrepareCheckoutResponse;
import com.webjpa.shopping.messaging.OrderNotificationEventPublisher;
import com.webjpa.shopping.messaging.OrderNotificationMessage;
import com.webjpa.shopping.search.ProductSearchRepository;
import com.webjpa.shopping.service.CartService;
import com.webjpa.shopping.service.MemberService;
import com.webjpa.shopping.service.PaymentGateway;
import com.webjpa.shopping.service.ProductSearchIndexService;
import com.webjpa.shopping.service.ProductService;
import jakarta.servlet.http.Cookie;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.BeforeEach;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.payment.provider=toss",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector="
                + "com.webjpa.shopping.controller.OrderControllerQueryCountIntegrationTest$ProductSelectStatementInspector"
})
@AutoConfigureMockMvc
@Transactional
class OrderControllerQueryCountIntegrationTest {

    private static final List<String> inspectedSql = new CopyOnWriteArrayList<>();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @MockitoBean
    private ProductSearchIndexService productSearchIndexService;

    @MockitoBean
    private ProductSearchRepository productSearchRepository;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private OrderNotificationEventPublisher orderNotificationEventPublisher;

    @MockitoBean
    private KafkaTemplate<String, OrderNotificationMessage> kafkaTemplate;

    @BeforeEach
    void setUp() {
        reset(paymentGateway, orderNotificationEventPublisher);
        inspectedSql.clear();
    }

    @Test
    void confirmCheckoutApi_withMultipleProducts_usesBulkProductSelects() throws Exception {
        MemberResponse member = createMember();
        BigDecimal amount = addProductsToCart(member.id(), "12000", "18000", "24000", "30000");
        AuthSession authSession = login(member.email(), "testpass1234");
        CsrfSession csrfSession = csrfSession();

        when(paymentGateway.startCheckout(eq(PaymentMethod.CARD), eq(amount), any(), any()))
                .thenReturn(new PaymentGateway.CheckoutStartResult("https://pay.example/checkout"));

        MvcResult prepareResult = mockMvc.perform(post("/api/orders/checkout/prepare")
                        .cookie(authSession.accessTokenCookie(), csrfSession.cookie())
                        .header(csrfSession.headerName(), csrfSession.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": %d,
                                  "paymentMethod": "CARD",
                                  "shippingAddress": "Seoul Gangnam 20"
                                }
                                """.formatted(member.id())))
                .andExpect(status().isOk())
                .andReturn();

        PrepareCheckoutResponse prepared = objectMapper.readValue(
                prepareResult.getResponse().getContentAsString(),
                PrepareCheckoutResponse.class
        );

        reset(paymentGateway, orderNotificationEventPublisher);
        when(paymentGateway.authorize(eq(PaymentMethod.CARD), eq(amount), eq("payment-key-api-query"), any()))
                .thenReturn(new PaymentGateway.PaymentResult(true, "payment-key-api-query", "DONE"));

        inspectedSql.clear();
        mockMvc.perform(post("/api/orders/checkout/confirm")
                        .cookie(authSession.accessTokenCookie(), csrfSession.cookie())
                        .header(csrfSession.headerName(), csrfSession.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": %d,
                                  "providerOrderId": "%s",
                                  "paymentKey": "payment-key-api-query",
                                  "amount": %s
                                }
                                """.formatted(member.id(), prepared.providerOrderId(), amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        assertThat(productSelectSql())
                .as("confirm checkout should keep product stock lookups bulked for validation and approval")
                .hasSize(2);
    }

    private MemberResponse createMember() {
        String token = UUID.randomUUID().toString().substring(0, 8);
        return memberService.create(new CreateMemberRequest(
                "api-query-" + token,
                token + "@alphashopper.local",
                "testpass1234"
        ));
    }

    private BigDecimal addProductsToCart(Long memberId, String... prices) {
        BigDecimal amount = BigDecimal.ZERO;
        for (int i = 0; i < prices.length; i++) {
            BigDecimal price = new BigDecimal(prices[i]);
            Long productId = productService.create(new CreateProductRequest(
                    "API Query Product " + i,
                    "ALPHA",
                    price,
                    10,
                    "API query count product",
                    "/catalog/test-product.svg"
            )).id();

            cartService.addItem(memberId, new AddCartItemRequest(productId, 1));
            amount = amount.add(price);
        }
        return amount;
    }

    private AuthSession login(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        return new AuthSession(loginResult.getResponse().getCookie("alphashopper_access_token"));
    }

    private CsrfSession csrfSession() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        var responseBody = objectMapper.readTree(csrfResult.getResponse().getContentAsString());
        return new CsrfSession(
                csrfResult.getResponse().getCookie("XSRF-TOKEN"),
                responseBody.get("headerName").asText(),
                responseBody.get("token").asText()
        );
    }

    private static List<String> productSelectSql() {
        return inspectedSql.stream()
                .filter(OrderControllerQueryCountIntegrationTest::isProductSelect)
                .toList();
    }

    private static boolean isProductSelect(String sql) {
        String normalized = sql.toLowerCase(Locale.ROOT)
                .replace('"', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.startsWith("select ") && normalized.contains(" from product ");
    }

    private record AuthSession(Cookie accessTokenCookie) {
    }

    private record CsrfSession(Cookie cookie, String headerName, String token) {
    }

    public static final class ProductSelectStatementInspector implements StatementInspector {

        @Override
        public String inspect(String sql) {
            inspectedSql.add(sql);
            return sql;
        }
    }
}
