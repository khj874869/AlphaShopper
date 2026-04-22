package com.webjpa.shopping.dto;

public record DltReplayResponse(
        String sourceTopic,
        String targetTopic,
        String consumerGroupId,
        boolean dryRun,
        int requestedMessages,
        int inspectedMessages,
        int replayedMessages,
        int committedMessages,
        int failedMessages,
        String lastError,
        Long auditId
) {
    public DltReplayResponse(String sourceTopic,
                             String targetTopic,
                             String consumerGroupId,
                             boolean dryRun,
                             int requestedMessages,
                             int inspectedMessages,
                             int replayedMessages,
                             int committedMessages,
                             int failedMessages,
                             String lastError) {
        this(
                sourceTopic,
                targetTopic,
                consumerGroupId,
                dryRun,
                requestedMessages,
                inspectedMessages,
                replayedMessages,
                committedMessages,
                failedMessages,
                lastError,
                null
        );
    }

    public DltReplayResponse withAuditId(Long auditId) {
        return new DltReplayResponse(
                sourceTopic,
                targetTopic,
                consumerGroupId,
                dryRun,
                requestedMessages,
                inspectedMessages,
                replayedMessages,
                committedMessages,
                failedMessages,
                lastError,
                auditId
        );
    }
}
