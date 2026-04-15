package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotNull(message = "회원 ID는 필수입니다.")
        Long memberId,

        @NotNull(message = "결제 수단은 필수입니다.")
        PaymentMethod paymentMethod,

        @NotBlank(message = "결제 참조값은 필수입니다.")
        @Size(max = 100, message = "결제 참조값은 100자 이하여야 합니다.")
        String paymentReference,

        @NotBlank(message = "배송지는 필수입니다.")
        @Size(max = 200, message = "배송지는 200자 이하여야 합니다.")
        String shippingAddress,

        @Size(max = 50, message = "쿠폰 코드는 50자 이하여야 합니다.")
        String couponCode
) {
}
