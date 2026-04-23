package com.webjpa.shopping.controller;

import com.webjpa.shopping.dto.ProductDiscoveryClickRequest;
import com.webjpa.shopping.dto.ProductDiscoveryClickResponse;
import com.webjpa.shopping.dto.ProductDiscoveryImpressionBatchRequest;
import com.webjpa.shopping.dto.ProductDiscoveryImpressionRequest;
import com.webjpa.shopping.dto.ProductDiscoveryImpressionResponse;
import com.webjpa.shopping.security.AccessGuard;
import com.webjpa.shopping.security.AuthenticatedMember;
import com.webjpa.shopping.service.ProductDiscoveryClickLogService;
import com.webjpa.shopping.service.ProductDiscoveryImpressionLogService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class ProductDiscoveryController {

    private final ProductDiscoveryClickLogService clickLogService;
    private final ProductDiscoveryImpressionLogService impressionLogService;
    private final AccessGuard accessGuard;

    public ProductDiscoveryController(ProductDiscoveryClickLogService clickLogService,
                                      ProductDiscoveryImpressionLogService impressionLogService,
                                      AccessGuard accessGuard) {
        this.clickLogService = clickLogService;
        this.impressionLogService = impressionLogService;
        this.accessGuard = accessGuard;
    }

    @PostMapping("/product-clicks")
    public ProductDiscoveryClickResponse record(@Valid @RequestBody ProductDiscoveryClickRequest request,
                                                @AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
        if (request.memberId() != null) {
            accessGuard.requireMemberAccess(request.memberId(), authenticatedMember);
        }
        return clickLogService.record(request);
    }

    @PostMapping("/product-impressions")
    public ProductDiscoveryImpressionResponse recordImpressions(
            @Valid @RequestBody ProductDiscoveryImpressionBatchRequest request,
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
        for (ProductDiscoveryImpressionRequest impression : request.impressions()) {
            if (impression.memberId() != null) {
                accessGuard.requireMemberAccess(impression.memberId(), authenticatedMember);
            }
        }
        return impressionLogService.record(request);
    }
}
