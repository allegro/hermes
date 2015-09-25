package pl.allegro.tech.hermes.consumers.consumer.offset;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionOffsetCommitQueues {

    private final LoadingCache<Integer, OffsetCommitQueue> queues;

    public SubscriptionOffsetCommitQueues(Subscription subscription, HermesMetrics hermesMetrics, Clock clock, ConfigFactory configFactory) {
        this.queues = CacheBuilder.newBuilder()
                .build(new OffsetCommitQueueLoader(subscription, hermesMetrics, clock, configFactory));
    }

    public void put(Message message) {
        OffsetCommitQueue helper = queues.getUnchecked(message.getPartition());
        helper.put(message.getOffset());
    }

    public void decrement(int partition, long offset) {
        OffsetCommitQueue offsetCommitQueue = queues.getUnchecked(partition);
        offsetCommitQueue.markDelivered(offset);
    }

    public List<PartitionOffset> getOffsetsToCommit() {
        List<PartitionOffset> offsets = new ArrayList<>();
        queues.asMap().forEach((partition, queue) -> queue.poll().ifPresent(offset -> offsets.add(new PartitionOffset(offset, partition))));
        return offsets;
    }

    private static final class OffsetCommitQueueLoader extends CacheLoader<Integer, OffsetCommitQueue> {

        private final Subscription subscription;
        private final HermesMetrics hermesMetrics;
        private final Clock clock;
        private final ConfigFactory configFactory;

        public OffsetCommitQueueLoader(Subscription subscription, HermesMetrics hermesMetrics, Clock clock, ConfigFactory configFactory) {
            this.subscription = subscription;
            this.hermesMetrics = hermesMetrics;
            this.clock = clock;
            this.configFactory = configFactory;
        }

        @Override
        public OffsetCommitQueue load(Integer partition) throws Exception {
           return new OffsetCommitQueue(new OffsetCommitQueueMonitor(subscription, partition, hermesMetrics, clock,
                   configFactory.getIntProperty(Configs.CONSUMER_OFFSET_COMMIT_QUEUE_ALERT_MINIMAL_IDLE_PERIOD),
                   configFactory.getIntProperty(Configs.CONSUMER_OFFSET_COMMIT_QUEUE_ALERT_SIZE)));
        }
    }

}
