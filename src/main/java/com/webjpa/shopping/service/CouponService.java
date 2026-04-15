package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.domain.Coupon;
import com.webjpa.shopping.dto.CouponResponse;
import com.webjpa.shopping.dto.CreateCouponRequest;
import com.webjpa.shopping.repository.CouponRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional
    public CouponResponse create(CreateCouponRequest request) {
        if (couponRepository.existsByCode(request.code())) {
            throw new ApiException(HttpStatus.CONFLICT, "Coupon code already exists.");
        }

        Coupon coupon = new Coupon(
                request.code(),
                request.name(),
                request.discountType(),
                request.discountValue(),
                request.minimumOrderAmount(),
                request.maxDiscountAmount(),
                request.expiresAt()
        );

        return CouponResponse.from(couponRepository.save(coupon));
    }

    public List<CouponResponse> getAll() {
        return couponRepository.findAll().stream()
                .map(CouponResponse::from)
                .toList();
    }

    public Coupon getUsableCoupon(String code) {
        return couponRepository.findByCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Coupon not found. code=" + code));
    }
}
