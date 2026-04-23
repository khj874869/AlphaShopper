package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.AiInteractionType;
import com.webjpa.shopping.dto.AiChatResponse;
import com.webjpa.shopping.dto.AiProductRecommendationResponse;
import com.webjpa.shopping.dto.AiRecommendationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ShoppingAiService {

    private static final String SYSTEM_PROMPT = """
            You are AlphaShopper's internal shopping assistant.
            Answer in Korean.
            Use only the provided product catalog context for product claims.
            Keep the answer concise and helpful.
            If product context is empty, say that there are no matching active products.
            """;

    private final ProductRecommendationService productRecommendationService;
    private final InternalLlmClient internalLlmClient;
    private final AiInteractionLogService aiInteractionLogService;

    public ShoppingAiService(ProductRecommendationService productRecommendationService,
                             InternalLlmClient internalLlmClient,
                             AiInteractionLogService aiInteractionLogService) {
        this.productRecommendationService = productRecommendationService;
        this.internalLlmClient = internalLlmClient;
        this.aiInteractionLogService = aiInteractionLogService;
    }

    @Transactional
    public AiChatResponse chat(String message, Long memberId, Integer maxRecommendations) {
        ProductRecommendationResult recommendationResult =
                productRecommendationService.recommend(message, memberId, maxRecommendations);
        List<AiProductRecommendationResponse> recommendations = recommendationResult.recommendations();
        String userPrompt = buildUserPrompt(message, recommendations);

        Optional<String> llmReply = internalLlmClient.complete(SYSTEM_PROMPT, userPrompt);
        AiChatResponse response = new AiChatResponse(
                llmReply.orElseGet(() -> buildFallbackReply(message, recommendations)),
                llmReply.isPresent(),
                recommendationResult.source(),
                recommendationResult.bucket(),
                recommendations
        );
        aiInteractionLogService.record(
                memberId,
                AiInteractionType.CHAT,
                message,
                response.reply(),
                response.llmUsed(),
                response.recommendationSource(),
                response.recommendationBucket(),
                recommendations
        );
        return response;
    }

    @Transactional
    public AiRecommendationResponse recommend(String prompt, Long memberId, Integer maxResults) {
        ProductRecommendationResult recommendationResult =
                productRecommendationService.recommend(prompt, memberId, maxResults);
        List<AiProductRecommendationResponse> recommendations = recommendationResult.recommendations();
        aiInteractionLogService.record(
                memberId,
                AiInteractionType.RECOMMENDATION,
                prompt,
                null,
                false,
                recommendationResult.source(),
                recommendationResult.bucket(),
                recommendations
        );
        return new AiRecommendationResponse(prompt, recommendationResult.source(), recommendationResult.bucket(), recommendations);
    }

    private String buildUserPrompt(String message, List<AiProductRecommendationResponse> recommendations) {
        return """
                Shopper request:
                %s

                Product candidates:
                %s
                """.formatted(message, buildProductContext(recommendations));
    }

    private String buildProductContext(List<AiProductRecommendationResponse> recommendations) {
        if (recommendations.isEmpty()) {
            return "(none)";
        }

        return recommendations.stream()
                .map(recommendation -> {
                    var product = recommendation.product();
                    return "- id=%d, brand=%s, name=%s, price=%s, stock=%d, description=%s, reason=%s, searchScore=%s, highlights=%s"
                            .formatted(
                                    product.id(),
                                    product.brand(),
                                    product.name(),
                                    product.price(),
                                    product.stockQuantity(),
                                    product.description(),
                                    recommendation.reason(),
                                    recommendation.searchScore() == null ? "n/a" : recommendation.searchScore(),
                                    recommendation.highlights().isEmpty() ? "none" : String.join(" | ", recommendation.highlights())
                            );
                })
                .collect(Collectors.joining("\n"));
    }

    private String buildFallbackReply(String message, List<AiProductRecommendationResponse> recommendations) {
        if (recommendations.isEmpty()) {
            return "\uD604\uC7AC \uC694\uCCAD\uC5D0 \uB9DE\uCDB0 \uCD94\uCC9C\uD560 \uC218 \uC788\uB294 \uD65C\uC131 \uC0C1\uD488\uC774 \uC5C6\uC2B5\uB2C8\uB2E4. \uB2E4\uB978 \uBD84\uC704\uAE30\uB098 \uCE74\uD14C\uACE0\uB9AC\uB85C \uB2E4\uC2DC \uBB3C\uC5B4\uBD10 \uC8FC\uC138\uC694.";
        }

        String names = recommendations.stream()
                .map(recommendation -> recommendation.product().name())
                .limit(3)
                .collect(Collectors.joining(", "));
        return "\uB0B4\uBD80 LLM \uC5F0\uACB0 \uC804\uC774\uB77C \uC0C1\uD488 \uB370\uC774\uD130 \uAE30\uC900\uC73C\uB85C \uCD94\uCC9C\uB4DC\uB9B4\uAC8C\uC694. \"%s\" \uC694\uCCAD\uC5D0\uB294 %s \uC911\uC2EC\uC73C\uB85C \uBCF4\uB294 \uAC83\uC774 \uC88B\uC2B5\uB2C8\uB2E4."
                .formatted(message, names);
    }
}
