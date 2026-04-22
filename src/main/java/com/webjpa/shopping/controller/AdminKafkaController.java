package com.webjpa.shopping.controller;

import com.webjpa.shopping.dto.DltReplayAuditResponse;
import com.webjpa.shopping.dto.DltReplayResponse;
import com.webjpa.shopping.security.AuthenticatedMember;
import com.webjpa.shopping.service.OrderNotificationDltReplayAuditService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/admin/kafka")
public class AdminKafkaController {

    private final OrderNotificationDltReplayAuditService orderNotificationDltReplayAuditService;

    public AdminKafkaController(OrderNotificationDltReplayAuditService orderNotificationDltReplayAuditService) {
        this.orderNotificationDltReplayAuditService = orderNotificationDltReplayAuditService;
    }

    @PostMapping("/order-notifications/dlt/replay")
    public DltReplayResponse replayOrderNotificationDlt(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int maxMessages,
            @RequestParam(defaultValue = "false") boolean dryRun,
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
        return orderNotificationDltReplayAuditService.replay(maxMessages, dryRun, authenticatedMember);
    }

    @GetMapping("/order-notifications/dlt/replay/audits")
    public List<DltReplayAuditResponse> listOrderNotificationDltReplayAudits(
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        return orderNotificationDltReplayAuditService.listRecentAudits(limit);
    }
}
