package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.DeliveryStatus;
import com.webjpa.shopping.domain.DiscountType;
import com.webjpa.shopping.domain.OrderStatus;
import com.webjpa.shopping.domain.PaymentMethod;
import com.webjpa.shopping.domain.PaymentStatus;
import com.webjpa.shopping.domain.Product;
import com.webjpa.shopping.domain.PurchaseOrder;
import com.webjpa.shopping.dto.AddCartItemRequest;
import com.webjpa.shopping.dto.CheckoutRequest;
import com.webjpa.shopping.dto.CreateCouponRequest;
import com.webjpa.shopping.dto.CreateMemberRequest;
import com.webjpa.shopping.dto.CreateProductRequest;
import com.webjpa.shopping.dto.MemberResponse;
import com.webjpa.shopping.dto.OrderResponse;
import com.webjpa.shopping.dto.ProductResponse;
import com.webjpa.shopping.messaging.OrderNotificationEventPublisher;
import com.webjpa.shopping.messaging.OrderNotificationType;
import com.webjpa.shopping.search.ProductSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {

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

    @MockBean
    private ProductSearchIndexService productSearchIndexService;

    @MockBean
    private ProductSearchRepository productSearchRepository;

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private OrderNotificationEventPublisher orderNotificationEventPublisher;

    @BeforeEach
    void setUp() {
        reset(orderNotificationEventPublisher);
    }

    @Test
    void checkout_withCoupon_marksOrderPaidAndClearsCart() {
        MemberResponse member = createMember();
        ProductResponse product = createProduct("Wide Denim Pants", "MUSINSA STANDARD", "49900", 10);
        addCartItem(member.id(), product.id(), 2);
        createCoupon("SPRING10", DiscountType.PERCENTAGE, "10", "50000", "20000");

        OrderResponse order = orderService.checkout(new CheckoutRequest(
                member.id(),
                PaymentMethod.CARD,
                "ORDER-OK-001",
                "Seoul Seongsu 1-gil 10",
                "SPRING10"
        ));

        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        assertThat(order.deliveryStatus()).isEqualTo(DeliveryStatus.PREPARING);
        assertThat(order.totalAmount()).isEqualByComparingTo("99800");
        assertThat(order.discountAmount()).isEqualByComparingTo("9980");
        assertThat(order.payAmount()).isEqualByComparingTo("89820");
        assertThat(order.couponCode()).isEqualTo("SPRING10");
        assertThat(order.payment().status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(order.payment().transactionKey()).isNotBlank();
        assertThat(cartService.getCart(member.id()).items()).isEmpty();
        assertThat(productService.getEntity(product.id()).getStockQuantity()).isEqualTo(8);
        verify(orderNotificationEventPublisher).publish(eq(OrderNotificationType.ORDER_CONFIRMED), any(PurchaseOrder.class));
    }

    @Test
    void checkout_whenPaymentFails_marksOrderFailedAndKeepsCartAndStock() {
        MemberResponse member = createMember();
        ProductResponse product = createProduct("Half Zip Knit", "ZIGZAG SELECT", "30000", 5);
        addCartItem(member.id(), product.id(), 1);

        OrderResponse order = orderService.checkout(new CheckoutRequest(
                member.id(),
                PaymentMethod.KAKAO_PAY,
                "FAIL-ORDER-001",
                "Busan Haeundae-ro 200",
                null
        ));

        assertThat(order.status()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(order.deliveryStatus()).isEqualTo(DeliveryStatus.READY);
        assertThat(order.payAmount()).isEqualByComparingTo("30000");
        assertThat(order.payment().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(cartService.getCart(member.id()).items()).hasSize(1);
        assertThat(productService.getEntity(product.id()).getStockQuantity()).isEqualTo(5);
        verify(orderNotificationEventPublisher).publish(eq(OrderNotificationType.PAYMENT_FAILED), any(PurchaseOrder.class));
    }

    @Test
    void refund_restoresStockAndCancelsOrder() {
        MemberResponse member = createMember();
        ProductResponse product = createProduct("Runner Sneaker", "CITY LAB", "45000", 4);
        addCartItem(member.id(), product.id(), 2);

        OrderResponse paidOrder = orderService.checkout(new CheckoutRequest(
                member.id(),
                PaymentMethod.NAVER_PAY,
                "ORDER-OK-REFUND",
                "Incheon Songdo 55",
                null
        ));

        OrderResponse refundedOrder = orderService.refund(
                paidOrder.orderId(),
                "Customer changed mind",
                member.id(),
                false
        );

        assertThat(refundedOrder.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(refundedOrder.payment().status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refundedOrder.payment().refundedAt()).isNotNull();
        assertThat(productService.getEntity(product.id()).getStockQuantity()).isEqualTo(4);
        verify(orderNotificationEventPublisher).publish(eq(OrderNotificationType.ORDER_CONFIRMED), any(PurchaseOrder.class));
        verify(orderNotificationEventPublisher).publish(eq(OrderNotificationType.ORDER_REFUNDED), any(PurchaseOrder.class));
    }

    private MemberResponse createMember() {
        String token = UUID.randomUUID().toString().substring(0, 8);
        return memberService.create(new CreateMemberRequest(
                "tester-" + token,
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
