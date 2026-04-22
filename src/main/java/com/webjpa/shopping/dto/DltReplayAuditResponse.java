package com.webjpa.shopping.dto;

import com.webjpa.shopping.domain.DltReplayAuditStatus;
import com.webjpa.shopping.domain.KafkaDltReplayAudit;

import java.time.LocalDateTime;

public record DltReplayAuditResponse(
        Long id,
        Long adminMemberId,
        String adminEmail,
        String adminName,
        String sourceTopic,
        String targetTopic,
        String consumerGroupId,
        boolean dryRun,
        int requestedMessages,
        int inspectedMessages,
        int replayedMessages,
        int committedMessages,
        int failedMessages,
        DltReplayAuditStatus status,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        String lastError
) {
    public static DltReplayAuditResponse from(KafkaDltReplayAudit audit) {
        return new DltReplayAuditResponse(
                audit.getId(),
                audit.getAdminMemberId(),
                audit.getAdminEmail(),
                audit.getAdminName(),
                audit.getSourceTopic(),
                audit.getTargetTopic(),
                audit.getConsumerGroupId(),
                audit.isDryRun(),
                audit.getRequestedMessages(),
                audit.getInspectedMessages(),
                audit.getReplayedMessages(),
                audit.getCommittedMessages(),
                audit.getFailedMessages(),
                audit.getStatus(),
                audit.getRequestedAt(),
                audit.getCompletedAt(),
                audit.getLastError()
        );
    }
}
