package com.webjpa.shopping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundRequest(
        @NotBlank(message = "환불 사유는 필수입니다.")
        @Size(max = 300, message = "환불 사유는 300자 이하여야 합니다.")
        String reason
) {
}
