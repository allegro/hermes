package pl.allegro.tech.hermes.consumers.supervisor.background;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;
import pl.allegro.tech.hermes.consumers.consumer.status.Status;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersExecutorService;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;

import java.time.Clock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

import static java.util.Optional.ofNullable;

public class ConsumerSupervisorProcess implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerSupervisorProcess.class);

    private final Map<SubscriptionName, Long> timestamps = new HashMap<>();
    private final Map<SubscriptionName, Future> runningTasks = new HashMap<>();

    private final AssignedConsumers consumers;

    private final ConsumersExecutorService executor;

    private final Clock clock;

    private long unhealthyAfter;

    private Retransmitter retransmitter;

    public ConsumerSupervisorProcess(AssignedConsumers consumers,
                                     ConsumersExecutorService executor,
                                     Retransmitter retransmitter,
                                     Clock clock,
                                     ConfigFactory configs) {
        this.consumers = consumers;
        this.executor = executor;
        this.retransmitter = retransmitter;
        this.clock = clock;
        this.unhealthyAfter = configs.getIntProperty(Configs.CONSUMER_BACKGROUND_SUPERVISOR_UNHEALTHY_AFTER);
    }

    @Override
    public void run() {
        long currentTime = clock.millis();
        Iterator<Consumer> iterator = consumers.iterator();
        while (iterator.hasNext()) {
            Consumer consumer = iterator.next();
            SubscriptionName subscription = consumer.getSubscription().toSubscriptionName();
            Status status = consumer.getStatus();
            if (isHealthy(currentTime, status, subscription)) {
                try {
                    switch (status.getType()) {
                        case NEW: start(subscription, consumer); break;
                        case STOPPED: {
                            switch (status.getShutdownCause().get()) {
                                case RETRANSMISSION: reloadOffsets(subscription);
                                case RESTART:
                                case BROKEN:
                                    start(subscription, consumer);
                                    break;
                                case CONTROLLED:
                                    remove(subscription, iterator);
                                    break;
                                default:
                                    break;
                            }
                        }
                        default:
                            break;
                    }
                } catch (Exception ex) {
                    logger.error("Failed to handle status {} for {}", status, subscription.getId(), ex);
                }
            } else {
                logger.info("Detected unhealthy consumer for {}", subscription.getId());
                kill(subscription);
                start(subscription, consumer);
            };
        }
    }

    private void kill(SubscriptionName subscription) {
        logger.info("Interrupting consumer for {}", subscription.getId());
        Future task = runningTasks.get(subscription);
        if (!task.isDone()) {
            if (task.cancel(true)) {
                logger.info("Interrupted consumer for {}", subscription.getId());
            } else {
                logger.error("Failed to interrupt consumer for {}, possible stale consumer", subscription.getId());
            }
        } else {
            logger.info("Consumer was already dead for {}", subscription.getId());
        }
    }

    private boolean isHealthy(long currentTime, Status status, SubscriptionName subscription) {
        Optional<Long> lastSeen = ofNullable(timestamps.get(subscription));
        if (lastSeen.isPresent()) {
            long delta = status.getTimestamp() - lastSeen.get();
            if (delta <= 0) {
                logger.info("Lost contact with consumer {}, status {}, delta {}", subscription.getId(), status.getTimestamp(), delta);
                if (currentTime - status.getTimestamp() > unhealthyAfter) {
                    return false;
                }
            }
        }
        return true;
    }

    private void remove(SubscriptionName subscriptionName, Iterator<Consumer> iterator) {
        logger.info("Deleting consumer for {}", subscriptionName.getId());
        iterator.remove();
        timestamps.remove(subscriptionName);
        runningTasks.remove(subscriptionName);
        logger.info("Deleted consumer for {}", subscriptionName.getId());
    }

    private void start(SubscriptionName subscriptionName, Consumer consumer) {
        logger.info("Starting consumer for {}", subscriptionName.getId());
        executor.execute(consumer);
    }

    private void reloadOffsets(SubscriptionName subscription) {
        logger.info("Reloading offsets for {}", subscription.getId());
        retransmitter.reloadOffsets(subscription);
        logger.info("Finished reloading offsets for {}", subscription.getId());
    }

    public void shutdown() {
//        consumers.shutdown();
        executor.shutdown();
    }
}
