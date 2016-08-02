package pl.allegro.tech.hermes.consumers.consumer.offset;

import org.jctools.queues.MessagePassingQueue;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.queue.MonitoredMpscQueue;

import javax.inject.Inject;

public class OffsetQueue {

    private final MonitoredMpscQueue<SubscriptionPartitionOffset> inflightOffsetsQueue;

    private final MonitoredMpscQueue<SubscriptionPartitionOffset> commitOffsetsQueue;

    @Inject
    public OffsetQueue(HermesMetrics metrics, ConfigFactory configFactory) {
        int queueSize = configFactory.getIntProperty(Configs.CONSUMER_COMMIT_OFFSET_QUEUES_SIZE);

        this.inflightOffsetsQueue = new MonitoredMpscQueue<>(metrics, "inflightOffsets", queueSize);
        this.commitOffsetsQueue = new MonitoredMpscQueue<>(metrics, "committedOffsets", queueSize);
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
