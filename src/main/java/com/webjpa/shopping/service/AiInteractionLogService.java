package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.domain.AiInteractionLog;
import com.webjpa.shopping.domain.AiInteractionType;
import com.webjpa.shopping.domain.AiRecommendationBucket;
import com.webjpa.shopping.domain.AiRecommendationSource;
import com.webjpa.shopping.dto.AiInteractionLogResponse;
import com.webjpa.shopping.dto.AiInteractionReviewRequest;
import com.webjpa.shopping.dto.AiInteractionReviewStatus;
import com.webjpa.shopping.dto.AiProductRecommendationResponse;
import com.webjpa.shopping.repository.AiInteractionLogRepository;
import com.webjpa.shopping.security.AuthenticatedMember;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiInteractionLogService {

    private final AiInteractionLogRepository aiInteractionLogRepository;

    public AiInteractionLogService(AiInteractionLogRepository aiInteractionLogRepository) {
        this.aiInteractionLogRepository = aiInteractionLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long memberId,
                       AiInteractionType interactionType,
                       String prompt,
                       String reply,
                       boolean llmUsed,
                       AiRecommendationSource recommendationSource,
                       AiRecommendationBucket recommendationBucket,
                       List<AiProductRecommendationResponse> recommendations) {
        aiInteractionLogRepository.save(AiInteractionLog.create(
                memberId,
                interactionType,
                prompt,
                reply,
                llmUsed,
                recommendationSource,
                recommendationBucket,
                recommendations
        ));
    }

    @Transactional(readOnly = true)
    public List<AiInteractionLogResponse> listRecent(int limit,
                                                     AiRecommendationSource recommendationSource,
                                                     AiRecommendationBucket recommendationBucket,
                                                     Boolean llmUsed,
                                                     AiInteractionReviewStatus reviewStatus) {
        Specification<AiInteractionLog> specification =
                buildSpecification(recommendationSource, recommendationBucket, llmUsed, reviewStatus);
        return aiInteractionLogRepository.findAll(
                        specification,
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "requestedAt"))
                )
                .getContent()
                .stream()
                .map(AiInteractionLogResponse::from)
                .toList();
    }

    private Specification<AiInteractionLog> buildSpecification(AiRecommendationSource recommendationSource,
                                                               AiRecommendationBucket recommendationBucket,
                                                               Boolean llmUsed,
                                                               AiInteractionReviewStatus reviewStatus) {
        Specification<AiInteractionLog> specification = Specification.unrestricted();

        if (recommendationSource != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("recommendationSource"), recommendationSource));
        }
        if (recommendationBucket != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("recommendationBucket"), recommendationBucket));
        }
        if (llmUsed != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("llmUsed"), llmUsed));
        }
        if (reviewStatus == AiInteractionReviewStatus.REVIEWED) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.isNotNull(root.get("qualityScore")));
        } else if (reviewStatus == AiInteractionReviewStatus.UNREVIEWED) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.isNull(root.get("qualityScore")));
        } else if (reviewStatus == AiInteractionReviewStatus.LOW_SCORE) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("qualityScore"), 2));
        }

        return specification;
    }

    @Transactional
    public AiInteractionLogResponse review(Long interactionId,
                                           AiInteractionReviewRequest request,
                                           AuthenticatedMember reviewer) {
        AiInteractionLog log = aiInteractionLogRepository.findById(interactionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AI interaction log not found. id=" + interactionId));
        log.review(request.qualityScore(), request.qualityNote(), reviewer.memberId());
        return AiInteractionLogResponse.from(log);
    }
}
