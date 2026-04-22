package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.DiscountType;
import com.webjpa.shopping.domain.OrderStatus;
import com.webjpa.shopping.domain.PaymentMethod;
import com.webjpa.shopping.domain.PaymentStatus;
import com.webjpa.shopping.domain.PurchaseOrder;
import com.webjpa.shopping.dto.AddCartItemRequest;
import com.webjpa.shopping.dto.ConfirmCheckoutRequest;
import com.webjpa.shopping.dto.CreateCouponRequest;
import com.webjpa.shopping.dto.CreateMemberRequest;
import com.webjpa.shopping.dto.CreateProductRequest;
import com.webjpa.shopping.dto.MemberResponse;
import com.webjpa.shopping.dto.PrepareCheckoutRequest;
import com.webjpa.shopping.dto.PrepareCheckoutResponse;
import com.webjpa.shopping.dto.ProductResponse;
import com.webjpa.shopping.messaging.OrderNotificationEventPublisher;
import com.webjpa.shopping.messaging.OrderNotificationMessage;
import com.webjpa.shopping.messaging.OrderNotificationType;
import com.webjpa.shopping.repository.PurchaseOrderRepository;
import com.webjpa.shopping.search.ProductSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "app.payment.provider=toss")
@Transactional
class TossCheckoutIntegrationTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TossWebhookService tossWebhookService;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

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
    }

    @Test
    void prepareAndConfirmCheckout_marksOrderPaidAndClearsCart() {
        MemberResponse member = createMember();
        ProductResponse product = createProduct("Straight Denim", "ALPHA", "59000", 6);
        addCartItem(member.id(), product.id(), 1);
        createCoupon("TOSS3000", DiscountType.FIXED_AMOUNT, "3000", "30000", "3000");

        when(paymentGateway.startCheckout(eq(PaymentMethod.CARD), eq(new BigDecimal("56000")), any(), any()))
                .thenReturn(new PaymentGateway.CheckoutStartResult("https://pay.example/checkout"));
        when(paymentGateway.authorize(eq(PaymentMethod.CARD), eq(new BigDecimal("56000")), eq("payment-key-001"), any()))
                .thenReturn(new PaymentGateway.PaymentResult(true, "payment-key-001", "DONE"));

        PrepareCheckoutResponse prepared = orderService.prepareCheckout(new PrepareCheckoutRequest(
                member.id(),
                PaymentMethod.CARD,
                "Seoul Seongsu 11",
                "TOSS3000"
        ));

        var confirmed = orderService.confirmCheckout(new ConfirmCheckoutRequest(
                member.id(),
                prepared.providerOrderId(),
                "payment-key-001",
                new BigDecimal("56000")
        ), member.id(), false);

        assertThat(prepared.provider()).isEqualTo("toss");
        assertThat(prepared.checkoutUrl()).isEqualTo("https://pay.example/checkout");
        assertThat(confirmed.status()).isEqualTo(OrderStatus.PAID);
        assertThat(confirmed.payment().status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(confirmed.payment().transactionKey()).isEqualTo("payment-key-001");
        assertThat(cartService.getCart(member.id()).items()).isEmpty();
        assertThat(productService.getEntity(product.id()).getStockQuantity()).isEqualTo(5);
        verify(orderNotificationEventPublisher).publish(eq(OrderNotificationType.ORDER_CONFIRMED), any(PurchaseOrder.class));
    }

    @Test
    void webhookDone_beforeBrowserConfirm_reconcilesPreparedOrderAndConfirmBecomesIdempotent() {
        MemberResponse member = createMember();
        ProductResponse product = createProduct("Crop Jacket", "ALPHA", "42000", 4);
        addCartItem(member.id(), product.id(), 1);

        when(paymentGateway.startCheckout(eq(PaymentMethod.NAVER_PAY), eq(new BigDecimal("42000")), any(), any()))
                .thenReturn(new PaymentGateway.CheckoutStartResult("https://pay.example/naver"));

        PrepareCheckoutResponse prepared = orderService.prepareCheckout(new PrepareCheckoutRequest(
                member.id(),
                PaymentMethod.NAVER_PAY,
                "Busan Marine City 2",
                null
        ));

        reset(paymentGateway, orderNotificationEventPublisher);
        when(paymentGateway.getPayment("payment-key-002"))
                .thenReturn(new PaymentGateway.PaymentLookupResult(
                        "payment-key-002",
                        prepared.providerOrderId(),
                        "DONE",
                        new BigDecimal("42000"),
                        "Toss payment status=DONE"
                ));

        tossWebhookService.handle(Map.of(
                "eventType", "PAYMENT_STATUS_CHANGED",
                "data", Map.of(
                        "orderId", prepared.providerOrderId(),
                        "paymentKey", "payment-key-002",
                        "status", "DONE",
                        "totalAmount", 42000
                )
        ));

        PurchaseOrder order = getOrder(prepared.providerOrderId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPayment().getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(order.getPayment().getTransactionKey()).isEqualTo("payment-key-002");
        assertThat(productService.getEntity(product.id()).getStockQuantity()).isEqualTo(3);

        var confirmed = orderService.confirmCheckout(new ConfirmCheckoutRequest(
                member.id(),
                prepared.providerOrderId(),
                "payment-key-002",
                new BigDecimal("42000")
        ), member.id(), false);

        assertThat(confirmed.status()).isEqualTo(OrderStatus.PAID);
        assertThat(productService.getEntity(product.id()).getStockQuantity()).isEqualTo(3);
        verify(paymentGateway, never()).authorize(any(), any(), any(), any());
        verify(orderNotificationEventPublisher).publish(eq(OrderNotificationType.ORDER_CONFIRMED), any(PurchaseOrder.class));
    }

    @Test
    void webhookCanceled_afterApproval_restoresStockAndMarksOrderCancelled() {
        MemberResponse member = createMember();
        ProductResponse product = createProduct("Waffle Tee", "ALPHA", "31000", 7);
        addCartItem(member.id(), product.id(), 2);

        when(paymentGateway.startCheckout(eq(PaymentMethod.CARD), eq(new BigDecimal("62000")), any(), any()))
                .thenReturn(new PaymentGateway.CheckoutStartResult("https://pay.example/card"));
        when(paymentGateway.authorize(eq(PaymentMethod.CARD), eq(new BigDecimal("62000")), eq("payment-key-003"), any()))
                .thenReturn(new PaymentGateway.PaymentResult(true, "payment-key-003", "DONE"));

        PrepareCheckoutResponse prepared = orderService.prepareCheckout(new PrepareCheckoutRequest(
                member.id(),
                PaymentMethod.CARD,
                "Incheon Songdo 88",
                null
        ));

        orderService.confirmCheckout(new ConfirmCheckoutRequest(
                member.id(),
                prepared.providerOrderId(),
                "payment-key-003",
                new BigDecimal("62000")
        ), member.id(), false);

        reset(orderNotificationEventPublisher);
        when(paymentGateway.getPayment("payment-key-003"))
                .thenReturn(new PaymentGateway.PaymentLookupResult(
                        "payment-key-003",
                        prepared.providerOrderId(),
                        "CANCELED",
                        new BigDecimal("62000"),
                        "Operator refund"
                ));

        tossWebhookService.handle(Map.of(
                "eventType", "PAYMENT_CANCELED",
                "data", Map.of(
                        "orderId", prepared.providerOrderId(),
                        "paymentKey", "payment-key-003",
                        "status", "CANCELED",
                        "cancels", List.of(Map.of("cancelReason", "Operator refund"))
                )
        ));

        PurchaseOrder order = getOrder(prepared.providerOrderId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPayment().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(order.getPayment().getRefundReason()).isEqualTo("Operator refund");
        assertThat(productService.getEntity(product.id()).getStockQuantity()).isEqualTo(7);
        verify(orderNotificationEventPublisher).publish(eq(OrderNotificationType.ORDER_REFUNDED), any(PurchaseOrder.class));
    }

    @Test
    void webhookAborted_marksPreparedOrderFailedAndKeepsCart() {
        MemberResponse member = createMember();
        ProductResponse product = createProduct("City Skirt", "ALPHA", "27000", 8);
        addCartItem(member.id(), product.id(), 1);

        when(paymentGateway.startCheckout(eq(PaymentMethod.CARD), eq(new BigDecimal("27000")), any(), any()))
                .thenReturn(new PaymentGateway.CheckoutStartResult("https://pay.example/card"));

        PrepareCheckoutResponse prepared = orderService.prepareCheckout(new PrepareCheckoutRequest(
                member.id(),
                PaymentMethod.CARD,
                "Daegu Center 3",
                null
        ));
        when(paymentGateway.getPayment("payment-key-004"))
                .thenReturn(new PaymentGateway.PaymentLookupResult(
                        "payment-key-004",
                        prepared.providerOrderId(),
                        "ABORTED",
                        new BigDecimal("27000"),
                        "USER_CANCEL: Buyer closed the window"
                ));

        tossWebhookService.handle(Map.of(
                "eventType", "PAYMENT_STATUS_CHANGED",
                "data", Map.of(
                        "orderId", prepared.providerOrderId(),
                        "paymentKey", "payment-key-004",
                        "status", "ABORTED",
                        "failure", Map.of(
                                "code", "USER_CANCEL",
                                "message", "Buyer closed the window"
                        )
                )
        ));

        PurchaseOrder order = getOrder(prepared.providerOrderId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(order.getPayment().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getPayment().getFailedReason()).isEqualTo("USER_CANCEL: Buyer closed the window");
        assertThat(cartService.getCart(member.id()).items()).hasSize(1);
        assertThat(productService.getEntity(product.id()).getStockQuantity()).isEqualTo(8);
        verify(orderNotificationEventPublisher).publish(eq(OrderNotificationType.PAYMENT_FAILED), any(PurchaseOrder.class));
    }

    @Test
    void webhookWithMismatchedLookupOrder_doesNotChangePreparedOrder() {
        MemberResponse member = createMember();
        ProductResponse product = createProduct("Oxford Shirt", "ALPHA", "35000", 5);
        addCartItem(member.id(), product.id(), 1);

        when(paymentGateway.startCheckout(eq(PaymentMethod.CARD), eq(new BigDecimal("35000")), any(), any()))
                .thenReturn(new PaymentGateway.CheckoutStartResult("https://pay.example/card"));

        PrepareCheckoutResponse prepared = orderService.prepareCheckout(new PrepareCheckoutRequest(
                member.id(),
                PaymentMethod.CARD,
                "Seoul Mapo 7",
                null
        ));
        when(paymentGateway.getPayment("payment-key-005"))
                .thenReturn(new PaymentGateway.PaymentLookupResult(
                        "payment-key-005",
                        "order_from_toss_lookup",
                        "DONE",
                        new BigDecimal("35000"),
                        "Toss payment status=DONE"
                ));

        tossWebhookService.handle(Map.of(
                "eventType", "PAYMENT_STATUS_CHANGED",
                "data", Map.of(
                        "orderId", prepared.providerOrderId(),
                        "paymentKey", "payment-key-005",
                        "status", "DONE",
                        "totalAmount", 35000
                )
        ));

        PurchaseOrder order = getOrder(prepared.providerOrderId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getPayment().getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(productService.getEntity(product.id()).getStockQuantity()).isEqualTo(5);
        verify(orderNotificationEventPublisher, never()).publish(any(), any(PurchaseOrder.class));
    }

    private PurchaseOrder getOrder(String providerOrderId) {
        return purchaseOrderRepository.findDetailByProviderOrderId(providerOrderId)
                .orElseThrow();
    }

    private MemberResponse createMember() {
        String token = UUID.randomUUID().toString().substring(0, 8);
        return memberService.create(new CreateMemberRequest(
                "toss-" + token,
                token + "@alphashopper.local",
                "testpass1234"
        ));
    }

    private ProductResponse createProduct(String name, String brand, String price, int stockQuantity) {
        return productService.create(new CreateProductRequest(
                name,
                brand,
                new BigDecimal(price),
                stockQuantity,
                name + " description",
                "/catalog/test-product.svg"
        ));
    }

    private void addCartItem(Long memberId, Long productId, int quantity) {
        cartService.addItem(memberId, new AddCartItemRequest(productId, quantity));
    }

    private void createCoupon(String code,
                              DiscountType discountType,
                              String discountValue,
                              String minimumOrderAmount,
                              String maxDiscountAmount) {
        couponService.create(new CreateCouponRequest(
                code,
                code + " campaign",
                discountType,
                new BigDecimal(discountValue),
                new BigDecimal(minimumOrderAmount),
                new BigDecimal(maxDiscountAmount),
                LocalDateTime.now().plusDays(30)
        ));
    }
}
