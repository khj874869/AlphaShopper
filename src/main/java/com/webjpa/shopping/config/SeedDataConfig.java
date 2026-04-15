package com.webjpa.shopping.config;

import com.webjpa.shopping.domain.Coupon;
import com.webjpa.shopping.domain.DiscountType;
import com.webjpa.shopping.domain.Member;
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
                               CouponRepository couponRepository) {
        return args -> {
            if (memberRepository.count() == 0) {
                memberRepository.save(Member.create("Zigzag Buyer A", "buyer1@zigzag.local"));
                memberRepository.save(Member.create("Zigzag Buyer B", "buyer2@zigzag.local"));
            }

            if (productRepository.count() == 0) {
                productRepository.save(new Product(
                        "Semi Wide Denim Pants",
                        "Mellow Fit",
                        BigDecimal.valueOf(42000),
                        50,
                        "Daily basic denim pants."
                ));
                productRepository.save(new Product(
                        "Square Neck Knit",
                        "Urban Muse",
                        BigDecimal.valueOf(35000),
                        40,
                        "Slim knit for mid-season styling."
                ));
                productRepository.save(new Product(
                        "Mini Shoulder Bag",
                        "Noir Studio",
                        BigDecimal.valueOf(68000),
                        25,
                        "Lightweight shoulder bag for daily looks."
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
