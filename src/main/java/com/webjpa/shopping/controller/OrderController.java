package com.webjpa.shopping.controller;

import com.webjpa.shopping.dto.CheckoutRequest;
import com.webjpa.shopping.dto.OrderResponse;
import com.webjpa.shopping.dto.RefundRequest;
import com.webjpa.shopping.dto.UpdateDeliveryRequest;
import com.webjpa.shopping.service.OrderService;
import jakarta.validation.Valid;
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

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public OrderResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(request);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId);
    }

    @PostMapping("/{orderId}/refund")
    public OrderResponse refund(@PathVariable Long orderId, @Valid @RequestBody RefundRequest request) {
        return orderService.refund(orderId, request.reason());
    }

    @PatchMapping("/{orderId}/delivery")
    public OrderResponse updateDelivery(@PathVariable Long orderId, @Valid @RequestBody UpdateDeliveryRequest request) {
        return orderService.updateDelivery(orderId, request.deliveryStatus(), request.trackingNumber());
    }
}
