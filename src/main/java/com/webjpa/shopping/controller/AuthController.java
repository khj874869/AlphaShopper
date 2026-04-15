package com.webjpa.shopping.controller;

import com.webjpa.shopping.dto.AuthResponse;
import com.webjpa.shopping.dto.CreateMemberRequest;
import com.webjpa.shopping.dto.LoginRequest;
import com.webjpa.shopping.dto.MemberResponse;
import com.webjpa.shopping.security.AuthenticatedMember;
import com.webjpa.shopping.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody CreateMemberRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public MemberResponse me(@AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
        return authService.me(authenticatedMember.memberId());
    }
}
