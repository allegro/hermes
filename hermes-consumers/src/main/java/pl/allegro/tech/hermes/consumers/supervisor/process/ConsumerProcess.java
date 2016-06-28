package pl.allegro.tech.hermes.consumers.supervisor.process;

import org.jctools.queues.SpscArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;

import java.time.Clock;
import java.util.Objects;

public class ConsumerProcess implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerProcess.class);

    private final SpscArrayQueue<Signal> signals = new SpscArrayQueue<>(100);

    private final Clock clock;

    private final SubscriptionName subscriptionName;

    private final Consumer consumer;

    private final Retransmitter retransmitter;

    private volatile boolean running = true;

    private volatile long healtcheckRefreshTime;

    public ConsumerProcess(
            SubscriptionName subscriptionName,
            Consumer consumer,
            Retransmitter retransmitter,
            Clock clock
    ) {
        this.subscriptionName = subscriptionName;
        this.consumer = consumer;
        this.retransmitter = retransmitter;
        this.clock = clock;
        this.healtcheckRefreshTime = clock.millis();
    }

    @Override
    public void run() {
        try {
            Thread.currentThread().setName("consumer-" + subscriptionName);

            start();
            while (running) {
                consumer.consume(() -> processSignals());
            }
            stop();

        } finally {
            refreshHealthcheck();
            Thread.currentThread().setName("consumer-released-thread");
        }
    }

    public ConsumerProcess accept(Signal signal) {
        this.signals.add(signal);
        return this;
    }

    public long healtcheckRefreshTime() {
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
        switch (signal.getType()) {
            case RESTART:
                restart();
                break;
            case STOP:
                this.running = false;
                break;
            case RETRANSMIT:
                retransmit();
                break;
            case UPDATE_SUBSCRIPTION:
                consumer.updateSubscription(signal.getExtractedPayload());
                break;
            case UPDATE_TOPIC:
                consumer.updateTopic(signal.getExtractedPayload());
                break;
        }
    }

    private void start() {
        long startTime = clock.millis();
        logger.info("Starting consumer for subscription {}", subscriptionName);

        consumer.initialize();

        logger.info("Started consumer for subscription {} in {}ms", subscriptionName, clock.millis() - startTime);
    }

    private void stop() {
        long startTime = clock.millis();
        logger.info("Stopping consumer for subscription {}", subscriptionName);

        consumer.tearDown();

        logger.info("Stopped consumer for subscription {} in {}ms", subscriptionName, clock.millis() - startTime);
    }

    private void retransmit() {
        long startTime = clock.millis();
        logger.info("Starting retransmission for consumer of subscription {}", subscriptionName);
        stop();
        retransmitter.reloadOffsets(subscriptionName);
        start();
        logger.info("Done retransmission for consumer of subscription {} in {}ms", subscriptionName, clock.millis() - startTime);
    }

    private void restart() {
        long startTime = clock.millis();
        logger.info("Restarting consumer for subscription {}", subscriptionName);
        stop();
        start();
        logger.info("Done restarting consumer for subscription {} in {}ms", subscriptionName, clock.millis() - startTime);
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
}
