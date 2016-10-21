package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;

import java.util.Optional;

public class NegotiatedMaxRateProvider implements MaxRateProvider {

    private static final Logger logger = LoggerFactory.getLogger(NegotiatedMaxRateProvider.class);

    private static final double FALLBACK_MAX_RATE = 1.0; // TODO use property

    private final String consumerId;
    private final MaxRateRegistry registry;
    private final MaxRateSupervisor maxRateSupervisor;
    // TODO: need only subscription qualified name - the other details could be updated during runtime, and we don't need to handle that
    private final Subscription subscription;
    private final SendCounters sendCounters;
    private final HermesMetrics metrics;
    private final int historyLimit;
    private volatile double maxRate = FALLBACK_MAX_RATE;

    NegotiatedMaxRateProvider(String consumerId, MaxRateRegistry registry, MaxRateSupervisor maxRateSupervisor,
                              Subscription subscription, SendCounters sendCounters, HermesMetrics metrics, int historyLimit) {
        this.consumerId = consumerId;
        this.registry = registry;
        this.maxRateSupervisor = maxRateSupervisor;
        this.subscription = subscription;
        this.sendCounters = sendCounters;
        this.metrics = metrics;
        this.historyLimit = historyLimit;
    }

    @Override
    public double get() {
        return maxRate;
    }

    void tickForHistory() {
        recordCurrentRate(sendCounters.getRate());
        maxRate = fetchOrDefaultMaxRate();
    }

    private void recordCurrentRate(double actualRate) {
        try {
            double usedRate = Math.min(actualRate / Math.max(maxRate, 1), 1.0);
            RateHistory rateHistory = registry.readOrCreateRateHistory(subscription, consumerId);
            RateHistory updatedHistory = RateHistory.updatedRates(rateHistory, usedRate, historyLimit);
            registry.writeRateHistory(subscription, consumerId, updatedHistory);
        } catch (Exception e) {
            metrics.rateHistoryFailuresCounter(subscription).inc();
            logger.warn("Encountered problem updating max rate for subscription {}, consumer {}",
                    subscription.getQualifiedName(), consumerId, e);
        }
    }

    private double fetchOrDefaultMaxRate() {
        // TODO: if I can't fetch max rate, I need to temporarily slow down so I don't abuse the subscriber
        return fetchCurrentMaxRate().orElse(new MaxRate(FALLBACK_MAX_RATE)).getMaxRate();
    }

    private Optional<MaxRate> fetchCurrentMaxRate() {
        try {
            return registry.readMaxRate(subscription, consumerId);
        } catch (Exception e) {
            metrics.maxRateFetchFailuresCounter(subscription).inc();
            logger.warn("Encountered problem fetching max rate for subscription: {}, consumer: {}." +
                            " Setting default max rate: {}",
                    subscription.getQualifiedName(), consumerId, FALLBACK_MAX_RATE);
            return Optional.empty();
        }
    }


    public void start() {
        maxRateSupervisor.register(this);
        metrics.registerMaxRateGauge(subscription, this::get);
        metrics.registerRateGauge(subscription, sendCounters::getRate);
    }

    public void shutdown() {
        maxRateSupervisor.unregister(this);
        metrics.unregisterMaxRateGauge(subscription);
        metrics.unregisterRateGauge(subscription);
    }
}
