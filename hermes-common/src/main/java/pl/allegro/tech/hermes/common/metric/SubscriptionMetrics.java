package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import pl.allegro.tech.hermes.api.SubscriptionName;

import java.util.List;

import static pl.allegro.tech.hermes.common.metric.Meters.SUBSCRIPTION_THROUGHPUT_BYTES;

public class SubscriptionMetrics {
    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;

    public SubscriptionMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
    }

    public SubscriptionHermesCounter subscriptionThroughputBytes(SubscriptionName subscription) {
        return SubscriptionHermesCounter.from(
                micrometerCounter("subscription-throughput", subscription),
                hermesMetrics.meter(SUBSCRIPTION_THROUGHPUT_BYTES, subscription.getTopicName(), subscription.getName()),
                SUBSCRIPTION_THROUGHPUT_BYTES, subscription);
    }

    public void unregister(SubscriptionHermesCounter hermesCounter) {
        meterRegistry.remove(hermesCounter.getMicrometerCounter());
        hermesMetrics.unregister(hermesCounter.getGraphiteName(), hermesCounter.getSubscription());
    }

    private Counter micrometerCounter(String metricName, SubscriptionName subscription) {
        return meterRegistry.counter(metricName, subscriptionTags(subscription));
    }

    private Iterable<Tag> subscriptionTags(SubscriptionName subscriptionName) {
        return List.of(
                Tag.of("group", subscriptionName.getTopicName().getGroupName()),
                Tag.of("topic", subscriptionName.getTopicName().getName()),
                Tag.of("subscription", subscriptionName.getName())
        );
    }
}
