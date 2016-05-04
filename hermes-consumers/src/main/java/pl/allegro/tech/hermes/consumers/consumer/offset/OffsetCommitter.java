package pl.allegro.tech.hermes.consumers.consumer.offset;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerHolder;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class OffsetCommitter implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OffsetCommitter.class);

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Supplier<Iterable<Consumer>> consumerHolder;
    private final List<MessageCommitter> messageCommitters;
    private final ConfigFactory configFactory;

    public OffsetCommitter(
            Supplier<Iterable<Consumer>> consumerHolder,
            List<MessageCommitter> messageCommitters,
            ConfigFactory configFactory) {
        this.consumerHolder = consumerHolder;
        this.messageCommitters = messageCommitters;
        this.configFactory = configFactory;
    }

    public void start() {
        scheduledExecutor.scheduleAtFixedRate(
                this,
                configFactory.getIntProperty(Configs.CONSUMER_COMMIT_OFFSET_PERIOD),
                configFactory.getIntProperty(Configs.CONSUMER_COMMIT_OFFSET_PERIOD),
                TimeUnit.SECONDS
        );
    }

    @Override
    public void run() {
        Map<Subscription, PartitionOffset> offsetsPerSubscription = Maps.newHashMap();
        for (Consumer consumer : consumerHolder.get()) {
            Subscription subscription = consumer.getSubscription();
            for (PartitionOffset partitionOffset : consumer.getOffsetsToCommit()) {
                for (MessageCommitter messageCommitter : messageCommitters) {
                    try {
                        messageCommitter.commitOffset(subscription.toSubscriptionName(), partitionOffset);
                    } catch (Exception e) {
                        LOGGER.error(String.format("Failed to commit offsets for subscription %s using message committer: %s",
                                subscription.getId(),
                                messageCommitter.getClass().getSimpleName()), e);
                    }
                }
                offsetsPerSubscription.put(subscription, partitionOffset);
            }
        }
    }

    public void shutdown() throws InterruptedException {
        scheduledExecutor.submit(this);
        scheduledExecutor.shutdown();
        scheduledExecutor.awaitTermination(1, TimeUnit.MINUTES);
    }

}
