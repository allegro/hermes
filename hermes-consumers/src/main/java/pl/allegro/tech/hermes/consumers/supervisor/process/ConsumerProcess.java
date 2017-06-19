package pl.allegro.tech.hermes.consumers.supervisor.process;

import com.google.common.collect.ImmutableMap;
import org.jctools.queues.SpscArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final SubscriptionName subscriptionName;

    private final Consumer consumer;

    private final Retransmitter retransmitter;

    private final java.util.function.Consumer<Signal> shutdownCallback;

    private boolean running = true;

    private long healtcheckRefreshTime;

    private Map<Signal.SignalType, Long> signalTimesheet = new ConcurrentHashMap<>();
    private Signal lastSignal;

    public ConsumerProcess(
            Signal startSignal,
            Retransmitter retransmitter,
            java.util.function.Consumer<Signal> shutdownCallback,
            Clock clock
    ) {
        this.subscriptionName = startSignal.getTarget();
        this.consumer = startSignal.getPayload();
        this.retransmitter = retransmitter;
        this.shutdownCallback = shutdownCallback;
        this.clock = clock;
        this.healtcheckRefreshTime = clock.millis();

        this.signals.add(startSignal);
    }

    @Override
    public void run() {
        try {
            Thread.currentThread().setName("consumer-" + subscriptionName);

            while (running && !Thread.interrupted()) {
                consumer.consume(this::processSignals);
            }
            stop();

        } catch (Exception ex) {
            logger.error("Consumer process of subscription {} failed", subscriptionName, ex);
        } finally {
            logger.info("Releasing consumer process thread of subscription {}", subscriptionName);
            shutdownCallback.accept(lastSignal);
            refreshHealthcheck();
            Thread.currentThread().setName("consumer-released-thread");
        }
    }

    public ConsumerProcess accept(Signal signal) {
        this.signals.add(signal);
        return this;
    }

    public long lastSeen() {
        return clock.millis() - healtcheckRefreshTime;
    }

    public long healthcheckRefreshTime() {
        return healtcheckRefreshTime;
    }

    private void processSignals() {
        refreshHealthcheck();
        signals.drain(this::process);
        refreshHealthcheck();
    }

    private void refreshHealthcheck() {
        this.healtcheckRefreshTime = clock.millis();
    }

    private void process(Signal signal) {
        lastSignal = signal;
        try {
            switch (signal.getType()) {
                case START:
                    start(signal.getId());
                    break;
                case RESTART:
                    restart(signal.getId());
                    break;
                case STOP:
                    logger.info("Received stop signal {}", signal);
                    this.running = false;
                    break;
                case RETRANSMIT:
                    retransmit(signal.getId());
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

    private void start(long signalId) {
        long startTime = clock.millis();
        logger.info("Starting consumer for subscription {}. Signal id {}", subscriptionName, signalId);

        consumer.initialize();

        long initializationTime = clock.millis();
        logger.info("Started consumer for subscription {} in {}ms. Signal id {}", subscriptionName, initializationTime - startTime, signalId);
        signalTimesheet.put(START, initializationTime);
    }

    private void stop() {
        long startTime = clock.millis();
        logger.info("Stopping consumer for subscription {}", subscriptionName);

        consumer.tearDown();

        logger.info("Stopped consumer for subscription {} in {}ms", subscriptionName, clock.millis() - startTime);
    }

    private void retransmit(long signalId) {
        long startTime = clock.millis();
        logger.info("Starting retransmission for consumer of subscription {}. Signal id {}", subscriptionName, signalId);
        try {
            retransmitter.reloadOffsets(subscriptionName, consumer);
            logger.info("Done retransmission for consumer of subscription {} in {}ms", subscriptionName, clock.millis() - startTime);
        } catch (Exception ex) {
            logger.error("Failed retransmission for consumer of subscription {} in {}ms",
                    subscriptionName, clock.millis() - startTime, ex);
        }
    }

    private void restart(long signalId) {
        long startTime = clock.millis();
        try {
            logger.info("Restarting consumer for subscription {}. Signal id {}", subscriptionName, signalId);
            stop();
            start(signalId);
            logger.info("Done restarting consumer for subscription {} in {}ms. Signal id {}",
                    subscriptionName, clock.millis() - startTime, signalId);
        } catch (Exception e) {
            logger.error("Failed restarting consumer for subscription {} in {}ms. Signal id {}",
                    subscriptionName, clock.millis() - startTime, signalId, e);
        }
    }

    @Override
    public String toString() {
        return "ConsumerProcess{" +
                "subscriptionName=" + subscriptionName +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsumerProcess that = (ConsumerProcess) o;
        return Objects.equals(subscriptionName, that.subscriptionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriptionName);
    }

    public SubscriptionName getSubscriptionName() {
        return subscriptionName;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public Map<Signal.SignalType, Long> getSignalTimesheet() {
        return ImmutableMap.copyOf(signalTimesheet);
    }
}
