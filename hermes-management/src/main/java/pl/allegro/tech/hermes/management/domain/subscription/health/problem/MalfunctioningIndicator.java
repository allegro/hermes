package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;

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
        if (areSubscriptionMetricsReliable(context) && isCode5xxErrorsRateHigh(context)) {
            return Optional.of(malfunctioning(context.getCode5xxErrorsRate()));
        }
        return Optional.empty();
    }

    private boolean areSubscriptionMetricsReliable(SubscriptionHealthContext context) {
        return context.getSubscriptionRateRespectingDeliveryType() > minSubscriptionRateForReliableMetrics;
    }

    private boolean isCode5xxErrorsRateHigh(SubscriptionHealthContext context) {
        double code5xxErrorsRate = context.getCode5xxErrorsRate();
        double rate = context.getSubscriptionRateRespectingDeliveryType();
        return code5xxErrorsRate > max5xxErrorsRatio * rate;
    }
}
