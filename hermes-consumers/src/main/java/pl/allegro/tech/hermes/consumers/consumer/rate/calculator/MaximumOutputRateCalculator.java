package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

class MaximumOutputRateCalculator {

    private final HermesMetrics hermesMetrics;

    MaximumOutputRateCalculator(HermesMetrics hermesMetrics) {
        this.hermesMetrics = hermesMetrics;
    }

    double calculateMaximumOutputRate(Subscription subscription) {
        int numberOfConsumersOnSubscription = hermesMetrics.countActiveConsumers(subscription);
        return subscription.getSerialSubscriptionPolicy().getRate().doubleValue() / Math.max(numberOfConsumersOnSubscription, 1);
    }
}
