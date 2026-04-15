package com.webjpa.shopping.security;

import com.webjpa.shopping.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AccessGuard {

    public void requireMemberAccess(Long targetMemberId, AuthenticatedMember authenticatedMember) {
        if (authenticatedMember == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        if (authenticatedMember.isAdmin()) {
            return;
        }

        if (!authenticatedMember.memberId().equals(targetMemberId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this member resource.");
        }
    }

    public void requireAdmin(AuthenticatedMember authenticatedMember) {
        if (authenticatedMember == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        if (!authenticatedMember.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin permission is required.");
        }
    }
}
