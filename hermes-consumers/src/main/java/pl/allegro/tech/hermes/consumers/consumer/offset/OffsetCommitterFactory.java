package pl.allegro.tech.hermes.consumers.consumer.offset;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;

public class OffsetCommitterFactory {


    private final ConsumerPartitionAssignmentState consumerPartitionAssignmentState;

    private final MetricsFacade metrics;

    private final int offsetCommitPeriodSeconds;

    public OffsetCommitterFactory(ConsumerPartitionAssignmentState consumerPartitionAssignmentState,
                                  MetricsFacade metrics,
                                  int offsetCommitPeriodSeconds) {
        this.consumerPartitionAssignmentState = consumerPartitionAssignmentState;
        this.metrics = metrics;
        this.offsetCommitPeriodSeconds = offsetCommitPeriodSeconds;
    }

    public OffsetCommitter createOffsetCommitter(SubscriptionName subscriptionName, MessageCommitter messageCommitter, OffsetQueue offsetQueue) {
        return new OffsetCommitter(
                offsetQueue,
                consumerPartitionAssignmentState,
                messageCommitter,
                offsetCommitPeriodSeconds,
                metrics,
                subscriptionName
        );
    }
}
