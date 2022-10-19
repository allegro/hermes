package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.Gauges;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ClusterAssignmentCache;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class MaxRateCalculator {

    private static final Logger logger = LoggerFactory.getLogger(MaxRateCalculator.class);

    private final ClusterAssignmentCache clusterAssignmentCache;
    private final SubscriptionsCache subscriptionsCache;
    private final MaxRateBalancer balancer;
    private final MaxRateRegistry maxRateRegistry;
    private final HermesMetrics metrics;
    private final Clock clock;

    private volatile long lastUpdateDurationMillis = 0;

    MaxRateCalculator(ClusterAssignmentCache clusterAssignmentCache,
                      SubscriptionsCache subscriptionsCache,
                      MaxRateBalancer balancer,
                      MaxRateRegistry maxRateRegistry,
                      HermesMetrics metrics,
                      Clock clock) {
        this.clusterAssignmentCache = clusterAssignmentCache;
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

            final long start = clock.millis();
            maxRateRegistry.onBeforeMaxRateCalculation();

            clusterAssignmentCache.getSubscriptionConsumers().forEach((subscriptionName, consumerIds) -> {
                try {
                    Subscription subscription = subscriptionsCache.getSubscription(subscriptionName);
                    if (!subscription.isBatchSubscription()) {

                        Set<ConsumerRateInfo> rateInfos = maxRateRegistry.ensureCorrectAssignments(
                                subscription.getQualifiedName(), consumerIds);

                        Optional<Map<String, MaxRate>> newRates
                                = balancer.balance(subscription.getSerialSubscriptionPolicy().getRate(), rateInfos);

                        newRates.ifPresent(rates -> {
                            logger.debug("Calculated new max rates for {}: {}", subscription.getQualifiedName(), rates);

                            maxRateRegistry.update(subscription.getQualifiedName(), rates);
                        });
                    }
                } catch (Exception e) {
                    logger.error("Problem calculating max rates for subscription {}", subscriptionName, e);
                }
            });

            maxRateRegistry.onAfterMaxRateCalculation();

            lastUpdateDurationMillis = clock.millis() - start;
            logger.info("Max rate calculation done in {} ms", lastUpdateDurationMillis);
        } catch (Exception e) {
            logger.error("Problem calculating max rate", e);
        }
    }
}
