package pl.allegro.tech.hermes.consumers.consumer.profiling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DefaultConsumerProfiler implements ConsumerProfiler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultConsumerProfiler.class);

    private final SubscriptionName subscriptionName;
    private StopWatch stopWatch;
    private final Map<String, StopWatch> partialMeasurements;
    private long retryDelayMillis = 0;

    public DefaultConsumerProfiler(SubscriptionName subscriptionName) {
        this.subscriptionName = subscriptionName;
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
    public void startMeasurements(Measurement measurement) {
        this.stopWatch = new StopWatch();
        this.stopWatch.start(measurement.getDescription());
    }

    @Override
    public void measure(Measurement measurement) {
        this.stopWatch.stop();
        this.stopWatch.start(measurement.getDescription());
    }

    @Override
    public void startPartialMeasurement(Measurement measurement) {
        if (!partialMeasurements.containsKey(measurement.getDescription())) {
            partialMeasurements.put(measurement.getDescription(), new StopWatch(measurement.getDescription()));
        }
        partialMeasurements.get(measurement.getDescription()).start(measurement.getDescription());
    }

    @Override
    public void stopPartialMeasurement(Measurement measurement) {
        partialMeasurements.get(measurement.getDescription()).stop();
    }

    @Override
    public void saveRetryDelay(long retryDelay) {
        this.retryDelayMillis = retryDelay;
    }

    @Override
    public void flushMeasurements(ConsumerRun consumerRun) {
        this.stopWatch.stop();
        if (retryDelayMillis != 0) {
            logger.info("Flushing measurements for subscription {} and {} run: \n {} retryDelayMillis {}",
                    subscriptionName, consumerRun, stopWatch.prettyPrint(TimeUnit.MILLISECONDS), retryDelayMillis);
        } else {
            logger.info("Flushing measurements for subscription {} and {} run: \n {} partialMeasurements: {}",
                    subscriptionName, consumerRun, stopWatch.prettyPrint(TimeUnit.MILLISECONDS), partialMeasurements);
        }
    }
}
