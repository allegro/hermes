package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionMetrics;

import static pl.allegro.tech.hermes.api.SubscriptionHealth.Problem.UNREACHABLE;

public class UnreachableIndicator extends AbstractSubscriptionHealthProblemIndicator {
    private final double maxOtherErrorsRatio;
    private final double minSubscriptionRateForReliableMetrics;

    public UnreachableIndicator(double maxOtherErrorsRatio, double minSubscriptionRateForReliableMetrics) {
        this.maxOtherErrorsRatio = maxOtherErrorsRatio;
        this.minSubscriptionRateForReliableMetrics = minSubscriptionRateForReliableMetrics;
    }

    @Override
    public boolean problemOccurs(SubscriptionHealthContext context) {
        SubscriptionMetrics subscriptionMetrics = context.getSubscriptionMetrics();
        return areSubscriptionMetricsReliable(subscriptionMetrics) && isOtherErrorsRateHigh(subscriptionMetrics);
    }

    private boolean areSubscriptionMetricsReliable(SubscriptionMetrics subscriptionMetrics) {
        return subscriptionMetrics.getRate() > minSubscriptionRateForReliableMetrics;
    }

    private boolean isOtherErrorsRateHigh(SubscriptionMetrics subscriptionMetrics) {
        double otherErrorsRate = subscriptionMetrics.getOtherErrorsRate();
        double rate = subscriptionMetrics.getRate();
        return otherErrorsRate > maxOtherErrorsRatio * rate;
    }

    @Override
    public SubscriptionHealth.Problem getProblem() {
        return UNREACHABLE;
    }
}
