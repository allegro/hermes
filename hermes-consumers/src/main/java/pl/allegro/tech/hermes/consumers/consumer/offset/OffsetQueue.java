package pl.allegro.tech.hermes.consumers.consumer.offset;

import org.jctools.queues.MessagePassingQueue;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.queue.FullDrainMpscQueue;
import pl.allegro.tech.hermes.consumers.queue.MpscQueue;
import pl.allegro.tech.hermes.consumers.queue.WaitFreeDrainMpscQueue;
import pl.allegro.tech.hermes.consumers.queue.MonitoredMpscQueue;

import javax.inject.Inject;

public class OffsetQueue {

    private final MpscQueue<SubscriptionPartitionOffset> inflightOffsetsQueue;

    private final MpscQueue<SubscriptionPartitionOffset> commitOffsetsQueue;

    @Inject
    public OffsetQueue(HermesMetrics metrics, ConfigFactory configFactory) {
        int queueSize = configFactory.getIntProperty(Configs.CONSUMER_COMMIT_OFFSET_QUEUES_SIZE);

        boolean fullDrainInflightsQueue = configFactory.getBooleanProperty(Configs.CONSUMER_COMMIT_OFFSET_QUEUES_INFLIGHT_DRAIN_FULL);
        this.inflightOffsetsQueue = new MonitoredMpscQueue<>(fullDrainInflightsQueue ?
                new FullDrainMpscQueue<>(queueSize) : new WaitFreeDrainMpscQueue<>(queueSize), metrics, "inflightOffsets");
        this.commitOffsetsQueue = new MonitoredMpscQueue<>(new WaitFreeDrainMpscQueue<>(queueSize), metrics, "committedOffsets");
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
