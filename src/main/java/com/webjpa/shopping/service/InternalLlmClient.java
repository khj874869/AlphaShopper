package com.webjpa.shopping.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class InternalLlmClient {

    private static final Logger log = LoggerFactory.getLogger(InternalLlmClient.class);

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final HttpClient httpClient;

    public InternalLlmClient(ObjectMapper objectMapper,
                             @Value("${app.ai.llm.enabled:false}") boolean enabled,
                             @Value("${app.ai.llm.base-url:}") String baseUrl,
                             @Value("${app.ai.llm.api-key:}") String apiKey,
                             @Value("${app.ai.llm.model:internal-shopping-assistant}") String model,
                             @Value("${app.ai.llm.timeout-ms:8000}") long timeoutMillis) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.timeout = Duration.ofMillis(timeoutMillis);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .build();
    }

    public Optional<String> complete(String systemPrompt, String userPrompt) {
        if (!enabled || baseUrl.isBlank()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "temperature", 0.2,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/chat/completions"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));

            if (!apiKey.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("event=internal_llm_request_failed status={}", response.statusCode());
                return Optional.empty();
            }

            JsonNode content = objectMapper.readTree(response.body())
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content");
            if (!content.isTextual() || content.asText().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(content.asText().trim());
        } catch (Exception ex) {
            log.warn("event=internal_llm_request_failed error={}", ex.toString());
            return Optional.empty();
        }
    }

    private static String normalizeBaseUrl(String value) {
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
