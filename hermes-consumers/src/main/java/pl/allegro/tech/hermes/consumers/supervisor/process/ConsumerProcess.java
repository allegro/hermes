package pl.allegro.tech.hermes.consumers.supervisor.process;

import static pl.allegro.tech.hermes.consumers.supervisor.process.Signal.SignalType.START;

import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jctools.queues.SpscArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.consumer.Consumer;

public class ConsumerProcess implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ConsumerProcess.class);

  private final SpscArrayQueue<Signal> signals = new SpscArrayQueue<>(100);

  private final Clock clock;

  private final Consumer consumer;

  private final Retransmitter retransmitter;

  private final java.util.function.Consumer<SubscriptionName> onConsumerStopped;

  private final Duration unhealthyAfter;

  private volatile boolean running = true;

  private volatile long healthcheckRefreshTime;

  private final Map<Signal.SignalType, Long> signalTimesheet = new ConcurrentHashMap<>();

  public ConsumerProcess(
      Signal startSignal,
      Consumer consumer,
      Retransmitter retransmitter,
      Clock clock,
      Duration unhealthyAfter,
      java.util.function.Consumer<SubscriptionName> onConsumerStopped) {
    this.consumer = consumer;
    this.retransmitter = retransmitter;
    this.onConsumerStopped = onConsumerStopped;
    this.clock = clock;
    this.healthcheckRefreshTime = clock.millis();
    this.unhealthyAfter = unhealthyAfter;

    addSignal(startSignal);
  }

  @Override
  public void run() {
    try {
      Thread.currentThread().setName("consumer-" + getSubscriptionName());

      while (running && !Thread.currentThread().isInterrupted()) {
        consumer.consume(this::processSignals);
      }
    } catch (Throwable throwable) {
      logger.error("Consumer process of subscription {} failed", getSubscriptionName(), throwable);
    } finally {
      logger.info("Releasing consumer process thread of subscription {}", getSubscriptionName());
      refreshHealthcheck();
      try {
        stop();
      } catch (Exception exceptionWhileStopping) {
        logger.error(
            "An error occurred while stopping consumer process of subscription {}",
            getSubscriptionName(),
            exceptionWhileStopping);
      } finally {
        onConsumerStopped.accept(getSubscriptionName());
        Thread.currentThread().setName("consumer-released-thread");
      }
    }
  }

  public ConsumerProcess accept(Signal signal) {
    addSignal(signal);
    return this;
  }

  public boolean isHealthy() {
    return unhealthyAfter.toMillis() > lastSeen();
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
    try {
      switch (signal.getType()) {
        case START:
          start(signal);
          break;
        case STOP:
          logger.info(
              "Stopping main loop for consumer {}. {}",
              signal.getTarget(),
              signal.getLogWithIdAndType());
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
    logger.info(
        "Starting consumer for subscription {}. {}",
        getSubscriptionName(),
        signal.getLogWithIdAndType());

    consumer.initialize();

    long initializationTime = clock.millis();
    logger.info(
        "Started consumer for subscription {} in {}ms. {}",
        getSubscriptionName(),
        initializationTime - startTime,
        signal.getLogWithIdAndType());
    signalTimesheet.put(START, initializationTime);
  }

  private void stop() {
    long startTime = clock.millis();
    logger.info("Stopping consumer for subscription {}", getSubscriptionName());

    consumer.tearDown();

    logger.info(
        "Stopped consumer for subscription {} in {}ms",
        getSubscriptionName(),
        clock.millis() - startTime);
  }

  private void retransmit(Signal signal) {
    long startTime = clock.millis();
    logger.info(
        "Starting retransmission for consumer of subscription {}. {}",
        getSubscriptionName(),
        signal.getLogWithIdAndType());
    try {
      retransmitter.reloadOffsets(getSubscriptionName(), consumer);
      logger.info(
          "Done retransmission for consumer of subscription {} in {}ms",
          getSubscriptionName(),
          clock.millis() - startTime);
    } catch (Exception ex) {
      logger.error(
          "Failed retransmission for consumer of subscription {} in {}ms",
          getSubscriptionName(),
          clock.millis() - startTime,
          ex);
    }
  }

  @Override
  public String toString() {
    return "ConsumerProcess{" + "subscriptionName=" + getSubscriptionName() + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsumerProcess that = (ConsumerProcess) o;
    return Objects.equals(getSubscriptionName(), that.getSubscriptionName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getSubscriptionName());
  }

  public Subscription getSubscription() {
    return consumer.getSubscription();
  }

  public Map<Signal.SignalType, Long> getSignalTimesheet() {
    return ImmutableMap.copyOf(signalTimesheet);
  }

  SubscriptionName getSubscriptionName() {
    return getSubscription().getQualifiedName();
  }

  private void addSignal(Signal signal) {
    if (!this.signals.add(signal)) {
      logger.error(
          "[Queue: consumerProcessSignals] Unable to add item: queue is full. Offered item: {}",
          signal);
    }
  }
}
