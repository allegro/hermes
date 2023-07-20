package pl.allegro.tech.hermes.common.metric;

import com.codahale.metrics.Meter;
import io.micrometer.core.instrument.Counter;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.metrics.counters.MeterBackedHermesCounter;

public class SubscriptionHermesCounter extends MeterBackedHermesCounter {

    private final String graphiteName;
    private final SubscriptionName subscription;

    private SubscriptionHermesCounter(Counter micrometerCounter,
                                      Meter graphiteMeter,
                                      String graphiteName, SubscriptionName subscription) {
        super(micrometerCounter, graphiteMeter);
        this.graphiteName = graphiteName;
        this.subscription = subscription;
    }

    public static SubscriptionHermesCounter from(Counter micrometerCounter, Meter graphiteMeter,
                                                 String graphiteName, SubscriptionName subscription) {
        return new SubscriptionHermesCounter(micrometerCounter, graphiteMeter, graphiteName, subscription);
    }

    String getGraphiteName() {
        return graphiteName;
    }

    SubscriptionName getSubscription() {
        return subscription;
    }

    Counter getMicrometerCounter() {
        return micrometerCounter;
    }
}
