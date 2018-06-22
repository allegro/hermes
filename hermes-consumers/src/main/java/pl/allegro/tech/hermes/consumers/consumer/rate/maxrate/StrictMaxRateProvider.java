package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.ActiveConsumerCounter;

/**
 * @deprecated This is a simplistic implementation, but it's not based on any solid foundation.
 * The consumer count comes from a temporary solution based on counters of inflight messages in Zookeeper, thus
 * is prone to errors. It also doesn't consider multiple DCs, dividing the subscription rate among all consumers
 * globally.
 * {@link NegotiatedMaxRateProvider} is regarded to be a reliable implementation at this time.
 */
@Deprecated
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
