package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.domain.Cart;
import com.webjpa.shopping.domain.Member;
import com.webjpa.shopping.domain.Product;
import com.webjpa.shopping.dto.AddCartItemRequest;
import com.webjpa.shopping.dto.CartResponse;
import com.webjpa.shopping.repository.CartRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final MemberService memberService;
    private final ProductService productService;

    public CartService(CartRepository cartRepository, MemberService memberService, ProductService productService) {
        this.cartRepository = cartRepository;
        this.memberService = memberService;
        this.productService = productService;
    }

    public Cart getDetailEntity(Long memberId) {
        return cartRepository.findDetailByMemberId(memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "장바구니를 찾을 수 없습니다. memberId=" + memberId));
    }

    public CartResponse getCart(Long memberId) {
        memberService.getEntity(memberId);
        return CartResponse.from(getDetailEntity(memberId));
    }

    @Transactional
    public CartResponse addItem(Long memberId, AddCartItemRequest request) {
        Member member = memberService.getEntity(memberId);
        Product product = productService.getEntity(request.productId());
        Cart cart = getDetailEntity(member.getId());
        cart.addItem(product, request.quantity());
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse removeItem(Long memberId, Long productId) {
        memberService.getEntity(memberId);
        Cart cart = getDetailEntity(memberId);
        cart.removeItem(productId);
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse clear(Long memberId) {
        memberService.getEntity(memberId);
        Cart cart = getDetailEntity(memberId);
        cart.clear();
        return CartResponse.from(cart);
    }
}
