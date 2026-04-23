package com.webjpa.shopping.controller;

import com.webjpa.shopping.dto.AiChatRequest;
import com.webjpa.shopping.dto.AiChatResponse;
import com.webjpa.shopping.dto.AiRecommendationRequest;
import com.webjpa.shopping.dto.AiRecommendationResponse;
import com.webjpa.shopping.security.AccessGuard;
import com.webjpa.shopping.security.AuthenticatedMember;
import com.webjpa.shopping.service.AiRateLimitService;
import com.webjpa.shopping.service.ShoppingAiService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ShoppingAiService shoppingAiService;
    private final AccessGuard accessGuard;
    private final AiRateLimitService aiRateLimitService;

    public AiController(ShoppingAiService shoppingAiService,
                        AccessGuard accessGuard,
                        AiRateLimitService aiRateLimitService) {
        this.shoppingAiService = shoppingAiService;
        this.accessGuard = accessGuard;
        this.aiRateLimitService = aiRateLimitService;
    }

    @PostMapping("/chat")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request,
                               @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
                               HttpServletRequest httpServletRequest) {
        aiRateLimitService.check("chat", authenticatedMember, httpServletRequest);
        requireMemberAccessIfPersonalized(request.memberId(), authenticatedMember);
        return shoppingAiService.chat(request.message(), request.memberId(), request.maxRecommendations());
    }

    @PostMapping("/recommendations")
    public AiRecommendationResponse recommendations(@Valid @RequestBody AiRecommendationRequest request,
                                                    @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
                                                    HttpServletRequest httpServletRequest) {
        aiRateLimitService.check("recommendations", authenticatedMember, httpServletRequest);
        requireMemberAccessIfPersonalized(request.memberId(), authenticatedMember);
        return shoppingAiService.recommend(request.prompt(), request.memberId(), request.maxResults());
    }

    private void requireMemberAccessIfPersonalized(Long memberId, AuthenticatedMember authenticatedMember) {
        if (memberId != null) {
            accessGuard.requireMemberAccess(memberId, authenticatedMember);
        }
    }
}
