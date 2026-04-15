package com.webjpa.shopping.controller;

import com.webjpa.shopping.dto.OrderSummaryResponse;
import com.webjpa.shopping.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members/{memberId}/orders")
public class MemberOrderController {

    private final OrderService orderService;

    public MemberOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderSummaryResponse> getOrders(@PathVariable Long memberId) {
        return orderService.getMemberOrders(memberId);
    }
}
