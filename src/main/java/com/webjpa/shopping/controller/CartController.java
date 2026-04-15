package com.webjpa.shopping.controller;

import com.webjpa.shopping.dto.AddCartItemRequest;
import com.webjpa.shopping.dto.CartResponse;
import com.webjpa.shopping.service.CartService;
import jakarta.validation.Valid;
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

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(@PathVariable Long memberId) {
        return cartService.getCart(memberId);
    }

    @PostMapping("/items")
    public CartResponse addItem(@PathVariable Long memberId, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(memberId, request);
    }
}

