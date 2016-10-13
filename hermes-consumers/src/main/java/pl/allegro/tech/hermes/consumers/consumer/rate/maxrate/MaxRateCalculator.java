package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import com.codahale.metrics.Timer;
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
        logger.info("Max rate calculation started");

        long start = clock.millis();

        Map<SubscriptionName, Set<String>> subscriptionConsumers = subscriptionConsumersCache.getSubscriptionsConsumers();

        subscriptionConsumers.entrySet().forEach(entry -> {
            Subscription subscription = subscriptionsCache.getSubscription(entry.getKey());
            Set<String> consumerIds = entry.getValue();

            Set<ConsumerRateInfo> rateInfos = maxRateRegistry.ensureCorrectAssignments(subscription, consumerIds);

            Optional<Map<String, MaxRate>> newRates
                    = balancer.balance(subscription.getSerialSubscriptionPolicy().getRate(), rateInfos);

            newRates.ifPresent(rates -> {
                maxRateRegistry.update(subscription, rates);
                metrics.maxRateUpdatesCounter(subscription).inc();
            });
        });

        lastUpdateDurationMillis = clock.millis() - start;

        logger.info("Max rate calculation done in {}", Duration.ofMillis(lastUpdateDurationMillis));
    }


}
