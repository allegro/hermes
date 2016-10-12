package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;

import java.util.Optional;

public class MaxRateProvider {

    private static final Logger logger = LoggerFactory.getLogger(MaxRateProvider.class);

    private static final double FALLBACK_MAX_RATE = 100.0;

    private final String consumerId;
    private final MaxRateRegistry registry;
    private final Subscription subscription;
    private final SendCounters sendCounters;
    private final int historyLimit;
    private volatile double maxRate;

    public MaxRateProvider(String consumerId, MaxRateRegistry registry, Subscription subscription,
                           SendCounters sendCounters, int historyLimit) {
        this.consumerId = consumerId;
        this.registry = registry;
        this.subscription = subscription;
        this.sendCounters = sendCounters;
        this.historyLimit = historyLimit;
    }

    public double get() {
        if (maxRate == 0) {
            maxRate = fetchOrDefaultMaxRate();
        }

        return maxRate;
    }

    public void tickForHistory() {
        try {
            double actualRate = sendCounters.getRate();
            recordCurrentRate(actualRate);
            maxRate = fetchOrDefaultMaxRate();
        } catch (Exception e) {
            logger.warn("Encountered problem updating consumer's max rate");
        }
    }

    private void recordCurrentRate(double actualRate) throws Exception {
        double usedRate = Math.min(actualRate / Math.max(maxRate, 1), 0.0);
        RateHistory rateHistory = registry.readOrCreateRateHistory(subscription, consumerId);
        RateHistory updatedHistory = RateHistory.updatedRates(rateHistory, usedRate, historyLimit);
        registry.writeRateHistory(subscription, consumerId, updatedHistory);
    }

    private double fetchOrDefaultMaxRate() {
        return fetchCurrentMaxRate().orElse(new MaxRate(FALLBACK_MAX_RATE)).getMaxRate();
    }

    private Optional<MaxRate> fetchCurrentMaxRate() {
        return registry.readMaxRate(subscription, consumerId);
    }
}
