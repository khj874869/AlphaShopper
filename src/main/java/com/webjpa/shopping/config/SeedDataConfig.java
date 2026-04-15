package com.webjpa.shopping.config;

import com.webjpa.shopping.domain.Coupon;
import com.webjpa.shopping.domain.DiscountType;
import com.webjpa.shopping.domain.Member;
import com.webjpa.shopping.domain.MemberRole;
import com.webjpa.shopping.domain.Product;
import com.webjpa.shopping.repository.CouponRepository;
import com.webjpa.shopping.repository.MemberRepository;
import com.webjpa.shopping.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class SeedDataConfig {

    @Bean
    CommandLineRunner seedData(MemberRepository memberRepository,
                               ProductRepository productRepository,
                               CouponRepository couponRepository,
                               org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        return args -> {
            if (memberRepository.count() == 0) {
                memberRepository.save(Member.create(
                        "Zigzag Buyer A",
                        "buyer1@zigzag.local",
                        passwordEncoder.encode("buyer1234"),
                        MemberRole.USER
                ));
                memberRepository.save(Member.create(
                        "Zigzag Buyer B",
                        "buyer2@zigzag.local",
                        passwordEncoder.encode("buyer1234"),
                        MemberRole.USER
                ));
                memberRepository.save(Member.create(
                        "Alpha Admin",
                        "admin@alphashopper.local",
                        passwordEncoder.encode("admin1234"),
                        MemberRole.ADMIN
                ));
            }

            if (productRepository.count() == 0) {
                productRepository.save(new Product(
                        "Semi Wide Denim Pants",
                        "Mellow Fit",
                        BigDecimal.valueOf(42000),
                        50,
                        "Daily basic denim pants.",
                        "/catalog/semi-wide-denim.svg"
                ));
                productRepository.save(new Product(
                        "Square Neck Knit",
                        "Urban Muse",
                        BigDecimal.valueOf(35000),
                        40,
                        "Slim knit for mid-season styling.",
                        "/catalog/square-neck-knit.svg"
                ));
                productRepository.save(new Product(
                        "Mini Shoulder Bag",
                        "Noir Studio",
                        BigDecimal.valueOf(68000),
                        25,
                        "Lightweight shoulder bag for daily looks.",
                        "/catalog/mini-shoulder-bag.svg"
                ));
                productRepository.save(new Product(
                        "Cropped Leather Jacket",
                        "Studio Layer",
                        BigDecimal.valueOf(119000),
                        18,
                        "Short leather jacket with a sharp city silhouette.",
                        "/catalog/cropped-leather-jacket.svg"
                ));
                productRepository.save(new Product(
                        "Soft Pleats Skirt",
                        "Atelier Day",
                        BigDecimal.valueOf(54000),
                        32,
                        "Swing pleats skirt for clean weekday styling.",
                        "/catalog/soft-pleats-skirt.svg"
                ));
                productRepository.save(new Product(
                        "Track Sole Loafer",
                        "Mono Lane",
                        BigDecimal.valueOf(79000),
                        22,
                        "Chunky sole loafer for polished casual outfits.",
                        "/catalog/track-sole-loafer.svg"
                ));
            }

            if (couponRepository.count() == 0) {
                couponRepository.save(new Coupon(
                        "WELCOME10",
                        "Welcome 10 Percent Coupon",
                        DiscountType.PERCENTAGE,
                        BigDecimal.valueOf(10),
                        BigDecimal.valueOf(30000),
                        BigDecimal.valueOf(15000),
                        LocalDateTime.now().plusMonths(3)
                ));
                couponRepository.save(new Coupon(
                        "SPRING5000",
                        "Spring 5000 Won Coupon",
                        DiscountType.FIXED_AMOUNT,
                        BigDecimal.valueOf(5000),
                        BigDecimal.valueOf(20000),
                        null,
                        LocalDateTime.now().plusMonths(1)
                ));
            }
        };
    }
}
