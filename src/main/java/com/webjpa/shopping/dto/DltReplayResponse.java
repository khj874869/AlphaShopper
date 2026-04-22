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
        String lastError
) {
}
