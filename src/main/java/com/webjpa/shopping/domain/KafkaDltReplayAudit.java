package com.webjpa.shopping.domain;

import com.webjpa.shopping.dto.DltReplayResponse;
import com.webjpa.shopping.logging.LogValues;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "kafka_dlt_replay_audit")
public class KafkaDltReplayAudit {

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long adminMemberId;

    @Column(nullable = false, length = 100)
    private String adminEmail;

    @Column(nullable = false, length = 50)
    private String adminName;

    @Column(nullable = false, length = 200)
    private String sourceTopic;

    @Column(nullable = false, length = 200)
    private String targetTopic;

    @Column(nullable = false, length = 200)
    private String consumerGroupId;

    @Column(nullable = false)
    private boolean dryRun;

    @Column(nullable = false)
    private int requestedMessages;

    @Column(nullable = false)
    private int inspectedMessages;

    @Column(nullable = false)
    private int replayedMessages;

    @Column(nullable = false)
    private int committedMessages;

    @Column(nullable = false)
    private int failedMessages;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DltReplayAuditStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime completedAt;

    @Column(length = 500)
    private String lastError;

    protected KafkaDltReplayAudit() {
    }

    private KafkaDltReplayAudit(Long adminMemberId,
                                String adminEmail,
                                String adminName,
                                String sourceTopic,
                                String targetTopic,
                                String consumerGroupId,
                                boolean dryRun,
                                int requestedMessages) {
        this.adminMemberId = adminMemberId;
        this.adminEmail = adminEmail;
        this.adminName = adminName;
        this.sourceTopic = sourceTopic;
        this.targetTopic = targetTopic;
        this.consumerGroupId = consumerGroupId;
        this.dryRun = dryRun;
        this.requestedMessages = requestedMessages;
        this.status = DltReplayAuditStatus.STARTED;
        this.requestedAt = LocalDateTime.now();
    }

    public static KafkaDltReplayAudit start(Long adminMemberId,
                                            String adminEmail,
                                            String adminName,
                                            String sourceTopic,
                                            String targetTopic,
                                            String consumerGroupId,
                                            boolean dryRun,
                                            int requestedMessages) {
        return new KafkaDltReplayAudit(
                adminMemberId,
                adminEmail,
                adminName,
                sourceTopic,
                targetTopic,
                consumerGroupId,
                dryRun,
                requestedMessages
        );
    }

    public void complete(DltReplayResponse response) {
        this.inspectedMessages = response.inspectedMessages();
        this.replayedMessages = response.replayedMessages();
        this.committedMessages = response.committedMessages();
        this.failedMessages = response.failedMessages();
        this.lastError = truncate(response.lastError());
        this.status = response.failedMessages() == 0 ? DltReplayAuditStatus.SUCCEEDED : DltReplayAuditStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(Exception ex) {
        this.failedMessages = Math.max(failedMessages, 1);
        this.lastError = truncate(ex.getClass().getSimpleName() + ": " + LogValues.safe(ex.getMessage()));
        this.status = DltReplayAuditStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }

    private String truncate(String value) {
        String sanitized = LogValues.safe(value);
        if (sanitized.isBlank()) {
            return null;
        }
        if (sanitized.length() <= MAX_ERROR_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, MAX_ERROR_LENGTH);
    }

    public Long getId() {
        return id;
    }

    public Long getAdminMemberId() {
        return adminMemberId;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getSourceTopic() {
        return sourceTopic;
    }

    public String getTargetTopic() {
        return targetTopic;
    }

    public String getConsumerGroupId() {
        return consumerGroupId;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public int getRequestedMessages() {
        return requestedMessages;
    }

    public int getInspectedMessages() {
        return inspectedMessages;
    }

    public int getReplayedMessages() {
        return replayedMessages;
    }

    public int getCommittedMessages() {
        return committedMessages;
    }

    public int getFailedMessages() {
        return failedMessages;
    }

    public DltReplayAuditStatus getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getLastError() {
        return lastError;
    }
}
