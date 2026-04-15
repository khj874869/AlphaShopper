package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.Coupon;

public record CouponResponse(
        Long id,
        String code,
        String name
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(coupon.getId(), coupon.getCode(), coupon.getName());
    }
}

