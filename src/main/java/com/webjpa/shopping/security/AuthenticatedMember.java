package com.webjpa.shopping.security;

import com.webjpa.shopping.domain.Member;
import com.webjpa.shopping.domain.MemberRole;

public record AuthenticatedMember(
        Long memberId,
        String email,
        String name,
        MemberRole role
) {
    public static AuthenticatedMember from(Member member) {
        return new AuthenticatedMember(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole()
        );
    }

    public boolean isAdmin() {
        return role == MemberRole.ADMIN;
    }
}
