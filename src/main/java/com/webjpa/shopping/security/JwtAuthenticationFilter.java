package com.webjpa.shopping.security;

import com.webjpa.shopping.domain.Member;
import com.webjpa.shopping.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final AuthCookieService authCookieService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   MemberRepository memberRepository,
                                   AuthCookieService authCookieService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.memberRepository = memberRepository;
        this.authCookieService = authCookieService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && !token.isBlank()) {
            if (jwtTokenProvider.isValid(token)) {
                memberRepository.findByEmail(jwtTokenProvider.getEmail(token))
                        .ifPresent(this::authenticate);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }

        Cookie cookie = WebUtils.getCookie(request, authCookieService.getCookieName());
        if (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }

        return null;
    }

    private void authenticate(Member member) {
        AuthenticatedMember principal = AuthenticatedMember.from(member);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
