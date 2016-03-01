package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionMetrics;

import static pl.allegro.tech.hermes.api.SubscriptionHealth.Problem.TIMING_OUT;

public class TimingOutIndicator extends AbstractSubscriptionHealthProblemIndicator {
    private final double maxTimeoutsRatio;
    private final double minSubscriptionRateForReliableMetrics;

    public TimingOutIndicator(double maxTimeoutsRatio, double minSubscriptionRateForReliableMetrics) {
        this.maxTimeoutsRatio = maxTimeoutsRatio;
        this.minSubscriptionRateForReliableMetrics = minSubscriptionRateForReliableMetrics;
    }

    @Override
    public boolean problemOccurs(SubscriptionHealthContext context) {
        SubscriptionMetrics subscriptionMetrics = context.getSubscriptionMetrics();
        return areSubscriptionMetricsReliable(subscriptionMetrics) && isTimeoutsRateHigh(subscriptionMetrics);
    }

    private boolean areSubscriptionMetricsReliable(SubscriptionMetrics subscriptionMetrics) {
        return subscriptionMetrics.getRate() > minSubscriptionRateForReliableMetrics;
    }

    private boolean isTimeoutsRateHigh(SubscriptionMetrics subscriptionMetrics) {
        double timeoutsRate = subscriptionMetrics.getTimeoutsRate();
        double rate = subscriptionMetrics.getRate();
        return timeoutsRate > maxTimeoutsRatio * rate;
    }

    @Override
    public SubscriptionHealth.Problem getProblem() {
        return TIMING_OUT;
    }
}
