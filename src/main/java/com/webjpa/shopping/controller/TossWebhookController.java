package com.webjpa.shopping.controller;

import com.webjpa.shopping.service.TossWebhookService;
import com.webjpa.shopping.service.TossWebhookSecurityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/toss")
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "toss")
public class TossWebhookController {

    private final TossWebhookService tossWebhookService;
    private final TossWebhookSecurityService tossWebhookSecurityService;

    public TossWebhookController(TossWebhookService tossWebhookService,
                                 TossWebhookSecurityService tossWebhookSecurityService) {
        this.tossWebhookService = tossWebhookService;
        this.tossWebhookSecurityService = tossWebhookSecurityService;
    }

    @PostMapping("/webhooks")
    public ResponseEntity<Void> handleWebhook(@RequestBody Map<String, Object> payload,
                                              HttpServletRequest request) {
        tossWebhookSecurityService.validate(payload, request);
        tossWebhookService.handle(payload);
        return ResponseEntity.ok().build();
    }
}
