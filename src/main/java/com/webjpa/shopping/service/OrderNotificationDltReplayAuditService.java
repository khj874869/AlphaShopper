package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.KafkaDltReplayAudit;
import com.webjpa.shopping.dto.DltReplayAuditResponse;
import com.webjpa.shopping.dto.DltReplayResponse;
import com.webjpa.shopping.logging.LogValues;
import com.webjpa.shopping.repository.KafkaDltReplayAuditRepository;
import com.webjpa.shopping.security.AuthenticatedMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class OrderNotificationDltReplayAuditService {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationDltReplayAuditService.class);

    private final OrderNotificationDltReplayService replayService;
    private final KafkaDltReplayAuditRepository auditRepository;
    private final OrderNotificationDltReplayMetrics replayMetrics;

    public OrderNotificationDltReplayAuditService(OrderNotificationDltReplayService replayService,
                                                  KafkaDltReplayAuditRepository auditRepository,
                                                  OrderNotificationDltReplayMetrics replayMetrics) {
        this.replayService = replayService;
        this.auditRepository = auditRepository;
        this.replayMetrics = replayMetrics;
    }

    public DltReplayResponse replay(int maxMessages, boolean dryRun, AuthenticatedMember admin) {
        KafkaDltReplayAudit audit = KafkaDltReplayAudit.start(
                admin.memberId(),
                admin.email(),
                admin.name(),
                replayService.sourceTopic(),
                replayService.targetTopic(),
                replayService.consumerGroupId(),
                dryRun,
                maxMessages
        );
        auditRepository.save(audit);
        replayMetrics.recordRequested(audit.getSourceTopic(), audit.getTargetTopic(), audit.isDryRun());
        Instant startedAt = Instant.now();

        try {
            DltReplayResponse response = replayService.replay(maxMessages, dryRun);
            audit.complete(response);
            auditRepository.save(audit);
            replayMetrics.recordCompleted(response, Duration.between(startedAt, Instant.now()));
            log.info("event=order_notification.dlt.replay_audit_completed auditId={} adminMemberId={} adminEmail={} status={} dryRun={} requestedMessages={} inspectedMessages={} replayedMessages={} committedMessages={} failedMessages={}",
                    audit.getId(),
                    audit.getAdminMemberId(),
                    LogValues.maskEmail(audit.getAdminEmail()),
                    audit.getStatus(),
                    audit.isDryRun(),
                    audit.getRequestedMessages(),
                    audit.getInspectedMessages(),
                    audit.getReplayedMessages(),
                    audit.getCommittedMessages(),
                    audit.getFailedMessages());
            return response.withAuditId(audit.getId());
        } catch (RuntimeException ex) {
            audit.fail(ex);
            auditRepository.save(audit);
            replayMetrics.recordException(audit.getSourceTopic(), audit.getTargetTopic(), audit.isDryRun(), ex, Duration.between(startedAt, Instant.now()));
            log.warn("event=order_notification.dlt.replay_audit_failed auditId={} adminMemberId={} adminEmail={} errorType={} error={}",
                    audit.getId(),
                    audit.getAdminMemberId(),
                    LogValues.maskEmail(audit.getAdminEmail()),
                    ex.getClass().getSimpleName(),
                    LogValues.safe(ex.getMessage()),
                    ex);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<DltReplayAuditResponse> listRecentAudits(int limit) {
        return auditRepository.findAllByOrderByRequestedAtDesc(PageRequest.of(
                        0,
                        limit,
                        Sort.by(Sort.Direction.DESC, "requestedAt")
                ))
                .stream()
                .map(DltReplayAuditResponse::from)
                .toList();
    }
}
