package com.webjpa.shopping.controller;

import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.ProductDiscoverySurface;
import com.webjpa.shopping.dto.AiInteractionLogResponse;
import com.webjpa.shopping.dto.AiInteractionReviewRequest;
import com.webjpa.shopping.dto.AiInteractionReviewStatus;
import com.webjpa.shopping.dto.AiRecommendationSettingsResponse;
import com.webjpa.shopping.dto.ProductDiscoveryClickLogResponse;
import com.webjpa.shopping.dto.ProductDiscoveryFunnelSummaryResponse;
import com.webjpa.shopping.dto.ProductDiscoveryImpressionLogResponse;
import com.webjpa.shopping.security.AuthenticatedMember;
import com.webjpa.shopping.service.AiInteractionLogService;
import com.webjpa.shopping.service.ProductDiscoveryClickLogService;
import com.webjpa.shopping.service.ProductDiscoveryFunnelService;
import com.webjpa.shopping.service.ProductDiscoveryImpressionLogService;
import com.webjpa.shopping.service.ProductRecommendationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDateTime;

@Validated
@RestController
@RequestMapping("/api/admin/ai")
public class AdminAiController {

    private final AiInteractionLogService aiInteractionLogService;
    private final ProductDiscoveryClickLogService productDiscoveryClickLogService;
    private final ProductDiscoveryImpressionLogService productDiscoveryImpressionLogService;
    private final ProductDiscoveryFunnelService productDiscoveryFunnelService;
    private final ProductRecommendationService productRecommendationService;

    public AdminAiController(AiInteractionLogService aiInteractionLogService,
                             ProductDiscoveryClickLogService productDiscoveryClickLogService,
                             ProductDiscoveryImpressionLogService productDiscoveryImpressionLogService,
                             ProductDiscoveryFunnelService productDiscoveryFunnelService,
                             ProductRecommendationService productRecommendationService) {
        this.aiInteractionLogService = aiInteractionLogService;
        this.productDiscoveryClickLogService = productDiscoveryClickLogService;
        this.productDiscoveryImpressionLogService = productDiscoveryImpressionLogService;
        this.productDiscoveryFunnelService = productDiscoveryFunnelService;
        this.productRecommendationService = productRecommendationService;
    }

    @GetMapping("/recommendation-settings")
    public AiRecommendationSettingsResponse recommendationSettings() {
        return productRecommendationService.settings();
    }

    @GetMapping("/interactions")
    public List<AiInteractionLogResponse> listInteractions(
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
            @RequestParam(required = false) AiRecommendationSource recommendationSource,
            @RequestParam(required = false) AiRecommendationBucket recommendationBucket,
            @RequestParam(required = false) Boolean llmUsed,
            @RequestParam(required = false) AiInteractionReviewStatus reviewStatus) {
        return aiInteractionLogService.listRecent(limit, recommendationSource, recommendationBucket, llmUsed, reviewStatus);
    }

    @PatchMapping("/interactions/{interactionId}/review")
    public AiInteractionLogResponse reviewInteraction(@PathVariable Long interactionId,
                                                      @Valid @RequestBody AiInteractionReviewRequest request,
                                                      @AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
        return aiInteractionLogService.review(interactionId, request, authenticatedMember);
    }

    @GetMapping("/product-clicks")
    public List<ProductDiscoveryClickLogResponse> listProductClicks(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit,
            @RequestParam(required = false) ProductDiscoverySurface surface,
            @RequestParam(required = false) AiRecommendationSource recommendationSource,
            @RequestParam(required = false) AiRecommendationBucket recommendationBucket) {
        return productDiscoveryClickLogService.listRecent(limit, surface, recommendationSource, recommendationBucket);
    }

    @GetMapping("/product-impressions")
    public List<ProductDiscoveryImpressionLogResponse> listProductImpressions(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit,
            @RequestParam(required = false) ProductDiscoverySurface surface,
            @RequestParam(required = false) AiRecommendationSource recommendationSource,
            @RequestParam(required = false) AiRecommendationBucket recommendationBucket) {
        return productDiscoveryImpressionLogService.listRecent(limit, surface, recommendationSource, recommendationBucket);
    }

    @GetMapping("/discovery-funnel")
    public ProductDiscoveryFunnelSummaryResponse summarizeDiscoveryFunnel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) ProductDiscoverySurface surface,
            @RequestParam(required = false) AiRecommendationSource recommendationSource,
            @RequestParam(required = false) AiRecommendationBucket recommendationBucket) {
        return productDiscoveryFunnelService.summarize(from, to, surface, recommendationSource, recommendationBucket);
    }
}
