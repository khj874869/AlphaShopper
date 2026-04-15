package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.domain.Member;
import com.webjpa.shopping.domain.MemberRole;
import com.webjpa.shopping.dto.CreateMemberRequest;
import com.webjpa.shopping.dto.MemberResponse;
import com.webjpa.shopping.repository.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public MemberResponse create(CreateMemberRequest request) {
        return create(request, MemberRole.USER);
    }

    @Transactional
    public MemberResponse create(CreateMemberRequest request, MemberRole role) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists.");
        }

        Member member = Member.create(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                role
        );
        return MemberResponse.from(memberRepository.save(member));
    }

    public List<MemberResponse> getAll() {
        return memberRepository.findAll().stream()
                .map(MemberResponse::from)
                .toList();
    }

    public MemberResponse get(Long memberId) {
        return MemberResponse.from(getEntity(memberId));
    }

    public Member getEntity(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member not found. id=" + memberId));
    }

    public Member getEntityByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member not found. email=" + email));
    }
}
