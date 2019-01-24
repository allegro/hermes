package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionMetrics;

import java.util.Optional;

import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.unreachable;

public class UnreachableIndicator implements SubscriptionHealthProblemIndicator {
    private final double maxOtherErrorsRatio;
    private final double minSubscriptionRateForReliableMetrics;

    public UnreachableIndicator(double maxOtherErrorsRatio, double minSubscriptionRateForReliableMetrics) {
        this.maxOtherErrorsRatio = maxOtherErrorsRatio;
        this.minSubscriptionRateForReliableMetrics = minSubscriptionRateForReliableMetrics;
    }

    @Override
    public Optional<SubscriptionHealthProblem> getProblem(SubscriptionHealthContext context) {
        SubscriptionMetrics subscriptionMetrics = context.getSubscriptionMetrics();
        if (areSubscriptionMetricsReliable(subscriptionMetrics) && isOtherErrorsRateHigh(subscriptionMetrics)) {
            return Optional.of(unreachable(subscriptionMetrics.getOtherErrorsRate()));
        }
        return Optional.empty();
    }

    private boolean areSubscriptionMetricsReliable(SubscriptionMetrics subscriptionMetrics) {
        return subscriptionMetrics.getRate() > minSubscriptionRateForReliableMetrics;
    }

    private boolean isOtherErrorsRateHigh(SubscriptionMetrics subscriptionMetrics) {
        double otherErrorsRate = subscriptionMetrics.getOtherErrorsRate();
        double rate = subscriptionMetrics.getRate();
        return otherErrorsRate > maxOtherErrorsRatio * rate;
    }
}
