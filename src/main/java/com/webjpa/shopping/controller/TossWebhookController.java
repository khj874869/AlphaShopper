package com.webjpa.shopping.controller;

import com.webjpa.shopping.service.TossWebhookService;
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

    public TossWebhookController(TossWebhookService tossWebhookService) {
        this.tossWebhookService = tossWebhookService;
    }

    @PostMapping("/webhooks")
    public ResponseEntity<Void> handleWebhook(@RequestBody Map<String, Object> payload) {
        tossWebhookService.handle(payload);
        return ResponseEntity.ok().build();
    }
}
