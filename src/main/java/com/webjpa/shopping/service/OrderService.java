package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.domain.Cart;
import com.webjpa.shopping.domain.CartItem;
import com.webjpa.shopping.domain.Coupon;
import com.webjpa.shopping.domain.DeliveryStatus;
import com.webjpa.shopping.domain.Member;
import com.webjpa.shopping.domain.OrderItem;
import com.webjpa.shopping.domain.OrderStatus;
import com.webjpa.shopping.domain.Payment;
import com.webjpa.shopping.domain.PaymentMethod;
import com.webjpa.shopping.domain.PaymentStatus;
import com.webjpa.shopping.domain.Product;
import com.webjpa.shopping.domain.PurchaseOrder;
import com.webjpa.shopping.dto.CheckoutFailureReportRequest;
import com.webjpa.shopping.dto.CheckoutRequest;
import com.webjpa.shopping.dto.ConfirmCheckoutRequest;
import com.webjpa.shopping.dto.OrderResponse;
import com.webjpa.shopping.dto.OrderSummaryResponse;
import com.webjpa.shopping.dto.PrepareCheckoutRequest;
import com.webjpa.shopping.dto.PrepareCheckoutResponse;
import com.webjpa.shopping.messaging.OrderNotificationEventPublisher;
import com.webjpa.shopping.messaging.OrderNotificationType;
import com.webjpa.shopping.repository.PurchaseOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final MemberService memberService;
    private final CartService cartService;
    private final ProductService productService;
    private final CouponService couponService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PaymentGateway paymentGateway;
    private final OrderNotificationEventPublisher orderNotificationEventPublisher;
    private final String paymentProvider;

    public OrderService(MemberService memberService,
                        CartService cartService,
                        ProductService productService,
                        CouponService couponService,
                        PurchaseOrderRepository purchaseOrderRepository,
                        PaymentGateway paymentGateway,
                        OrderNotificationEventPublisher orderNotificationEventPublisher,
                        @Value("${app.payment.provider:fake}") String paymentProvider) {
        this.memberService = memberService;
        this.cartService = cartService;
        this.productService = productService;
        this.couponService = couponService;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.paymentGateway = paymentGateway;
        this.orderNotificationEventPublisher = orderNotificationEventPublisher;
        this.paymentProvider = paymentProvider;
    }

    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        if (usesHostedCheckout()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Direct checkout is disabled for the configured payment provider.");
        }

        PurchaseOrder order = createOrderFromCart(
                request.memberId(),
                request.paymentMethod(),
                request.shippingAddress(),
                request.couponCode(),
                request.paymentReference()
        );

        PaymentGateway.PaymentResult paymentResult = paymentGateway.authorize(
                request.paymentMethod(),
                order.getPayAmount(),
                request.paymentReference(),
                Long.toString(order.getId())
        );

        if (!paymentResult.approved()) {
            failPreparedOrder(order, paymentResult.message());
            return OrderResponse.from(order);
        }

        completeApprovedOrder(order, paymentResult.transactionKey(), order.getMember().getId());
        return OrderResponse.from(order);
    }

    @Transactional
    public PrepareCheckoutResponse prepareCheckout(PrepareCheckoutRequest request) {
        requireHostedCheckoutProvider();

        PurchaseOrder order = createOrderFromCart(
                request.memberId(),
                request.paymentMethod(),
                request.shippingAddress(),
                request.couponCode(),
                generateProviderOrderId()
        );

        PaymentGateway.CheckoutStartResult checkoutStartResult = paymentGateway.startCheckout(
                request.paymentMethod(),
                order.getPayAmount(),
                order.getPayment().getPaymentReference(),
                buildOrderName(order)
        );

        return new PrepareCheckoutResponse(
                paymentProvider,
                order.getPayment().getPaymentReference(),
                checkoutStartResult.checkoutUrl(),
                order.getPayAmount()
        );
    }

    @Transactional
    public OrderResponse confirmCheckout(ConfirmCheckoutRequest request, Long actorMemberId, boolean admin) {
        requireHostedCheckoutProvider();

        PurchaseOrder order = getAccessibleOrderByProviderOrderId(request.providerOrderId(), actorMemberId, admin);
        if (isApprovedOrder(order, request.paymentKey())) {
            return OrderResponse.from(order);
        }

        validatePreparedOrder(order, request.amount());

        PaymentGateway.PaymentResult paymentResult = paymentGateway.authorize(
                order.getPayment().getMethod(),
                order.getPayAmount(),
                request.paymentKey(),
                request.providerOrderId()
        );

        if (!paymentResult.approved()) {
            failPreparedOrder(order, paymentResult.message());
            return OrderResponse.from(order);
        }

        completeApprovedOrder(order, paymentResult.transactionKey(), order.getMember().getId());
        return OrderResponse.from(order);
    }

    @Transactional
    public void markCheckoutFailed(CheckoutFailureReportRequest request, Long actorMemberId, boolean admin) {
        requireHostedCheckoutProvider();

        PurchaseOrder order = getAccessibleOrderByProviderOrderId(request.providerOrderId(), actorMemberId, admin);
        failPreparedOrder(order, buildFailureReason(request.errorCode(), request.errorMessage()));
    }

    public OrderResponse getOrder(Long orderId, Long actorMemberId, boolean admin) {
        return OrderResponse.from(getAccessibleOrder(orderId, actorMemberId, admin));
    }

    public List<OrderSummaryResponse> getMemberOrders(Long memberId) {
        memberService.getEntity(memberId);
        return purchaseOrderRepository.findSummariesByMemberId(memberId).stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }

    @Transactional
    public OrderResponse refund(Long orderId, String reason, Long actorMemberId, boolean admin) {
        PurchaseOrder order = getAccessibleOrder(orderId, actorMemberId, admin);

        if (order.getStatus() != OrderStatus.PAID) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only paid orders can be refunded.");
        }

        Payment payment = order.getPayment();
        boolean refunded = paymentGateway.refund(payment.getTransactionKey(), payment.getAmount(), reason);
        if (!refunded) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Refund failed.");
        }

        applyRefund(order, reason);
        return OrderResponse.from(order);
    }

    @Transactional
    public void reconcileApprovedPayment(String providerOrderId, String transactionKey, BigDecimal approvedAmount) {
        requireHostedCheckoutProvider();

        if (transactionKey == null || transactionKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment key is required to reconcile an approved payment.");
        }

        PurchaseOrder order = getDetailOrderByProviderOrderId(providerOrderId);
        if (isApprovedOrder(order, transactionKey)) {
            return;
        }

        if (order.getPayment().getStatus() == PaymentStatus.REFUNDED || order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }

        validatePreparedOrder(order, approvedAmount);
        completeApprovedOrder(order, transactionKey, order.getMember().getId());
    }

    @Transactional
    public void reconcileCanceledPayment(String providerOrderId, String transactionKey, String reason) {
        requireHostedCheckoutProvider();

        PurchaseOrder order = getDetailOrderByProviderOrderId(providerOrderId);
        if (order.getStatus() == OrderStatus.CANCELLED && order.getPayment().getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        if (order.getStatus() == OrderStatus.CREATED && order.getPayment().getStatus() == PaymentStatus.READY) {
            failPreparedOrder(order, reason);
            return;
        }

        if (!isApprovedOrder(order, transactionKey)) {
            return;
        }

        applyRefund(order, reason);
    }

    @Transactional
    public void reconcileFailedPayment(String providerOrderId, String reason) {
        requireHostedCheckoutProvider();

        PurchaseOrder order = getDetailOrderByProviderOrderId(providerOrderId);
        failPreparedOrder(order, reason);
    }

    @Transactional
    public OrderResponse updateDelivery(Long orderId, DeliveryStatus deliveryStatus, String trackingNumber) {
        PurchaseOrder order = getDetailOrder(orderId);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cancelled orders cannot change delivery status.");
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only paid orders can move through delivery flow.");
        }

        switch (deliveryStatus) {
            case READY -> {
            }
            case PREPARING -> order.prepareDelivery();
            case SHIPPED -> {
                if (trackingNumber == null || trackingNumber.isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Tracking number is required when shipping.");
                }
                order.ship(trackingNumber);
                orderNotificationEventPublisher.publish(OrderNotificationType.ORDER_SHIPPED, order);
            }
            case DELIVERED -> {
                order.deliver();
                orderNotificationEventPublisher.publish(OrderNotificationType.ORDER_DELIVERED, order);
            }
        }

        return OrderResponse.from(order);
    }

    private PurchaseOrder getDetailOrder(Long orderId) {
        return purchaseOrderRepository.findDetailById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found. id=" + orderId));
    }

    private PurchaseOrder getDetailOrderByProviderOrderId(String providerOrderId) {
        return purchaseOrderRepository.findDetailByProviderOrderId(providerOrderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found. providerOrderId=" + providerOrderId));
    }

    private PurchaseOrder getAccessibleOrder(Long orderId, Long actorMemberId, boolean admin) {
        PurchaseOrder order = getDetailOrder(orderId);
        if (!admin && !order.getMember().getId().equals(actorMemberId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this order.");
        }
        return order;
    }

    private PurchaseOrder getAccessibleOrderByProviderOrderId(String providerOrderId, Long actorMemberId, boolean admin) {
        PurchaseOrder order = getDetailOrderByProviderOrderId(providerOrderId);
        if (!admin && !order.getMember().getId().equals(actorMemberId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this order.");
        }
        return order;
    }

    private void validateStock(Cart cart) {
        for (CartItem item : cart.getItems()) {
            if (!item.getProduct().hasEnoughStock(item.getQuantity())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Insufficient stock. productId=" + item.getProduct().getId());
            }
        }
    }

    private void applyCouponIfPresent(PurchaseOrder order, String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            order.applyCoupon(null, null, BigDecimal.ZERO);
            return;
        }

        Coupon coupon = couponService.getUsableCoupon(couponCode);
        BigDecimal discountAmount = coupon.calculateDiscount(order.getTotalAmount());
        order.applyCoupon(coupon.getCode(), coupon.getName(), discountAmount);
    }

    private PurchaseOrder createOrderFromCart(Long memberId,
                                              PaymentMethod paymentMethod,
                                              String shippingAddress,
                                              String couponCode,
                                              String paymentReference) {
        Member member = memberService.getEntity(memberId);
        Cart cart = cartService.getDetailEntity(member.getId());

        if (cart.getItems().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cart is empty.");
        }

        validateStock(cart);

        PurchaseOrder order = PurchaseOrder.create(member, shippingAddress);
        for (CartItem cartItem : cart.getItems()) {
            order.addItem(cartItem.getProduct(), cartItem.getQuantity(), cartItem.getUnitPrice());
        }

        applyCouponIfPresent(order, couponCode);

        Payment payment = Payment.ready(order, paymentMethod, order.getPayAmount(), paymentReference);
        order.attachPayment(payment);
        return purchaseOrderRepository.save(order);
    }

    private void validatePreparedOrder(PurchaseOrder order, BigDecimal requestedAmount) {
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ApiException(HttpStatus.CONFLICT, "Order is not awaiting payment confirmation.");
        }

        if (order.getPayment().getStatus() != PaymentStatus.READY) {
            throw new ApiException(HttpStatus.CONFLICT, "Payment is not awaiting confirmation.");
        }

        if (requestedAmount != null && order.getPayAmount().compareTo(requestedAmount) != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment amount does not match the prepared order.");
        }

        validateStock(order);
    }

    private void validateStock(PurchaseOrder order) {
        Map<Long, Product> productsById = getOrderItemProducts(order);
        for (OrderItem item : order.getItems()) {
            Product product = productsById.get(item.getProductId());
            if (!product.hasEnoughStock(item.getQuantity())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Insufficient stock. productId=" + item.getProductId());
            }
        }
    }

    private void completeApprovedOrder(PurchaseOrder order, String transactionKey, Long memberId) {
        Map<Long, Product> productsById = getOrderItemProducts(order);
        for (OrderItem item : order.getItems()) {
            Product product = productsById.get(item.getProductId());
            product.decreaseStock(item.getQuantity());
        }

        order.getPayment().approve(transactionKey);
        order.markPaid();
        order.prepareDelivery();
        cartService.clear(memberId);
        orderNotificationEventPublisher.publish(OrderNotificationType.ORDER_CONFIRMED, order);
    }

    private void failPreparedOrder(PurchaseOrder order, String reason) {
        if (order.getStatus() != OrderStatus.CREATED || order.getPayment().getStatus() != PaymentStatus.READY) {
            return;
        }

        order.markPaymentFailed();
        order.getPayment().fail(reason);
        orderNotificationEventPublisher.publish(OrderNotificationType.PAYMENT_FAILED, order);
    }

    private void applyRefund(PurchaseOrder order, String reason) {
        restoreStock(order);
        order.getPayment().refund(reason);
        order.cancel();
        orderNotificationEventPublisher.publish(OrderNotificationType.ORDER_REFUNDED, order);
    }

    private void restoreStock(PurchaseOrder order) {
        Map<Long, Product> productsById = getOrderItemProducts(order);
        order.getItems().forEach(item -> {
            Product product = productsById.get(item.getProductId());
            product.increaseStock(item.getQuantity());
        });
    }

    private Map<Long, Product> getOrderItemProducts(PurchaseOrder order) {
        return productService.getEntitiesByIds(order.getItems().stream()
                .map(OrderItem::getProductId)
                .toList());
    }

    private boolean usesHostedCheckout() {
        return "toss".equalsIgnoreCase(paymentProvider);
    }

    private void requireHostedCheckoutProvider() {
        if (!usesHostedCheckout()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Hosted checkout is not enabled for the configured payment provider.");
        }
    }

    private String buildOrderName(PurchaseOrder order) {
        if (order.getItems().isEmpty()) {
            return "AlphaShopper order";
        }

        OrderItem firstItem = order.getItems().get(0);
        if (order.getItems().size() == 1) {
            return firstItem.getProductName();
        }

        return firstItem.getProductName() + " and " + (order.getItems().size() - 1) + " more";
    }

    private String generateProviderOrderId() {
        return "order_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String buildFailureReason(String errorCode, String errorMessage) {
        if (errorCode != null && !errorCode.isBlank() && errorMessage != null && !errorMessage.isBlank()) {
            return errorCode + ": " + errorMessage;
        }

        if (errorMessage != null && !errorMessage.isBlank()) {
            return errorMessage;
        }

        if (errorCode != null && !errorCode.isBlank()) {
            return errorCode;
        }

        return "Payment failed before authorization.";
    }

    private boolean isApprovedOrder(PurchaseOrder order, String transactionKey) {
        return order.getStatus() == OrderStatus.PAID
                && order.getPayment().getStatus() == PaymentStatus.APPROVED
                && Objects.equals(order.getPayment().getTransactionKey(), transactionKey);
    }
}
