package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.ActiveConsumerCounter;

class StrictMaxRateProvider implements MaxRateProvider {

    private final ActiveConsumerCounter activeConsumerCounter;

    private volatile Subscription subscription;

    StrictMaxRateProvider(ActiveConsumerCounter activeConsumerCounter, Subscription subscription) {
        this.activeConsumerCounter = activeConsumerCounter;
        this.subscription = subscription;
    }

    @Override
    public double get() {
        int consumersCount = activeConsumerCounter.countActiveConsumers(subscription);
        double subscriptionRate = subscription.getSerialSubscriptionPolicy().getRate().doubleValue();
        return subscriptionRate / Math.max(consumersCount, 1);
    }

    @Override
    public void updateSubscription(Subscription newSubscription) {
        this.subscription = newSubscription;
    }
}
