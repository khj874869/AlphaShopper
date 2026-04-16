package com.webjpa.shopping.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {

    private final JwtTokenProvider jwtTokenProvider;
    private final String cookieName;
    private final boolean secure;
    private final String sameSite;

    public AuthCookieService(JwtTokenProvider jwtTokenProvider,
                             @Value("${app.auth.cookie.name:alphashopper_access_token}") String cookieName,
                             @Value("${app.auth.cookie.secure:false}") boolean secure,
                             @Value("${app.auth.cookie.same-site:Lax}") String sameSite) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return ResponseCookie.from(cookieName, accessToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(jwtTokenProvider.getAccessTokenExpiration())
                .build();
    }

    public ResponseCookie clearAccessTokenCookie() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();
    }

    public String getCookieName() {
        return cookieName;
    }
}
