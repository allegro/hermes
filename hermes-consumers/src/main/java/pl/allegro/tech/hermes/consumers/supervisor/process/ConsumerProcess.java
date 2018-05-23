package pl.allegro.tech.hermes.consumers.supervisor.process;

import com.google.common.collect.ImmutableMap;
import org.jctools.queues.SpscArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.START;

public class ConsumerProcess implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerProcess.class);

    private final SpscArrayQueue<Signal> signals = new SpscArrayQueue<>(100);

    private final Clock clock;

    private final Subscription subscription;

    private final Consumer consumer;

    private final Retransmitter retransmitter;

    private final java.util.function.Consumer<Signal> shutdownCallback;

    private final long unhealthyAfter;

    private volatile boolean running = true;

    private volatile long healthcheckRefreshTime;

    private Map<Signal.SignalType, Long> signalTimesheet = new ConcurrentHashMap<>();
    private Signal lastSignal;

    public ConsumerProcess(
            Signal startSignal,
            Consumer consumer,
            Retransmitter retransmitter,
            java.util.function.Consumer<Signal> shutdownCallback,
            Clock clock,
            long unhealthyAfter) {
        this.subscription = startSignal.getPayload();
        this.consumer = consumer;
        this.retransmitter = retransmitter;
        this.shutdownCallback = shutdownCallback;
        this.clock = clock;
        this.healthcheckRefreshTime = clock.millis();
        this.unhealthyAfter = unhealthyAfter;

        this.signals.add(startSignal);
    }

    @Override
    public void run() {
        try {
            Thread.currentThread().setName("consumer-" + getSubscriptionName());

            while (running && !Thread.interrupted()) {
                consumer.consume(this::processSignals);
            }
            stop();

        } catch (Exception ex) {
            logger.error("Consumer process of subscription {} failed", getSubscriptionName(), ex);
        } finally {
            logger.info("Releasing consumer process thread of subscription {}", getSubscriptionName());
            shutdownCallback.accept(lastSignal);
            refreshHealthcheck();
            Thread.currentThread().setName("consumer-released-thread");
        }
    }

    public ConsumerProcess accept(Signal signal) {
        this.signals.add(signal);
        return this;
    }

    public boolean isHealthy() {
        return unhealthyAfter > lastSeen();
    }

    public long lastSeen() {
        return clock.millis() - healthcheckRefreshTime;
    }

    public long healthcheckRefreshTime() {
        return healthcheckRefreshTime;
    }

    private void processSignals() {
        refreshHealthcheck();
        signals.drain(this::process);
        refreshHealthcheck();
    }

    private void refreshHealthcheck() {
        this.healthcheckRefreshTime = clock.millis();
    }

    private void process(Signal signal) {
        lastSignal = signal;
        try {
            switch (signal.getType()) {
                case START:
                    start(signal);
                    break;
                case RESTART:
                    restart(signal);
                    break;
                case STOP:
                    logger.info("Stopping main loop for consumer {}. {}", signal.getTarget(), signal.getLogWithIdAndType());
                    this.running = false;
                    break;
                case RETRANSMIT:
                    retransmit(signal);
                    break;
                case UPDATE_SUBSCRIPTION:
                    consumer.updateSubscription(signal.getPayload());
                    break;
                case UPDATE_TOPIC:
                    consumer.updateTopic(signal.getPayload());
                    break;
                case COMMIT:
                    consumer.commit(signal.getPayload());
                    break;
                default:
                    logger.warn("Unhandled signal found {}", signal);
                    break;
            }
            signalTimesheet.put(signal.getType(), clock.millis());
        } catch (Exception ex) {
            logger.error("Failed to process signal {}", signal, ex);
        }
    }

    private void start(Signal signal) {
        long startTime = clock.millis();
        logger.info("Starting consumer for subscription {}. {}",
                getSubscriptionName(), signal.getLogWithIdAndType());

        consumer.initialize();

        long initializationTime = clock.millis();
        logger.info("Started consumer for subscription {} in {}ms. {}",
                getSubscriptionName(), initializationTime - startTime, signal.getLogWithIdAndType());
        signalTimesheet.put(START, initializationTime);
    }

    private void stop() {
        long startTime = clock.millis();
        logger.info("Stopping consumer for subscription {}", getSubscriptionName());

        consumer.tearDown();

        logger.info("Stopped consumer for subscription {} in {}ms", getSubscriptionName(), clock.millis() - startTime);
    }

    private void retransmit(Signal signal) {
        long startTime = clock.millis();
        logger.info("Starting retransmission for consumer of subscription {}. {}",
                getSubscriptionName(), signal.getLogWithIdAndType());
        try {
            retransmitter.reloadOffsets(getSubscriptionName(), consumer);
            logger.info("Done retransmission for consumer of subscription {} in {}ms",
                    getSubscriptionName(), clock.millis() - startTime);
        } catch (Exception ex) {
            logger.error("Failed retransmission for consumer of subscription {} in {}ms",
                    getSubscriptionName(), clock.millis() - startTime, ex);
        }
    }

    private void restart(Signal signal) {
        long startTime = clock.millis();
        try {
            logger.info("Restarting consumer for subscription {}. {}",
                    getSubscriptionName(), signal.getLogWithIdAndType());
            stop();
            start(signal);
            logger.info("Done restarting consumer for subscription {} in {}ms. {}",
                    getSubscriptionName(), clock.millis() - startTime, signal.getLogWithIdAndType());
        } catch (Exception e) {
            logger.error("Failed restarting consumer for subscription {} in {}ms. {}",
                    getSubscriptionName(), clock.millis() - startTime, signal.getLogWithIdAndType(), e);
        }
    }

    @Override
    public String toString() {
        return "ConsumerProcess{" +
                "subscriptionName=" + getSubscriptionName() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsumerProcess that = (ConsumerProcess) o;
        return Objects.equals(getSubscriptionName(), that.getSubscriptionName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSubscriptionName());
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public Map<Signal.SignalType, Long> getSignalTimesheet() {
        return ImmutableMap.copyOf(signalTimesheet);
    }

    private SubscriptionName getSubscriptionName() {
        return subscription.getQualifiedName();
    }
}
