package pl.allegro.tech.hermes.consumers.consumer.offset;

import org.jctools.queues.MessagePassingQueue;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.queue.FullDrainMpscQueue;
import pl.allegro.tech.hermes.consumers.queue.MonitoredMpscQueue;
import pl.allegro.tech.hermes.consumers.queue.MpscQueue;
import pl.allegro.tech.hermes.consumers.queue.WaitFreeDrainMpscQueue;

public class OffsetQueue {

    private final MpscQueue<SubscriptionPartitionOffset> inflightOffsetsQueue;

    private final MpscQueue<SubscriptionPartitionOffset> deliveredOffsetsQueue;

    public OffsetQueue(MetricsFacade metrics, int commitOffsetQueuesSize) {
        this.inflightOffsetsQueue =
                new MonitoredMpscQueue<>(new FullDrainMpscQueue<>(commitOffsetQueuesSize), metrics, "inflightOffsets");
        this.deliveredOffsetsQueue =
                new MonitoredMpscQueue<>(new WaitFreeDrainMpscQueue<>(commitOffsetQueuesSize), metrics, "committedOffsets");
    }

    public void offerInflightOffset(SubscriptionPartitionOffset offset) {
        inflightOffsetsQueue.offer(offset);
    }

    public void offerCommittedOffset(SubscriptionPartitionOffset offset) {
        deliveredOffsetsQueue.offer(offset);
    }

    public void drainInflightOffsets(MessagePassingQueue.Consumer<SubscriptionPartitionOffset> consumer) {
        inflightOffsetsQueue.drain(consumer);
    }

    public void drainCommittedOffsets(MessagePassingQueue.Consumer<SubscriptionPartitionOffset> consumer) {
        deliveredOffsetsQueue.drain(consumer);
    }
}
