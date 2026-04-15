package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.domain.Member;
import com.webjpa.shopping.dto.CreateMemberRequest;
import com.webjpa.shopping.dto.MemberResponse;
import com.webjpa.shopping.repository.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public MemberResponse create(CreateMemberRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다.");
        }

        Member member = Member.create(request.name(), request.email());
        return MemberResponse.from(memberRepository.save(member));
    }

    public Member getEntity(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다. id=" + memberId));
    }
}

