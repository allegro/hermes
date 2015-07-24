package pl.allegro.tech.hermes.consumers.consumer.offset;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageCommitter;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumerHolder;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OffsetCommitter implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OffsetCommitter.class);

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ConsumerHolder consumerHolder;
    private final List<MessageCommitter> messageCommitters;
    private final ConfigFactory configFactory;

    public OffsetCommitter(
            ConsumerHolder consumerHolder,
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
        for (Consumer consumer : consumerHolder) {
            Subscription subscription = consumer.getSubscription();
            for (PartitionOffset partitionOffset : consumer.getOffsetsToCommit()) {
                for (MessageCommitter messageCommitter : messageCommitters) {
                    try {
                        messageCommitter.commitOffset(subscription, partitionOffset);
                    } catch (Exception e) {
                        LOGGER.error(String.format("Failed to commit offsets using message committer: %s",
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
