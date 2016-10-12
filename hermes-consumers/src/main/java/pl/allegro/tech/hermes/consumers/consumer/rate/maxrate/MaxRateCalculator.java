package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;

import java.util.Map;
import java.util.Set;

public class MaxRateCalculator {
    private final SubscriptionConsumersCache subscriptionConsumersCache;
    private final SubscriptionsCache subscriptionsCache;
    private final MaxRateBalancer balancer;
    private final MaxRateRegistry maxRateRegistry;

    public MaxRateCalculator(SubscriptionConsumersCache subscriptionConsumersCache,
                             SubscriptionsCache subscriptionsCache,
                             MaxRateBalancer balancer,
                             MaxRateRegistry maxRateRegistry) {
        this.subscriptionConsumersCache = subscriptionConsumersCache;
        this.subscriptionsCache = subscriptionsCache;
        this.balancer = balancer;
        this.maxRateRegistry = maxRateRegistry;
    }

    public void calculate() {
        Map<SubscriptionName, Set<String>> subscriptionConsumers = subscriptionConsumersCache.getSubscriptionsConsumers();

        subscriptionConsumers.entrySet().forEach(entry -> {
            Subscription subscription = subscriptionsCache.getSubscription(entry.getKey());
            Set<String> consumerIds = entry.getValue();

            Set<ConsumerRateInfo> rateInfos = maxRateRegistry.ensureCorrectAssignments(subscription, consumerIds);

            Map<String, MaxRate> newRates
                    = balancer.balance(subscription.getSerialSubscriptionPolicy().getRate(), rateInfos);

            maxRateRegistry.update(subscription, newRates);
        });
    }
}
