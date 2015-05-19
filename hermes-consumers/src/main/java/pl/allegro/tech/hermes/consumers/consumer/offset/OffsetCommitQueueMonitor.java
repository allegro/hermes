package pl.allegro.tech.hermes.consumers.consumer.offset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.counter.MetricsDeltaCalculator;
import pl.allegro.tech.hermes.common.time.Clock;

public class OffsetCommitQueueMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OffsetCommitQueueMonitor.class);

    private final Subscription subscription;
    private final Integer partition;
    private final HermesMetrics hermesMetrics;
    private final Clock clock;

    private final MetricsDeltaCalculator metricsDeltaCalculator;

    private Long offset;
    private Long offsetChangeTimestampSec;

    private final long offsetCommitQueueAlertSize;
    private final long offsetCommitAdditionalIdlePeriodAlert;

    public OffsetCommitQueueMonitor(Subscription subscription, Integer partition, HermesMetrics hermesMetrics, Clock clock, int offsetCommitAdditionalIdlePeriodAlert, int offsetCommitQueueAlertSize) {
        this.subscription = subscription;
        this.partition = partition;
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
                LOGGER.warn("Commit queue idle for partition {} / {}. Current size {}", subscription.getName(), partition, size);
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
        return (offsetChangeTimestampSec + offsetCommitAdditionalIdlePeriodAlert + subscription.getSubscriptionPolicy().getMessageTtl() < currentTimestampSec());
    }

    private boolean alertSizeExceeded(long size) {
        return size > offsetCommitQueueAlertSize;
    }

    private void removeCounter() {
        hermesMetrics.removeCounterForOffsetCommitIdlePeriod(subscription, partition);
        metricsDeltaCalculator.clear();
    }

    private void reportIdlenessPeriod() {
        long idlenessPeriod = currentTimestampSec() - offsetChangeTimestampSec;
        hermesMetrics.counterForOffsetCommitIdlePeriod(subscription, partition).inc(
                metricsDeltaCalculator.calculateDelta(subscription.getId() + "_" + partition, idlenessPeriod)
        );
    }

    private long currentTimestampSec() {
        return clock.getTime() / 1000;
    }
}
