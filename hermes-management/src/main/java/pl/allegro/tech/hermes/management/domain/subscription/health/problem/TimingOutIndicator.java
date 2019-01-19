package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionMetrics;

import java.util.Optional;

import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.timingOut;

public class TimingOutIndicator implements SubscriptionHealthProblemIndicator {
    private final double maxTimeoutsRatio;
    private final double minSubscriptionRateForReliableMetrics;

    public TimingOutIndicator(double maxTimeoutsRatio, double minSubscriptionRateForReliableMetrics) {
        this.maxTimeoutsRatio = maxTimeoutsRatio;
        this.minSubscriptionRateForReliableMetrics = minSubscriptionRateForReliableMetrics;
    }

    @Override
    public Optional<SubscriptionHealthProblem> getProblem(SubscriptionHealthContext context) {
        SubscriptionMetrics subscriptionMetrics = context.getSubscriptionMetrics();
        if (areSubscriptionMetricsReliable(subscriptionMetrics) && isTimeoutsRateHigh(subscriptionMetrics)) {
            return Optional.of(timingOut(subscriptionMetrics.getTimeoutsRate()));
        }
        return Optional.empty();
    }

    private boolean areSubscriptionMetricsReliable(SubscriptionMetrics subscriptionMetrics) {
        return subscriptionMetrics.getRate() > minSubscriptionRateForReliableMetrics;
    }

    private boolean isTimeoutsRateHigh(SubscriptionMetrics subscriptionMetrics) {
        double timeoutsRate = subscriptionMetrics.getTimeoutsRate();
        double rate = subscriptionMetrics.getRate();
        return timeoutsRate > maxTimeoutsRatio * rate;
    }
}
