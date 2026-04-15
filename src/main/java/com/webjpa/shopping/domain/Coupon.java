package com.webjpa.shopping.domain;

import com.webjpa.shopping.common.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DiscountType discountType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumOrderAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    protected Coupon() {
    }

    public Coupon(String code,
                  String name,
                  DiscountType discountType,
                  BigDecimal discountValue,
                  BigDecimal minimumOrderAmount,
                  BigDecimal maxDiscountAmount,
                  LocalDateTime expiresAt) {
        this.code = code;
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minimumOrderAmount = minimumOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.active = true;
        this.expiresAt = expiresAt;
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isUsableFor(orderAmount)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Coupon is not applicable to this order.");
        }

        BigDecimal discount = switch (discountType) {
            case FIXED_AMOUNT -> discountValue;
            case PERCENTAGE -> orderAmount.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
        };

        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }

        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount.max(BigDecimal.ZERO);
    }

    public boolean isUsableFor(BigDecimal orderAmount) {
        return active
                && expiresAt.isAfter(LocalDateTime.now())
                && orderAmount.compareTo(minimumOrderAmount) >= 0;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
