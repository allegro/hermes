package pl.allegro.tech.hermes.management.domain.subscription.health.problem;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionHealth;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthContext;
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionMetrics;

import static pl.allegro.tech.hermes.api.SubscriptionHealth.Problem.RECEIVING_MALFORMED_MESSAGES;

public class ReceivingMalformedMessagesIndicator extends AbstractSubscriptionHealthProblemIndicator {
    private final double max4xxErrorsRatio;
    private final double minSubscriptionRateForReliableMetrics;

    public ReceivingMalformedMessagesIndicator(double max4xxErrorsRatio, double minSubscriptionRateForReliableMetrics) {
        this.max4xxErrorsRatio = max4xxErrorsRatio;
        this.minSubscriptionRateForReliableMetrics = minSubscriptionRateForReliableMetrics;
    }

    @Override
    public boolean problemOccurs(SubscriptionHealthContext context) {
        Subscription subscription = context.getSubscription();
        SubscriptionMetrics subscriptionMetrics = context.getSubscriptionMetrics();
        return hasClientErrorRetry(subscription)
                && areSubscriptionMetricsReliable(subscriptionMetrics)
                && isCode4xxErrorsRateHigh(subscriptionMetrics);
    }

    private boolean hasClientErrorRetry(Subscription subscription) {
        if (subscription.isBatchSubscription()) {
            return subscription.getBatchSubscriptionPolicy().isRetryClientErrors();
        } else {
            return subscription.getSerialSubscriptionPolicy().isRetryClientErrors();
        }
    }

    private boolean areSubscriptionMetricsReliable(SubscriptionMetrics subscriptionMetrics) {
        return subscriptionMetrics.getRate() > minSubscriptionRateForReliableMetrics;
    }

    private boolean isCode4xxErrorsRateHigh(SubscriptionMetrics subscriptionMetrics) {
        double code4xxErrorsRate = subscriptionMetrics.getCode4xxErrorsRate();
        double rate = subscriptionMetrics.getRate();
        return code4xxErrorsRate > max4xxErrorsRatio * rate;
    }

    @Override
    public SubscriptionHealth.Problem getProblem() {
        return RECEIVING_MALFORMED_MESSAGES;
    }
}
