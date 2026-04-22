package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.DltReplayAuditStatus;
import com.webjpa.shopping.domain.KafkaDltReplayAudit;
import com.webjpa.shopping.domain.MemberRole;
import com.webjpa.shopping.dto.DltReplayAuditResponse;
import com.webjpa.shopping.dto.DltReplayResponse;
import com.webjpa.shopping.repository.KafkaDltReplayAuditRepository;
import com.webjpa.shopping.security.AuthenticatedMember;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderNotificationDltReplayAuditServiceTest {

    private OrderNotificationDltReplayService replayService;
    private KafkaDltReplayAuditRepository auditRepository;
    private SimpleMeterRegistry meterRegistry;
    private OrderNotificationDltReplayAuditService auditService;

    @BeforeEach
    void setUp() {
        replayService = mock(OrderNotificationDltReplayService.class);
        auditRepository = mock(KafkaDltReplayAuditRepository.class);
        meterRegistry = new SimpleMeterRegistry();
        auditService = new OrderNotificationDltReplayAuditService(
                replayService,
                auditRepository,
                new OrderNotificationDltReplayMetrics(meterRegistry)
        );

        when(replayService.sourceTopic()).thenReturn("orders.DLT");
        when(replayService.targetTopic()).thenReturn("orders");
        when(replayService.consumerGroupId()).thenReturn("replay-group");
        when(auditRepository.save(any(KafkaDltReplayAudit.class))).thenAnswer(invocation -> {
            KafkaDltReplayAudit audit = invocation.getArgument(0);
            if (audit.getId() == null) {
                ReflectionTestUtils.setField(audit, "id", 42L);
            }
            return audit;
        });
    }

    @Test
    void replay_recordsSucceededAuditAndReturnsAuditId() {
        when(replayService.replay(25, false)).thenReturn(new DltReplayResponse(
                "orders.DLT",
                "orders",
                "replay-group",
                false,
                25,
                2,
                2,
                2,
                0,
                null
        ));

        DltReplayResponse response = auditService.replay(25, false, admin());

        assertThat(response.auditId()).isEqualTo(42L);
        ArgumentCaptor<KafkaDltReplayAudit> auditCaptor = ArgumentCaptor.forClass(KafkaDltReplayAudit.class);
        verify(auditRepository, times(2)).save(auditCaptor.capture());
        KafkaDltReplayAudit audit = auditCaptor.getValue();
        assertThat(audit.getStatus()).isEqualTo(DltReplayAuditStatus.SUCCEEDED);
        assertThat(audit.getAdminMemberId()).isEqualTo(7L);
        assertThat(audit.getAdminEmail()).isEqualTo("admin@example.com");
        assertThat(audit.getRequestedMessages()).isEqualTo(25);
        assertThat(audit.getInspectedMessages()).isEqualTo(2);
        assertThat(audit.getReplayedMessages()).isEqualTo(2);
        assertThat(audit.getCommittedMessages()).isEqualTo(2);
        assertThat(audit.getFailedMessages()).isZero();
        assertThat(audit.getCompletedAt()).isNotNull();
        assertThat(counterValue(OrderNotificationDltReplayMetrics.REQUESTS)).isEqualTo(1.0);
        assertThat(counterValue(OrderNotificationDltReplayMetrics.RESULTS, "result", "succeeded")).isEqualTo(1.0);
        assertThat(counterValue(OrderNotificationDltReplayMetrics.MESSAGES, "kind", "inspected")).isEqualTo(2.0);
        assertThat(counterValue(OrderNotificationDltReplayMetrics.MESSAGES, "kind", "replayed")).isEqualTo(2.0);
        assertThat(meterRegistry.find(OrderNotificationDltReplayMetrics.DURATION).tag("result", "succeeded").timer())
                .isNotNull();
    }

    @Test
    void replay_whenDelegateFails_recordsFailedAuditAndRethrows() {
        when(replayService.replay(10, true)).thenThrow(new IllegalStateException("broker unavailable"));

        assertThatThrownBy(() -> auditService.replay(10, true, admin()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("broker unavailable");

        ArgumentCaptor<KafkaDltReplayAudit> auditCaptor = ArgumentCaptor.forClass(KafkaDltReplayAudit.class);
        verify(auditRepository, times(2)).save(auditCaptor.capture());
        KafkaDltReplayAudit audit = auditCaptor.getValue();
        assertThat(audit.getStatus()).isEqualTo(DltReplayAuditStatus.FAILED);
        assertThat(audit.isDryRun()).isTrue();
        assertThat(audit.getFailedMessages()).isEqualTo(1);
        assertThat(audit.getLastError()).contains("IllegalStateException");
        assertThat(audit.getCompletedAt()).isNotNull();
        assertThat(counterValue(OrderNotificationDltReplayMetrics.REQUESTS)).isEqualTo(1.0);
        assertThat(counterValue(OrderNotificationDltReplayMetrics.RESULTS, "result", "exception")).isEqualTo(1.0);
        assertThat(meterRegistry.find(OrderNotificationDltReplayMetrics.DURATION).tag("result", "exception").timer())
                .isNotNull();
    }

    @Test
    void listRecentAudits_mapsRepositoryRows() {
        KafkaDltReplayAudit audit = KafkaDltReplayAudit.start(
                7L,
                "admin@example.com",
                "Admin",
                "orders.DLT",
                "orders",
                "replay-group",
                true,
                10
        );
        audit.complete(new DltReplayResponse(
                "orders.DLT",
                "orders",
                "replay-group",
                true,
                10,
                3,
                0,
                0,
                0,
                null
        ));
        ReflectionTestUtils.setField(audit, "id", 42L);
        when(auditRepository.findAllByOrderByRequestedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(audit)));

        List<DltReplayAuditResponse> responses = auditService.listRecentAudits(5);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(42L);
        assertThat(responses.get(0).status()).isEqualTo(DltReplayAuditStatus.SUCCEEDED);
        assertThat(responses.get(0).inspectedMessages()).isEqualTo(3);
    }

    private AuthenticatedMember admin() {
        return new AuthenticatedMember(
                7L,
                "admin@example.com",
                "Admin",
                MemberRole.ADMIN
        );
    }

    private double counterValue(String name, String... tags) {
        return meterRegistry.find(name).tags(tags).counter().count();
    }
}
