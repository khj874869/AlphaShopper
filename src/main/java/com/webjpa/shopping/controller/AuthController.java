package com.webjpa.shopping.controller;

import com.webjpa.shopping.dto.AuthResponse;
import com.webjpa.shopping.dto.CreateMemberRequest;
import com.webjpa.shopping.dto.LoginRequest;
import com.webjpa.shopping.dto.MemberResponse;
import com.webjpa.shopping.security.AuthenticatedMember;
import com.webjpa.shopping.security.AuthCookieService;
import com.webjpa.shopping.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    public AuthController(AuthService authService, AuthCookieService authCookieService) {
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody CreateMemberRequest request) {
        AuthService.AuthSession authSession = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, authCookieService.createAccessTokenCookie(authSession.accessToken()).toString())
                .body(authSession.response());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthSession authSession = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.createAccessTokenCookie(authSession.accessToken()).toString())
                .body(authSession.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.clearAccessTokenCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public MemberResponse me(@AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
        return authService.me(authenticatedMember.memberId());
    }
}
