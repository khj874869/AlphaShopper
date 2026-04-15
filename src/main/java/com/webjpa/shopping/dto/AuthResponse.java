package com.webjpa.shopping.dto;

public record AuthResponse(
        String accessToken,
        MemberResponse member
) {
}
