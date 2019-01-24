package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import pl.allegro.tech.hermes.api.SubscriptionHealthProblem;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthProblemIndicator;

import java.util.Optional;

import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.receivingMalformedMessages;

public class ReceivingMalformedMessagesIndicator implements SubscriptionHealthProblemIndicator {
    private final double max4xxErrorsRatio;
    private final double minSubscriptionRateForReliableMetrics;

    public ReceivingMalformedMessagesIndicator(double max4xxErrorsRatio, double minSubscriptionRateForReliableMetrics) {
        this.max4xxErrorsRatio = max4xxErrorsRatio;
        this.minSubscriptionRateForReliableMetrics = minSubscriptionRateForReliableMetrics;
    }

    @Override
    public Optional<SubscriptionHealthProblem> getProblem(SubscriptionHealthContext context) {
        if (context.subscriptionHasRetryOnError()
                && areSubscriptionMetricsReliable(context)
                && isCode4xxErrorsRateHigh(context)) {
            return Optional.of(receivingMalformedMessages(context.getCode4xxErrorsRate()));
        }
        return Optional.empty();
    }

    private boolean areSubscriptionMetricsReliable(SubscriptionHealthContext context) {
        return context.getSubscriptionRateRespectingDeliveryType() > minSubscriptionRateForReliableMetrics;
    }

    private boolean isCode4xxErrorsRateHigh(SubscriptionHealthContext context) {
        double code4xxErrorsRate = context.getCode4xxErrorsRate();
        double rate = context.getSubscriptionRateRespectingDeliveryType();
        return code4xxErrorsRate > max4xxErrorsRatio * rate;
    }
}
