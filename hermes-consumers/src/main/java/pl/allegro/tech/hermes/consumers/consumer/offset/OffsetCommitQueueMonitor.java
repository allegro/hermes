package pl.allegro.tech.hermes.consumers.consumer.offset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.counter.MetricsDeltaCalculator;

import java.time.Clock;

public class OffsetCommitQueueMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OffsetCommitQueueMonitor.class);

    private final Subscription subscription;
    private final TopicPartition topicPartition;
    private final HermesMetrics hermesMetrics;
    private final Clock clock;

    private final MetricsDeltaCalculator metricsDeltaCalculator;

    private Long offset;
    private Long offsetChangeTimestampSec;

    private final long offsetCommitQueueAlertSize;
    private final long offsetCommitAdditionalIdlePeriodAlert;

    public OffsetCommitQueueMonitor(Subscription subscription, TopicPartition topicPartition, HermesMetrics hermesMetrics,
                                    Clock clock, int offsetCommitAdditionalIdlePeriodAlert, int offsetCommitQueueAlertSize) {
        this.subscription = subscription;
        this.topicPartition = topicPartition;
        this.hermesMetrics = hermesMetrics;
        this.clock = clock;

        this.offsetCommitAdditionalIdlePeriodAlert = offsetCommitAdditionalIdlePeriodAlert;
        this.offsetCommitQueueAlertSize = offsetCommitQueueAlertSize;
        this.metricsDeltaCalculator = new MetricsDeltaCalculator();

    }

    public void nothingNewToCommit(long size, long firstOffset) {
        if (offset != null && firstOffset == offset) {
            if (alertSizeExceeded(size) || idlePeriodDurationAlert()) {
                reportIdlenessPeriod();
                LOGGER.warn("Commit queue idle for partition {} of kafka topic {} and subscription {}. Current size {}",
                        topicPartition.getPartition(), topicPartition.getTopic(), subscription.getName(), size);
            }
        } else {
            this.offset = firstOffset;
            this.offsetChangeTimestampSec = currentTimestampSec();
        }
    }

    public void newOffsetCommit() {
        removeCounter();
        this.offset = null;
        this.offsetChangeTimestampSec = null;
    }

    private boolean idlePeriodDurationAlert() {
        return (offsetChangeTimestampSec + offsetCommitAdditionalIdlePeriodAlert + subscription.getSerialSubscriptionPolicy().getMessageTtl() < currentTimestampSec());
    }

    private boolean alertSizeExceeded(long size) {
        return size > offsetCommitQueueAlertSize;
    }

    private void removeCounter() {
        hermesMetrics.removeCounterForOffsetCommitIdlePeriod(subscription, topicPartition.getTopic(), topicPartition.getPartition());
        metricsDeltaCalculator.clear();
    }

    private void reportIdlenessPeriod() {
        long idlenessPeriod = currentTimestampSec() - offsetChangeTimestampSec;
        hermesMetrics.counterForOffsetCommitIdlePeriod(subscription, topicPartition.getTopic(), topicPartition.getPartition()).inc(
                metricsDeltaCalculator.calculateDelta(deltaKey(), idlenessPeriod)
        );
    }

    private String deltaKey() {
        return subscription.getId() + "_" + topicPartition.getTopic() + "_" + topicPartition.getPartition();
    }

    private long currentTimestampSec() {
        return clock.millis() / 1000;
    }
}
