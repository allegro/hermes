package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.Gauges;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class MaxRateCalculator {

    private static final Logger logger = LoggerFactory.getLogger(MaxRateCalculator.class);

    private final SubscriptionConsumersCache subscriptionConsumersCache;
    private final SubscriptionsCache subscriptionsCache;
    private final MaxRateBalancer balancer;
    private final MaxRateRegistry maxRateRegistry;
    private final HermesMetrics metrics;
    private final Clock clock;

    private volatile long lastUpdateDurationMillis = 0;

    MaxRateCalculator(SubscriptionConsumersCache subscriptionConsumersCache,
                      SubscriptionsCache subscriptionsCache,
                      MaxRateBalancer balancer,
                      MaxRateRegistry maxRateRegistry,
                      HermesMetrics metrics,
                      Clock clock) {
        this.subscriptionConsumersCache = subscriptionConsumersCache;
        this.subscriptionsCache = subscriptionsCache;
        this.balancer = balancer;
        this.maxRateRegistry = maxRateRegistry;
        this.metrics = metrics;
        this.clock = clock;

        metrics.registerGauge(Gauges.MAX_RATE_CALCULATION_DURATION, () -> lastUpdateDurationMillis);
    }

    void calculate() {
        try {
            logger.info("Max rate calculation started");

            long start = clock.millis();

            Map<SubscriptionName, Set<String>> subscriptionConsumers =
                    subscriptionConsumersCache.getSubscriptionsConsumers();

            subscriptionConsumers.entrySet().forEach(entry -> {
                try {
                    Subscription subscription = subscriptionsCache.getSubscription(entry.getKey());
                    if (!subscription.isBatchSubscription()) {
                        Set<String> consumerIds = entry.getValue();

                        Set<ConsumerRateInfo> rateInfos = maxRateRegistry.ensureCorrectAssignments(
                                subscription.getQualifiedName(), consumerIds);

                        Optional<Map<String, MaxRate>> newRates
                                = balancer.balance(subscription.getSerialSubscriptionPolicy().getRate(), rateInfos);

                        newRates.ifPresent(rates -> {
                            logger.info("Calculated new max rates for {}: {}",
                                    subscription.getQualifiedName(), rates);

                            maxRateRegistry.update(subscription.getQualifiedName(), rates);
                            metrics.maxRateUpdatesCounter(subscription).inc();
                        });
                    }
                } catch (Exception e) {
                    logger.error("Problem calculating max rates for subscription {}", entry.getKey(), e);
                }
            });

            lastUpdateDurationMillis = clock.millis() - start;
            logger.info("Max rate calculation done in {} ms", lastUpdateDurationMillis);
        } catch (Exception e) {
            logger.error("Problem calculating max rate", e);
        }
    }
}
