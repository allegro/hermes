package pl.allegro.tech.hermes.consumers.consumer.offset;

import org.jctools.queues.MessagePassingQueue;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.queue.MonitoredMpscQueue;

import javax.inject.Inject;

public class BetterOffsetQueue {

    private final MonitoredMpscQueue<SubscriptionPartitionOffset> inflightOffsetsQueue;

    private final MonitoredMpscQueue<SubscriptionPartitionOffset> commitOffsetsQueue;

    @Inject
    public BetterOffsetQueue(HermesMetrics metrics) {
        this.inflightOffsetsQueue = new MonitoredMpscQueue<>(metrics, "inflightOffsets", 100_000);
        this.commitOffsetsQueue = new MonitoredMpscQueue<>(metrics, "committedOffsets", 100_000);
    }

    public void offerInflightOffset(SubscriptionPartitionOffset offset) {
        inflightOffsetsQueue.offer(offset);
    }

    public void offerCommittedOffset(SubscriptionPartitionOffset offset) {
        commitOffsetsQueue.offer(offset);
    }

    public void drainInflightOffsets(MessagePassingQueue.Consumer<SubscriptionPartitionOffset> consumer) {
        inflightOffsetsQueue.drain(consumer);
    }

    public void drainCommittedOffsets(MessagePassingQueue.Consumer<SubscriptionPartitionOffset> consumer) {
        commitOffsetsQueue.drain(consumer);
    }
}
