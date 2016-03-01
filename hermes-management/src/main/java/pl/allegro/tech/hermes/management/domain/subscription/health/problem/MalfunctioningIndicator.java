package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionMetrics;

import static pl.allegro.tech.hermes.api.SubscriptionHealth.Problem.MALFUNCTIONING;

public class MalfunctioningIndicator extends AbstractSubscriptionHealthProblemIndicator {
    private final double max5xxErrorsRatio;
    private final double minSubscriptionRateForReliableMetrics;

    public MalfunctioningIndicator(double max5xxErrorsRatio, double minSubscriptionRateForReliableMetrics) {
        this.max5xxErrorsRatio = max5xxErrorsRatio;
        this.minSubscriptionRateForReliableMetrics = minSubscriptionRateForReliableMetrics;
    }

    @Override
    public boolean problemOccurs(SubscriptionHealthContext context) {
        SubscriptionMetrics subscriptionMetrics = context.getSubscriptionMetrics();
        return areSubscriptionMetricsReliable(subscriptionMetrics) && isCode5xxErrorsRateHigh(subscriptionMetrics);
    }

    private boolean areSubscriptionMetricsReliable(SubscriptionMetrics subscriptionMetrics) {
        return subscriptionMetrics.getRate() > minSubscriptionRateForReliableMetrics;
    }

    private boolean isCode5xxErrorsRateHigh(SubscriptionMetrics subscriptionMetrics) {
        double code5xxErrorsRate = subscriptionMetrics.getCode5xxErrorsRate();
        double rate = subscriptionMetrics.getRate();
        return code5xxErrorsRate > max5xxErrorsRatio * rate;
    }

    @Override
    public SubscriptionHealth.Problem getProblem() {
        return MALFUNCTIONING;
    }
}
