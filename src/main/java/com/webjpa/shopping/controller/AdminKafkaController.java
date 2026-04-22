package com.webjpa.shopping.controller;

import com.webjpa.shopping.dto.DltReplayResponse;
import com.webjpa.shopping.service.OrderNotificationDltReplayService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/kafka")
public class AdminKafkaController {

    private final OrderNotificationDltReplayService orderNotificationDltReplayService;

    public AdminKafkaController(OrderNotificationDltReplayService orderNotificationDltReplayService) {
        this.orderNotificationDltReplayService = orderNotificationDltReplayService;
    }

    @PostMapping("/order-notifications/dlt/replay")
    public DltReplayResponse replayOrderNotificationDlt(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int maxMessages,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return orderNotificationDltReplayService.replay(maxMessages, dryRun);
    }
}
