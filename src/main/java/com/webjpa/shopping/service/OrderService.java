package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.domain.Cart;
import com.webjpa.shopping.domain.CartItem;
import com.webjpa.shopping.domain.Coupon;
import com.webjpa.shopping.domain.DeliveryStatus;
import com.webjpa.shopping.domain.Member;
import com.webjpa.shopping.domain.OrderStatus;
import com.webjpa.shopping.domain.Payment;
import com.webjpa.shopping.domain.Product;
import com.webjpa.shopping.domain.PurchaseOrder;
import com.webjpa.shopping.dto.CheckoutRequest;
import com.webjpa.shopping.dto.OrderResponse;
import com.webjpa.shopping.dto.OrderSummaryResponse;
import com.webjpa.shopping.messaging.OrderNotificationEventPublisher;
import com.webjpa.shopping.messaging.OrderNotificationType;
import com.webjpa.shopping.repository.PurchaseOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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

    public OrderService(MemberService memberService,
                        CartService cartService,
                        ProductService productService,
                        CouponService couponService,
                        PurchaseOrderRepository purchaseOrderRepository,
                        PaymentGateway paymentGateway,
                        OrderNotificationEventPublisher orderNotificationEventPublisher) {
        this.memberService = memberService;
        this.cartService = cartService;
        this.productService = productService;
        this.couponService = couponService;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.paymentGateway = paymentGateway;
        this.orderNotificationEventPublisher = orderNotificationEventPublisher;
    }

    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        Member member = memberService.getEntity(request.memberId());
        Cart cart = cartService.getDetailEntity(member.getId());

        if (cart.getItems().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cart is empty.");
        }

        validateStock(cart);

        PurchaseOrder order = PurchaseOrder.create(member, request.shippingAddress());
        for (CartItem cartItem : cart.getItems()) {
            order.addItem(cartItem.getProduct(), cartItem.getQuantity(), cartItem.getUnitPrice());
        }

        applyCouponIfPresent(order, request.couponCode());

        Payment payment = Payment.ready(order, request.paymentMethod(), order.getPayAmount(), request.paymentReference());
        order.attachPayment(payment);
        purchaseOrderRepository.save(order);

        PaymentGateway.PaymentResult paymentResult = paymentGateway.authorize(
                request.paymentMethod(),
                order.getPayAmount(),
                request.paymentReference(),
                order.getId()
        );

        if (!paymentResult.approved()) {
            order.markPaymentFailed();
            payment.fail(paymentResult.message());
            orderNotificationEventPublisher.publish(OrderNotificationType.PAYMENT_FAILED, order);
            return OrderResponse.from(order);
        }

        for (CartItem cartItem : cart.getItems()) {
            cartItem.getProduct().decreaseStock(cartItem.getQuantity());
        }

        payment.approve(paymentResult.transactionKey());
        order.markPaid();
        order.prepareDelivery();
        cart.clear();
        orderNotificationEventPublisher.publish(OrderNotificationType.ORDER_CONFIRMED, order);
        return OrderResponse.from(order);
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
        boolean refunded = paymentGateway.refund(payment.getTransactionKey(), payment.getAmount());
        if (!refunded) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Refund failed.");
        }

        order.getItems().forEach(item -> {
            Product product = productService.getEntity(item.getProductId());
            product.increaseStock(item.getQuantity());
        });

        payment.refund(reason);
        order.cancel();
        orderNotificationEventPublisher.publish(OrderNotificationType.ORDER_REFUNDED, order);
        return OrderResponse.from(order);
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

    private PurchaseOrder getAccessibleOrder(Long orderId, Long actorMemberId, boolean admin) {
        PurchaseOrder order = getDetailOrder(orderId);
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
}
