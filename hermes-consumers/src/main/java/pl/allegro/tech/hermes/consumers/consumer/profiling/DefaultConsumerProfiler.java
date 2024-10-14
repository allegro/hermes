package pl.allegro.tech.hermes.consumers.consumer.profiling;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;
import pl.allegro.tech.hermes.api.SubscriptionName;

/** This class is designed to be fully thread safe. */
public class DefaultConsumerProfiler implements ConsumerProfiler {

  private static final Logger logger = LoggerFactory.getLogger(DefaultConsumerProfiler.class);

  private final SubscriptionName subscriptionName;
  private final long profilingThresholdMs;
  private StopWatch stopWatch;
  private StopWatch partialMeasurements;
  private long retryDelayMillis = 0;

  public DefaultConsumerProfiler(SubscriptionName subscriptionName, long profilingThresholdMs) {
    this.subscriptionName = subscriptionName;
    this.profilingThresholdMs = profilingThresholdMs;
  }

  @Override
  public synchronized void startMeasurements(String measurement) {
    this.stopWatch = new StopWatch();
    this.stopWatch.start(measurement);
  }

  @Override
  public synchronized void measure(String measurement) {
    this.stopWatch.stop();
    this.stopWatch.start(measurement);
  }

  @Override
  public synchronized void startPartialMeasurement(String measurement) {
    if (partialMeasurements == null) {
      partialMeasurements = new StopWatch();
    }
    partialMeasurements.start(measurement);
  }

  @Override
  public synchronized void stopPartialMeasurement() {
    partialMeasurements.stop();
  }

  @Override
  public synchronized void saveRetryDelay(long retryDelay) {
    this.retryDelayMillis = retryDelay;
  }

  @Override
  public synchronized void flushMeasurements(ConsumerRun consumerRun) {
    this.stopWatch.stop();
    if (stopWatch.getTotalTimeMillis() > profilingThresholdMs) {
      logMeasurements(consumerRun);
    }
  }

  private void logMeasurements(ConsumerRun consumerRun) {
    if (partialMeasurements != null) {
      logger.info(
          "Consumer profiler measurements for subscription {} and {} run: \n {} partialMeasurements: {} retryDelayMillis {}",
          subscriptionName,
          consumerRun,
          stopWatch.prettyPrint(TimeUnit.MILLISECONDS),
          partialMeasurements.prettyPrint(TimeUnit.MILLISECONDS),
          retryDelayMillis);
    } else {
      logger.info(
          "Consumer profiler measurements for subscription {} and {} run: \n {} partialMeasurements: {}, retryDelayMillis {}",
          subscriptionName,
          consumerRun,
          stopWatch.prettyPrint(TimeUnit.MILLISECONDS),
          null,
          retryDelayMillis);
    }
  }
}
