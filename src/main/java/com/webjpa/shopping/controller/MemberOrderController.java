package com.webjpa.shopping.controller;

import com.webjpa.shopping.dto.OrderSummaryResponse;
import com.webjpa.shopping.security.AccessGuard;
import com.webjpa.shopping.security.AuthenticatedMember;
import com.webjpa.shopping.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members/{memberId}/orders")
public class MemberOrderController {

    private final OrderService orderService;
    private final AccessGuard accessGuard;

    public MemberOrderController(OrderService orderService, AccessGuard accessGuard) {
        this.orderService = orderService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public List<OrderSummaryResponse> getOrders(@PathVariable Long memberId,
                                                @AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
        accessGuard.requireMemberAccess(memberId, authenticatedMember);
        return orderService.getMemberOrders(memberId);
    }
}
