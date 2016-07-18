package pl.allegro.tech.hermes.consumers.consumer.rate.calculator;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.ActiveConsumerCounter;

class MaximumOutputRateCalculator {

    private final ActiveConsumerCounter activeConsumerCounter;

    MaximumOutputRateCalculator(ActiveConsumerCounter activeConsumerCounter) {
        this.activeConsumerCounter = activeConsumerCounter;
    }

    double calculateMaximumOutputRate(Subscription subscription) {
        int numberOfConsumersOnSubscription = activeConsumerCounter.countActiveConsumers(subscription);
        return subscription.getSerialSubscriptionPolicy().getRate().doubleValue() / Math.max(numberOfConsumersOnSubscription, 1);
    }
}
