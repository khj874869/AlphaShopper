package com.webjpa.shopping.controller;

import com.webjpa.shopping.dto.CreateMemberRequest;
import com.webjpa.shopping.dto.MemberResponse;
import com.webjpa.shopping.security.AccessGuard;
import com.webjpa.shopping.security.AuthenticatedMember;
import com.webjpa.shopping.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final AccessGuard accessGuard;

    public MemberController(MemberService memberService, AccessGuard accessGuard) {
        this.memberService = memberService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public List<MemberResponse> getAll() {
        return memberService.getAll();
    }

    @GetMapping("/{memberId}")
    public MemberResponse get(@PathVariable Long memberId,
                              @AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
        accessGuard.requireMemberAccess(memberId, authenticatedMember);
        return memberService.get(memberId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@Valid @RequestBody CreateMemberRequest request) {
        return memberService.create(request);
    }
}
