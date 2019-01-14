package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionMetrics;

import java.util.Optional;

import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.malfunctioning;

public class MalfunctioningIndicator implements SubscriptionHealthProblemIndicator {
    private final double max5xxErrorsRatio;
    private final double minSubscriptionRateForReliableMetrics;

    public MalfunctioningIndicator(double max5xxErrorsRatio, double minSubscriptionRateForReliableMetrics) {
        this.max5xxErrorsRatio = max5xxErrorsRatio;
        this.minSubscriptionRateForReliableMetrics = minSubscriptionRateForReliableMetrics;
    }

    @Override
    public Optional<SubscriptionHealthProblem> getProblem(SubscriptionHealthContext context) {
        SubscriptionMetrics subscriptionMetrics = context.getSubscriptionMetrics();
        if (areSubscriptionMetricsReliable(subscriptionMetrics) && isCode5xxErrorsRateHigh(subscriptionMetrics)) {
            return Optional.of(malfunctioning(subscriptionMetrics.getCode5xxErrorsRate()));
        }
        return Optional.empty();
    }

    private boolean areSubscriptionMetricsReliable(SubscriptionMetrics subscriptionMetrics) {
        return subscriptionMetrics.getRate() > minSubscriptionRateForReliableMetrics;
    }

    private boolean isCode5xxErrorsRateHigh(SubscriptionMetrics subscriptionMetrics) {
        double code5xxErrorsRate = subscriptionMetrics.getCode5xxErrorsRate();
        double rate = subscriptionMetrics.getRate();
        return code5xxErrorsRate > max5xxErrorsRatio * rate;
    }
}
