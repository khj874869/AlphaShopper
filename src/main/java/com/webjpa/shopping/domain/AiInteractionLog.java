package com.webjpa.shopping.domain;

import com.webjpa.shopping.dto.AiProductRecommendationResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "ai_interaction_log")
public class AiInteractionLog {

    private static final int MAX_PROMPT_LENGTH = 1000;
    private static final int MAX_REPLY_LENGTH = 2000;
    private static final int MAX_NOTE_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiInteractionType interactionType;

    @Column(nullable = false, length = MAX_PROMPT_LENGTH)
    private String prompt;

    @Column(length = MAX_REPLY_LENGTH)
    private String reply;

    @Column(nullable = false)
    private boolean llmUsed;

    @Column(nullable = false)
    private int recommendationCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiRecommendationSource recommendationSource;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private AiRecommendationBucket recommendationBucket;

    @Column(length = 500)
    private String recommendedProductIds;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private Integer qualityScore;

    @Column(length = MAX_NOTE_LENGTH)
    private String qualityNote;

    private LocalDateTime reviewedAt;

    private Long reviewedByMemberId;

    protected AiInteractionLog() {
    }

    private AiInteractionLog(Long memberId,
                             AiInteractionType interactionType,
                             String prompt,
                             String reply,
                             boolean llmUsed,
                             AiRecommendationSource recommendationSource,
                             AiRecommendationBucket recommendationBucket,
                             List<AiProductRecommendationResponse> recommendations) {
        this.memberId = memberId;
        this.interactionType = interactionType;
        this.prompt = truncate(prompt, MAX_PROMPT_LENGTH);
        this.reply = truncate(reply, MAX_REPLY_LENGTH);
        this.llmUsed = llmUsed;
        this.recommendationCount = recommendations.size();
        this.recommendationSource = recommendationSource;
        this.recommendationBucket = recommendationBucket;
        this.recommendedProductIds = recommendations.stream()
                .map(recommendation -> String.valueOf(recommendation.product().id()))
                .collect(Collectors.joining(","));
        this.requestedAt = LocalDateTime.now();
    }

    public static AiInteractionLog create(Long memberId,
                                          AiInteractionType interactionType,
                                          String prompt,
                                          String reply,
                                          boolean llmUsed,
                                          AiRecommendationSource recommendationSource,
                                          AiRecommendationBucket recommendationBucket,
                                          List<AiProductRecommendationResponse> recommendations) {
        return new AiInteractionLog(
                memberId,
                interactionType,
                prompt,
                reply,
                llmUsed,
                recommendationSource,
                recommendationBucket,
                recommendations
        );
    }

    public void review(int qualityScore, String qualityNote, Long reviewedByMemberId) {
        this.qualityScore = qualityScore;
        this.qualityNote = truncate(qualityNote, MAX_NOTE_LENGTH);
        this.reviewedByMemberId = reviewedByMemberId;
        this.reviewedAt = LocalDateTime.now();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public AiInteractionType getInteractionType() {
        return interactionType;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getReply() {
        return reply;
    }

    public boolean isLlmUsed() {
        return llmUsed;
    }

    public int getRecommendationCount() {
        return recommendationCount;
    }

    public AiRecommendationSource getRecommendationSource() {
        return recommendationSource;
    }

    public AiRecommendationBucket getRecommendationBucket() {
        return recommendationBucket;
    }

    public String getRecommendedProductIds() {
        return recommendedProductIds;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public String getQualityNote() {
        return qualityNote;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public Long getReviewedByMemberId() {
        return reviewedByMemberId;
    }
}
