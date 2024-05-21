package pl.allegro.tech.hermes.consumers.consumer.profiling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class is designed to be fully thread safe, except methods <code>startPartialMeasurement</code> and <code>stopPartialMeasurement</code>,
 * since they are always used in a single thread. Also, method <code>saveRetryDelay</code> is designed to be thread safe,
 * as <code>retryDelayMillis</code> is modified only by one thread, and it's volatile, so other threads always see updated value.
 */
public class DefaultConsumerProfiler implements ConsumerProfiler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultConsumerProfiler.class);

    private final SubscriptionName subscriptionName;
    private final long profilingThresholdMs;
    private volatile StopWatch stopWatch;
    private final Map<String, StopWatch> partialMeasurements;
    private volatile long retryDelayMillis = 0;

    public DefaultConsumerProfiler(SubscriptionName subscriptionName, long profilingThresholdMs) {
        this.subscriptionName = subscriptionName;
        this.profilingThresholdMs = profilingThresholdMs;
        this.partialMeasurements = new HashMap<>() {
            @Override
            public String toString() {
                StringBuilder stb = new StringBuilder("\n");
                for (Map.Entry<String, StopWatch> entry : this.entrySet()) {
                    stb.append(entry.getKey()).append(" = ")
                            .append(entry.getValue().prettyPrint(TimeUnit.MILLISECONDS)).append("\n");
                }
                return stb.toString();
            }
        };
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
    public void startPartialMeasurement(String measurement) {
        partialMeasurements.computeIfAbsent(
                measurement,
                (m) -> new StopWatch(measurement)
        ).start(measurement);
    }

    @Override
    public void stopPartialMeasurement(String measurement) {
        partialMeasurements.get(measurement).stop();
    }

    @Override
    public void saveRetryDelay(long retryDelay) {
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
        if (retryDelayMillis != 0) {
            logger.info("Consumer profiler measurements for subscription {} and {} run: \n {} retryDelayMillis {}",
                    subscriptionName, consumerRun, stopWatch.prettyPrint(TimeUnit.MILLISECONDS), retryDelayMillis);
        } else {
            logger.info("Consumer profiler measurements for subscription {} and {} run: \n {} partialMeasurements: {}",
                    subscriptionName, consumerRun, stopWatch.prettyPrint(TimeUnit.MILLISECONDS), partialMeasurements);
        }
    }
}
