package com.webjpa.shopping.service;

import com.webjpa.shopping.dto.DltReplayResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class OrderNotificationDltReplayMetrics {

    static final String REQUESTS = "alphashopper.kafka.dlt.replay.requests";
    static final String RESULTS = "alphashopper.kafka.dlt.replay.results";
    static final String MESSAGES = "alphashopper.kafka.dlt.replay.messages";
    static final String DURATION = "alphashopper.kafka.dlt.replay.duration";

    private final MeterRegistry meterRegistry;

    public OrderNotificationDltReplayMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordRequested(String sourceTopic, String targetTopic, boolean dryRun) {
        counter(REQUESTS, sourceTopic, targetTopic, dryRun)
                .increment();
    }

    public void recordCompleted(DltReplayResponse response, Duration duration) {
        String result = response.failedMessages() == 0 ? "succeeded" : "failed";
        counter(RESULTS, response.sourceTopic(), response.targetTopic(), response.dryRun(), "result", result)
                .increment();
        recordMessages(response, "inspected", response.inspectedMessages());
        recordMessages(response, "replayed", response.replayedMessages());
        recordMessages(response, "committed", response.committedMessages());
        recordMessages(response, "failed", response.failedMessages());
        timer(response.sourceTopic(), response.targetTopic(), response.dryRun(), result)
                .record(duration);
    }

    public void recordException(String sourceTopic,
                                String targetTopic,
                                boolean dryRun,
                                Exception ex,
                                Duration duration) {
        counter(RESULTS, sourceTopic, targetTopic, dryRun, "result", "exception", "exception", ex.getClass().getSimpleName())
                .increment();
        timer(sourceTopic, targetTopic, dryRun, "exception")
                .record(duration);
    }

    private void recordMessages(DltReplayResponse response, String kind, int count) {
        if (count <= 0) {
            return;
        }
        counter(MESSAGES, response.sourceTopic(), response.targetTopic(), response.dryRun(), "kind", kind)
                .increment(count);
    }

    private Counter counter(String name, String sourceTopic, String targetTopic, boolean dryRun, String... extraTags) {
        Counter.Builder builder = Counter.builder(name)
                .tag("source_topic", sourceTopic)
                .tag("target_topic", targetTopic)
                .tag("dry_run", Boolean.toString(dryRun));
        for (int i = 0; i < extraTags.length; i += 2) {
            builder.tag(extraTags[i], extraTags[i + 1]);
        }
        return builder.register(meterRegistry);
    }

    private Timer timer(String sourceTopic, String targetTopic, boolean dryRun, String result) {
        return Timer.builder(DURATION)
                .tag("source_topic", sourceTopic)
                .tag("target_topic", targetTopic)
                .tag("dry_run", Boolean.toString(dryRun))
                .tag("result", result)
                .register(meterRegistry);
    }
}
