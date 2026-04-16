package com.webjpa.shopping.controller;

import com.webjpa.shopping.dto.CheckoutFailureReportRequest;
import com.webjpa.shopping.dto.CheckoutRequest;
import com.webjpa.shopping.dto.ConfirmCheckoutRequest;
import com.webjpa.shopping.dto.OrderResponse;
import com.webjpa.shopping.dto.PrepareCheckoutRequest;
import com.webjpa.shopping.dto.PrepareCheckoutResponse;
import com.webjpa.shopping.dto.RefundRequest;
import com.webjpa.shopping.dto.UpdateDeliveryRequest;
import com.webjpa.shopping.security.AccessGuard;
import com.webjpa.shopping.security.AuthenticatedMember;
import com.webjpa.shopping.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final AccessGuard accessGuard;

    public OrderController(OrderService orderService, AccessGuard accessGuard) {
        this.orderService = orderService;
        this.accessGuard = accessGuard;
    }

    @PostMapping("/checkout/prepare")
    public PrepareCheckoutResponse prepareCheckout(@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
                                                   @Valid @RequestBody PrepareCheckoutRequest request) {
        accessGuard.requireMemberAccess(request.memberId(), authenticatedMember);
        return orderService.prepareCheckout(request);
    }

    @PostMapping("/checkout/confirm")
    public OrderResponse confirmCheckout(@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
                                         @Valid @RequestBody ConfirmCheckoutRequest request) {
        accessGuard.requireMemberAccess(request.memberId(), authenticatedMember);
        return orderService.confirmCheckout(request, authenticatedMember.memberId(), authenticatedMember.isAdmin());
    }

    @PostMapping("/checkout/fail")
    public void markCheckoutFailed(@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
                                   @Valid @RequestBody CheckoutFailureReportRequest request) {
        accessGuard.requireMemberAccess(request.memberId(), authenticatedMember);
        orderService.markCheckoutFailed(request, authenticatedMember.memberId(), authenticatedMember.isAdmin());
    }

    @PostMapping("/checkout")
    public OrderResponse checkout(@AuthenticationPrincipal AuthenticatedMember authenticatedMember,
                                  @Valid @RequestBody CheckoutRequest request) {
        accessGuard.requireMemberAccess(request.memberId(), authenticatedMember);
        return orderService.checkout(request);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId,
                                  @AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
        return orderService.getOrder(orderId, authenticatedMember.memberId(), authenticatedMember.isAdmin());
    }

    @PostMapping("/{orderId}/refund")
    public OrderResponse refund(@PathVariable Long orderId,
                                @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
                                @Valid @RequestBody RefundRequest request) {
        return orderService.refund(orderId, request.reason(), authenticatedMember.memberId(), authenticatedMember.isAdmin());
    }

    @PatchMapping("/{orderId}/delivery")
    public OrderResponse updateDelivery(@PathVariable Long orderId, @Valid @RequestBody UpdateDeliveryRequest request) {
        return orderService.updateDelivery(orderId, request.deliveryStatus(), request.trackingNumber());
    }
}
