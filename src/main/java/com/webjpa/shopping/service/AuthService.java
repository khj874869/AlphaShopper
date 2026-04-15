package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.domain.Member;
import com.webjpa.shopping.dto.AuthResponse;
import com.webjpa.shopping.dto.CreateMemberRequest;
import com.webjpa.shopping.dto.LoginRequest;
import com.webjpa.shopping.dto.MemberResponse;
import com.webjpa.shopping.repository.MemberRepository;
import com.webjpa.shopping.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(MemberRepository memberRepository,
                       MemberService memberService,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.memberRepository = memberRepository;
        this.memberService = memberService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse register(CreateMemberRequest request) {
        MemberResponse memberResponse = memberService.create(request);
        Member member = memberService.getEntity(memberResponse.id());
        return new AuthResponse(jwtTokenProvider.createAccessToken(member), memberResponse);
    }

    public AuthResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is invalid."));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is invalid.");
        }

        return new AuthResponse(jwtTokenProvider.createAccessToken(member), MemberResponse.from(member));
    }

    public MemberResponse me(Long memberId) {
        return memberService.get(memberId);
    }
}
