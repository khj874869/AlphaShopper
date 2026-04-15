package com.webjpa.shopping.controller;

import com.webjpa.shopping.dto.AddCartItemRequest;
import com.webjpa.shopping.dto.CartResponse;
import com.webjpa.shopping.security.AccessGuard;
import com.webjpa.shopping.security.AuthenticatedMember;
import com.webjpa.shopping.service.CartService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/{memberId}/cart")
public class CartController {

    private final CartService cartService;
    private final AccessGuard accessGuard;

    public CartController(CartService cartService, AccessGuard accessGuard) {
        this.cartService = cartService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public CartResponse getCart(@PathVariable Long memberId,
                                @AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
        accessGuard.requireMemberAccess(memberId, authenticatedMember);
        return cartService.getCart(memberId);
    }

    @PostMapping("/items")
    public CartResponse addItem(@PathVariable Long memberId,
                                @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
                                @Valid @RequestBody AddCartItemRequest request) {
        accessGuard.requireMemberAccess(memberId, authenticatedMember);
        return cartService.addItem(memberId, request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@PathVariable Long memberId,
                                   @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
                                   @PathVariable Long productId) {
        accessGuard.requireMemberAccess(memberId, authenticatedMember);
        return cartService.removeItem(memberId, productId);
    }

    @DeleteMapping("/items")
    public CartResponse clear(@PathVariable Long memberId,
                              @AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
        accessGuard.requireMemberAccess(memberId, authenticatedMember);
        return cartService.clear(memberId);
    }
}
